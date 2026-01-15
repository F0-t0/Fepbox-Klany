# Specyfikacja techniczna – Fepbox-Klany (Minecraft 1.21.4, Paper/Spigot, Maven)

## 1. Ocena złożoności

- Poziom: **hard** (rozbudowany system klanów + ranking PvP + integracje + storage asynchroniczny).
- Wiele warstw: domena (klany/punkty), UI (title/chat), storage (SQLite + migracje), integracje (PlaceholderAPI), konfiguracja z walidacją.
- Wysokie wymagania dot. stabilności (brak blokowania main-thread, spójność danych, edge-case’y walki / logoutów).

## 2. Kontekst techniczny

- **Język**: Java 21 (docelowo kompatybilna z serwerem 1.21.4).
- **Platforma**: Paper/Spigot 1.21.4 (`api-version: 1.21` w `plugin.yml`).
- **Build**: Maven, gotowy projekt pod IntelliJ IDEA.
- **Główne dependency**:
  - Bukkit/Paper API (domyślnie przez `spigot-api` lub `paper-api` jako `provided`).
  - Adventure API (wbudowane w Paper, używane z poziomu API serwera; opcjonalnie import `net.kyori.adventure` artefaktów jeśli potrzeba).
  - **PlaceholderAPI** – jako soft-depend (bez bezpośredniego `compile` dependency lub z `provided`, plugin nie może crashować przy braku PAPI).
  - **SQLite JDBC** (`org.xerial:sqlite-jdbc`) – do storage.
  - Opcjonalnie lekki pool połączeń (np. HikariCP) – możliwe, ale raczej prosty, dobrze zarządzany `DataSource` z jednym współdzielonym połączeniem + async-taski wystarczy, biorąc pod uwagę ograniczoną skalę.

## 3. Architektura i podejście implementacyjne

### 3.1 Warstwa główna pluginu

- Klasa główna: `pl.fepbox.klany.FepboxKlanyPlugin` (dziedziczy po `JavaPlugin`).
- Odpowiedzialności:
  - Inicjalizacja configu i walidacji.
  - Bootstrap storage (SQLite, migracje).
  - Utworzenie i rejestracja serwisów domenowych:
    - `ClanService`
    - `PointsService`
    - `PlayerService` (lub `ProfileService` – zarządzanie cache profili).
    - `InviteService` (opcjonalnie osobna warstwa).
  - Rejestracja listenerów eventów (PVP/śmierć, join/quit, zmiana nicku).
  - Rejestracja komend i tab-completerów.
  - Integracja z PlaceholderAPI (rejestracja expansion przy obecności PAPI).
  - Poprawne zamknięcie zasobów w `onDisable()` (wątki, executory, połączenia DB).

### 3.2 Warstwa konfiguracji

- Klasa: `ConfigManager` + struktury DTO/Konfig:
  - `LimitsConfig`
  - `FilterConfig`
  - `PointsConfig`
  - `UIConfig` (title/subtitle/messages/emoji).
  - `RankingConfig`
  - `PlaceholderConfig`
  - `StorageConfig`
- Schemat:
  - Przy starcie wczytanie `config.yml` do pamięci.
  - Walidacja:
    - Brak klucza → wartość domyślna + ostrzeżenie w logu.
    - Zły typ/dane → fallback do domyślnego + ostrzeżenie.
    - Niedozwolone wartości (np. max < min, złe regexy, niedozwolone limity) → poprawny zakres + czytelny log.
  - Brak NPE: wszystkie odczyty są z klas konfigu wymuszających nie-nullowe pola.
  - Reload: `/klan reload` odczytuje config od nowa, re-waliduje, aktualizuje struktury w pamięci bez restartu pluginu.

### 3.3 Warstwa storage (SQLite)

- Klasa: `SqliteDatabase` / `DatabaseManager`.
- Połączenie:
  - Ścieżka do pliku DB z configu: `plugins/Fepbox-Klany/data.db`.
  - Utworzenie katalogu pluginu i pliku przy starcie, jeśli nie istnieją.
  - Użycie `java.sql` + JDBC; logika w oddzielnej klasie, która:
    - Utrzymuje jeden `DataSource` lub jedno długotrwałe połączenie z odpowiednią konfiguracją (auto-commit, walidacja).
    - Wykonuje migracje schematu (`schema_version` w osobnej tabeli).
- Schemat tabel (propozycja):
  - `players`:
    - `id` INTEGER PRIMARY KEY AUTOINCREMENT
    - `uuid` TEXT UNIQUE NOT NULL
    - `name` TEXT NOT NULL
    - `points` INTEGER NOT NULL
    - `created_at` INTEGER (timestamp)
  - `clans`:
    - `id` INTEGER PRIMARY KEY AUTOINCREMENT
    - `uuid` TEXT UNIQUE NOT NULL
    - `tag` TEXT UNIQUE NOT NULL
    - `name` TEXT NOT NULL
    - `color` TEXT NOT NULL
    - `created_at` INTEGER
    - Dodatkowe indeksy po `tag`, `uuid`.
  - `clan_members`:
    - `clan_id` INTEGER NOT NULL
    - `player_uuid` TEXT NOT NULL
    - `role` TEXT NOT NULL (enum: LEADER, OFFICER, MEMBER)
    - `joined_at` INTEGER
    - PRIMARY KEY (`clan_id`, `player_uuid`)
  - `invites` (opcjonalne):
    - `id` INTEGER PRIMARY KEY AUTOINCREMENT
    - `clan_id` INTEGER NOT NULL
    - `target_uuid` TEXT NOT NULL
    - `inviter_uuid` TEXT NOT NULL
    - `created_at` INTEGER
    - `expires_at` INTEGER (dla automatycznego wygaszania).
- Migracje:
  - Tabela `schema_version` z numerem wersji.
  - Na starcie plugin porównuje aktualną wersję kodową z DB i wykonuje sekwencję `ALTER TABLE`/`CREATE TABLE` w transakcjach.
  - Migracje są idempotentne, dobrze zalogowane.
- Operacje async:
  - Dedykowany `ExecutorService` (np. `Executors.newFixedThreadPool(n)`) lub `BukkitScheduler#runTaskAsynchronously`.
  - Wszystkie operacje IO do DB wykonywane poza main-thread.
  - API serwisów zwraca dane w cache natychmiast, a zapis do DB jest wykonywany w tle (write-behind) lub w stylu:
    - operacje, które wymagają aktualnej wartości (np. join clan + weryfikacja istniejącego klanu) – najpierw czytanie z cache/DB w async, wynik przekazywany z powrotem przez callback lub sync-task.

### 3.4 Warstwa domenowa (serwisy)

#### 3.4.1 Modele

- `Clan`:
  - `UUID id`
  - `String tag`
  - `String name`
  - `String colorCode` lub `String displayColor` (kody MiniMessage / legacy).
  - `Instant createdAt`
  - `UUID ownerUuid`
  - Kolekcja członków (UUID + rola).
- `ClanMemberRole` (enum): `LEADER`, `OFFICER`, `MEMBER`.
- `PlayerProfile`:
  - `UUID uuid`
  - `String name`
  - `int points`
  - `Clan` lub `UUID clanId` (nullable)
  - `Instant createdAt`
- `Invite`:
  - `Clan clan`
  - `UUID targetUuid`
  - `UUID inviterUuid`
  - `Instant createdAt`
  - `Instant expiresAt`

#### 3.4.2 Serwisy

- `ClanService`:
  - Tworzenie klanu:
    - Walidacja tagu/nazwy (limity, regex, blacklist, „puste” nazwy, powtarzające się znaki).
    - Sprawdzenie unikalności taga/nazwy.
    - Zapis do DB + aktualizacja cache.
  - Zarządzanie członkostwem:
    - `invite`, `join`, `leave`, `kick`, `transferLeader`.
    - Spójne reguły uprawnień (tylko leader/officer).
    - Obsługa sytuacji, gdy leader opuszcza klan → wymuszenie transferu lub rozwiązanie klanu.
  - Dane klanu:
    - Pobieranie klanu po tagu/nazwie/UUID.
    - Obliczanie średnich punktów klanu (na podstawie bieżącego cache punktów graczy).
  - Ranking klanów:
    - Funkcja zwracająca listę posortowanych klanów po średnich punktach.
- `PointsService`:
  - Utrzymywanie bieżącej liczby punktów graczy w pamięci.
  - Inicjalizacja nowego gracza do `startPoints` z configu.
  - Modyfikacje punktów:
    - `applyKill(killerUuid, victimUuid)`.
    - `applySelfDeath(playerUuid, cause)`.
    - Operacje admina: `setPoints`, `addPoints`, `recalc` (przeliczenie rankingów / średnich).
  - Algorytm skalowania:
    - Konfigurowalne parametry (`baseKillReward`, `factor`, `minChange`, `maxChange`).
    - Propozycja wzoru (dla zabijającego):
      - `delta = base + (victimPoints - killerPoints) * factor`.
      - Następnie `delta` clampowane do `[minChange, maxChange]`.
      - Dla ofiary analogicznie, z własnymi parametrami lub `loss = delta`/`loss = baseLoss + ...`.
    - Dodatkowy clamp na końcowe punkty gracza `[minPoints, maxPoints]`.
  - Ranking graczy:
    - Metoda zwracająca posortowaną listę graczy wg punktów malejąco.
- `PlayerService`:
  - Ładowanie profili przy join (async, później sync-insert do cache).
  - Zapisywanie przy quit / okresowe flush’e do DB.
  - Mapowanie UUID ↔ nazwa gracza (dla komend admina).

### 3.5 Warstwa UI (title, wiadomości, placeholdery)

- Użycie Adventure API (`Component`, `Title`) lub aktualnego API Spigot 1.21 do wyświetlania:
  - `Title` po zabójstwie/śmierci:
    - Title: z domyślną emotką 💀 (konfigurowalna).
    - Subtitle: liczby zdobytych/straconych punktów, formatowane według configu.
    - Czas: `fadeIn`, `stay`, `fadeOut` z configu.
  - Wiadomości czatowe:
    - Szablony w `config.yml` z placeholderami:
      - Punkty: przed/po, zmiana.
      - Klan: tag/nazwa/kolor, punkty klanu.
  - System placeholderów wewnętrznych pluginu (np. `%points_before%`, `%points_after%`, `%delta%`, `%clan_tag%`, `%clan_name%`, `%clan_points%`, itp.) do podmiany w stringach z configu.
  - Zapewnienie poprawnego UTF-8:
    - Plik `config.yml` generowany w UTF-8.
    - Wszystkie stringi w kodzie w UTF-8 (domyślne dla Javy).

### 3.6 PlaceholderAPI

- Klasa: `FepboxKlanyPlaceholderExpansion` dziedzicząca z `PlaceholderExpansion` (gdy PAPI jest obecne).
- Rejestracja:
  - W `onEnable()` sprawdzenie `Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null`.
  - Przy obecności PAPI – rejestracja expansion, w przeciwnym razie log informacyjny i bez crasha.
- Obsługiwane placeholdery:
  - `%fepbox_klan_tag%`
  - `%fepbox_klan_name%`
  - `%fepbox_klan_color%`
  - `%fepbox_klan_display%` – np. `[TAG] Nazwa` z kolorem.
  - `%fepbox_points%`
  - `%fepbox_points_formatted%` (np. z tysięcznymi separatorami).
  - `%fepbox_clan_points%`
  - `%fepbox_rank_position%`
  - `%fepbox_clan_rank_position%`
- Brak klanu:
  - Użycie konfigurowalnego tekstu `noClanText` (np. `-`).

### 3.7 Komendy i tab-completion

- Struktura komend:
  - Root: `/klan` (`/klany` jako alias opcjonalnie).
    - `/klan zaloz <TAG> <NAZWA>`
    - `/klan zapros <gracz>`
    - `/klan dolacz <klan|tag>`
    - `/klan opusc`
    - `/klan wyrzuc <gracz>`
    - `/klan info [klan|tag]`
    - `/klan top [gracze|klany] [strona]`
    - `/klan punkty [gracz]`
    - `/klan reload` (tylko z permisją `fepboxklany.reload`).
  - Admin: `/fepboxklany admin ...`
    - `forcejoin <gracz> <tag|klan>`
    - `forcekick <gracz>`
    - `rename <tag|klan> <nowa_nazwa>`
    - `retag <tag|klan> <nowy_tag>`
    - `setpoints <gracz> <wartosc>`
    - `addpoints <gracz> <wartosc>`
    - `recalc` (przeliczenie punktów/średnich wg aktualnych danych).
- Permisje:
  - `fepboxklany.use` – podstawowy dostęp do `/klan`.
  - `fepboxklany.clan.*` – pełen dostęp do komend klanu.
  - `fepboxklany.admin.*` – wszystkie komendy admina.
  - `fepboxklany.reload` – tylko reload.
- Implementacja:
  - Dedykowany system komend (np. własny dispatcher) zamiast logiki w jednej klasie.
  - Walidacja argumentów:
    - Czytelne błędy (brak gracza, brak klanu, brak uprawnień, itp.).
  - Tab-completion:
    - Subkomendy `/klan`.
    - Gracze online.
    - Tag/nazwy klanów.
    - Opcje `gracze|klany` przy `/klan top`.

### 3.8 Ranking / Topki

- Ranking graczy:
  - Wyznaczany na podstawie bieżącego cache punktów (dla online i offline, jeśli dane są w pamięci lub można je doczytać).
  - Sortowanie malejąco po punktach.
- Ranking klanów:
  - Dla każdego klanu obliczana średnia punktów członków:
    - Na podstawie punktów z cache; dla niezaładowanych graczy można:
      - Albo zakładać, że cache zawiera wszystkich (pre-load przy starcie).
      - Albo doczytywać w tle przy obliczaniu rankingu (droższe, ale dokładniejsze).
    - Aby spełnić wymaganie „zawsze zgodna z danymi”, preferowany wariant: pre-load wszystkich profili przy starcie pluginu (z DB) i trzymanie ich w cache, plus zapisywanie zmian do DB w tle.
  - Sortowanie po średniej malejąco.
- Paginacja:
  - W configu `ranking.pageSize`.
  - `/klan top gracze [strona]` / `/klan top klany [strona]`.
  - Czytelny format: numeracja pozycji, kolory, wyróżnienie gracza wywołującego (np. inny kolor tła lub gwiazdka).

### 3.9 Edge-case’y i stabilność

- Logout podczas walki:
  - Listener `EntityDamageByEntityEvent` zapisuje informację o ostatnim atakującym i czasie.
  - Jeśli gracz ginie bez bezpośredniego killer-a, ale w krótkim czasie po PVP (np. 10s), przypisujemy kill do ostatniego atakującego.
  - Jeśli gracz wyloguje się w trakcie takiego „combat tagu”, można:
    - albo potraktować to jak śmierć z własnej winy (bez zabójcy) – modyfikacja punktów wg `selfDeathLoss`,
    - albo zignorować (decyzja konfigurowalna, ale domyślnie kara, by uniknąć exploitów).
- Równoczesne invite/join:
  - `ClanService` trzyma w pamięci strukturę `invites` i operacje na niej są synchronizowane (np. przez `synchronized` lub dedykowany wątek).
  - Sprawdzenie ważności zaproszenia przy join (czas + czy klan nadal istnieje).
- Zmiana nicku:
  - System identyfikuje graczy wyłącznie po UUID.
  - Pole `name` w tabeli `players` jest aktualizowane przy każdym join, jeśli się zmieni.
- Brak klanu:
  - Wszystkie komendy i placeholdery obsługują brak klanu (użytkownik dostaje czytelny komunikat / `noClanText`).
- Pętle eventów śmierci:
  - Logika modyfikacji punktów umieszczona wyłącznie w jednym miejscu (np. `PlayerDeathListener`), z ochroną przed wielokrotnym przetwarzaniem tego samego eventu (flagi lokalne).

### 3.10 Algorytm punktów PvP

- Składniki konfigurowalne:
  - `points.start`: domyślne punkty startowe (1000).
  - `points.min`: minimalne punkty (0).
  - `points.max`: maksymalne punkty (opcjonalne, można wyłączyć clamp od góry).
  - `points.kill.baseReward` – bazowa nagroda za kill.
  - `points.kill.factor` – współczynnik różnicy rankingów (im silniejsza ofiara względem zabójcy, tym większy bonus).
  - `points.kill.minChange` / `points.kill.maxChange` – clamp dla zmian.
  - `points.death.baseLoss` / `points.death.factor` / clamp – analogicznie dla utraty punktów ofiary.
  - `points.selfDeath.defaultLoss` – domyślna kara za śmierć bez zabójcy.
  - `points.selfDeath.causes.<cause>` – nadpisanie per przyczyna (fall, lava, fire, cactus, starvation, explosion, drowning, itp.).
- Przykładowy wzór (killer):
  - `deltaKiller = baseReward + (victimPoints - killerPoints) * factor`.
  - Zastosowanie clamp `[minChange, maxChange]`.
- Ofiara:
  - `deltaVictim = baseLoss + (killerPoints - victimPoints) * factorLoss`.
  - Clamp `[minLoss, maxLoss]`.
  - Aplikacja jako ujemna zmiana (odejmowanie punktów).
- Utrzymanie stabilności:
  - Dodatkowe zabezpieczenie przed exploitami:
    - Konfigurowalny minimalny czas między zdobyciami punktów na tym samym przeciwniku.
    - Możliwość wyłączenia przyznawania punktów za kill w tej samej drużynie (klan vs klan).

## 4. Struktura kodu (pakiety / klasy)

- `pl.fepbox.klany`
  - `FepboxKlanyPlugin` – main.
- `pl.fepbox.klany.config`
  - `ConfigManager`
  - `LimitsConfig`, `FilterConfig`, `PointsConfig`, `UIConfig`, `RankingConfig`, `PlaceholderConfig`, `StorageConfig`.
- `pl.fepbox.klany.storage`
  - `DatabaseManager` / `SqliteDatabase`
  - `MigrationRunner`
  - `PlayerRepository`
  - `ClanRepository`
  - `InviteRepository`
- `pl.fepbox.klany.model`
  - `Clan`
  - `ClanMemberRole`
  - `PlayerProfile`
  - `Invite`
- `pl.fepbox.klany.service`
  - `ClanService`
  - `PointsService`
  - `PlayerService`
  - `InviteService`
- `pl.fepbox.klany.command`
  - `ClanCommand` (dispatcher dla `/klan`).
  - `ClanAdminCommand` (dispatcher dla `/fepboxklany admin`).
  - `KlanTabCompleter`, `KlanAdminTabCompleter`.
- `pl.fepbox.klany.listener`
  - `PlayerJoinListener`
  - `PlayerQuitListener`
  - `PlayerDeathListener`
  - `CombatTagListener` (EntityDamageByEntityEvent).
- `pl.fepbox.klany.placeholder`
  - `FepboxKlanyPlaceholderExpansion`.
- `pl.fepbox.klany.ui`
  - `TitleService` (wysyłanie title/subtitle).
  - `MessageFormatter` / `PlaceholderFormatter` (wewnętrzne placeholdery).
- `pl.fepbox.klany.util`
  - `NameValidationUtil` (tag/nazwa klanu).
  - `TextUtil` (obsługa kolorów, MiniMessage/legacy).
  - `NumberFormatUtil` (formatowanie punktów).

## 5. Konfiguracja – `config.yml`

### 5.1 Główne sekcje

- `limits`:
  - `tagMaxLength: 4`
  - `nameMaxLength: 16`
- `filter`:
  - `allowedTagRegex: "^[A-Za-z]{2,4}$"` (przykład)
  - `allowedNameRegex: "^[A-Za-z0-9_ ]{3,16}$"`
  - `blockedWords: ["admin", "moderator", "owner", ...]`
  - Dodatkowe reguły: blokada nazw będących zlepkiem powtarzających się znaków (np. "aaaa", "!!!!!") – obsługiwane w kodzie.
- `points`:
  - `startPoints: 1000`
  - `minPoints: 0`
  - `maxPoints: 0` (0 = brak limitu górnego, np.)
  - `kill:` – podsekcja z parametrami scalingu.
  - `death:` – parametry utraty punktów.
  - `selfDeath:`:
    - `defaultLoss: 10`
    - `causes:`:
      - `FALL: 10`
      - `LAVA: 15`
      - `DROWNING: 12`
      - itd.
- `ui`:
  - `skullSymbol: "💀"`
  - `titles:`:
    - `kill:`:
      - `title: "<skull> ZABÓJSTWO <skull>"`
      - `subtitle: "+<delta_killer> | -<delta_victim>"`
    - `selfDeath:`:
      - `title: "<skull> ŚMIERĆ <skull>"`
      - `subtitle: "-<delta_victim>"`
    - czasy: `fadeIn`, `stay`, `fadeOut`.
  - `messages:`:
    - `kill:`:
      - `killer: "Zabiłeś <victim_name> (+<delta_killer>)"`
      - `victim: "Zginąłeś z rąk <killer_name> (-<delta_victim>)"`
    - `selfDeath:`:
      - per przyczyna: `FALL`, `LAVA`, itd., każdy z placeholderem straconych punktów.
- `ranking`:
  - `pageSize: 10`
  - Szablony linii rankingu (prefix, numer, nick/tag, punkty).
- `placeholders`:
  - `noClanText: "-"`.
- `storage`:
  - `type: sqlite`
  - `file: "plugins/Fepbox-Klany/data.db"`

### 5.2 Walidacja configu

- Dla każdego pola:
  - Sprawdzenie typu (np. liczba vs string).
  - Sprawdzenie zakresu (np. `tagMaxLength >= 1`).
  - Dla regexów – próba kompilacji, w razie błędu fallback do bezpiecznego domyślnego regexu.
  - Dla `blockedWords` – normalizacja (lowercase, trim).
  - Dla `selfDeath.causes` – mapowanie kluczy na `DamageCause` (ignorowanie niepoprawnych, log ostrzegawczy).

## 6. Dane / API / interfejsy

### 6.1 API serwisów

- `ClanService` (interfejs + implementacja):
  - `Optional<Clan> getClanByTag(String tag)`
  - `Optional<Clan> getClanByName(String name)`
  - `Optional<Clan> getClanByPlayer(UUID playerUuid)`
  - `Clan createClan(Player creator, String tag, String name, String color)`
  - `void invitePlayer(Clan clan, Player inviter, OfflinePlayer target)`
  - `void joinClan(Player target, String tagOrName)`
  - `void leaveClan(Player player)`
  - `void kickMember(Player actor, OfflinePlayer target)`
  - `void transferLeadership(Player actor, OfflinePlayer newLeader)`
  - `List<Clan> getTopClans(int page, int pageSize)`
  - `double getClanAveragePoints(Clan clan)`
- `PointsService`:
  - `int getPoints(UUID uuid)`
  - `void setPoints(UUID uuid, int value, Reason reason)`
  - `void addPoints(UUID uuid, int delta, Reason reason)`
  - `void applyKill(UUID killerUuid, UUID victimUuid)`
  - `void applySelfDeath(UUID uuid, DamageCause cause)`
  - `List<PlayerProfile> getTopPlayers(int page, int pageSize)`
  - `int getRankPosition(UUID uuid)`
  - `int getClanRankPosition(Clan clan)`
- `PlayerService`:
  - `PlayerProfile getOrCreateProfile(UUID uuid, String currentName)`
  - `void loadProfileAsync(UUID uuid, Consumer<PlayerProfile> callback)`
  - `void saveProfileAsync(PlayerProfile profile)`

### 6.2 Util / walidacja nazw

- `NameValidationUtil`:
  - Metody:
    - `boolean isValidTag(String tag, Config cfg)`
    - `boolean isValidName(String name, Config cfg)`
    - `ValidationResult validateTag(...)`, `ValidationResult validateName(...)` – z informacją o błędzie dla UI (np. „Tag za długi”, „Zawiera niedozwolone znaki”, „Zabronione słowo”, „Za dużo powtórzeń tego samego znaku”).
  - Reguły:
    - trymowanie whitespace.
    - odrzucenie pustych stringów lub składających się tylko ze spacji / znaków specjalnych.
    - limit długości (max z configu).
    - regex (allowed).
    - blacklist słów (case-insensitive).
    - blokada „spamowych” patternów (np. 1 unikalny znak powtórzony >70% długości).

## 7. Weryfikacja / testowanie

### 7.1 Budowa i uruchomienie

- Maven:
  - `mvn clean package` – generuje `Fepbox-Klany.jar`.
  - Upewnienie się, że `.gitignore` zawiera: `target/`, ewentualne logi, pliki DB.
- Manualne testy na lokalnym serwerze testowym (Paper 1.21.4):
  - Start serwera z pluginem.
  - Weryfikacja, że plugin startuje bez błędów, tworzy `config.yml` i `data.db`.

### 7.2 Scenariusze testowe (wysoki poziom)

- Tworzenie klanu:
  - Prawidłowy tag/nazwa → klan powstaje, gracz jest leaderem.
  - Za długi tag / nazwa → poprawny komunikat, brak tworzenia.
  - Niedozwolone znaki / słowa → blokada.
- Punkty:
  - Nowy gracz → 1000 pkt.
  - Kill gracza o niższym/wyszym rankingu → różne wartości `delta`.
  - Clamp na min/max punktów działa (brak zejścia poniżej 0).
- Śmierci:
  - Śmierć z zabójcą → Title + chat + poprawne punkty.
  - Śmierć bez zabójcy (różne przyczyny) → Title + chat + poprawna kara i wiadomość.
- Ranking:
  - `/klan top gracze` i `/klan top klany` – sortowanie, paginacja, wyróżnienie wywołującego.
- PlaceholderAPI:
  - Z PAPI – placeholdery zwracają poprawne wartości.
  - Bez PAPI – brak crasha, log z informacją o pominięciu integracji.

### 7.3 Testy jednostkowe (opcjonalne, ale zalecane)

- Testy dla:
  - `NameValidationUtil` – różne warianty tagów/nazw.
  - Algorytmów punktowych (`PointsService`) – sprawdzenie wartości `delta` przy różnych kombinacjach.
  - Funkcji liczenia średnich punktów klanów.

---

Ta specyfikacja definiuje architekturę, modele danych, główne serwisy oraz zachowanie pluginu Fepbox-Klany zgodnie z wymaganiami, umożliwiając implementację w kolejnych krokach (warstwy domeny, storage, UI, komendy i integracje).

