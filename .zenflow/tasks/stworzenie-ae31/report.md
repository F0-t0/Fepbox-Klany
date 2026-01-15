## Wykonanie zadania: Fepbox-Klany

- Utworzono kompletny projekt Maven (`pom.xml`) dla pluginu Fepbox-Klany pod IntelliJ z Java 21, zależnościami `spigot-api`, `sqlite-jdbc` i PlaceholderAPI (scope `provided`) oraz konfiguracją shade (relokacja SQLite).
- Dodano pliki zasobów `plugin.yml` i `config.yml` z nazwą pluginu, komendami (`/klan`, `/fepboxklany`), permisjami, softdepend na PlaceholderAPI i rozbudowaną, walidowaną konfiguracją (limity tag/nazwa, punkty, kary za śmierć, tytuły, wiadomości, ranking, placeholdery, storage SQLite).
- Zaimplementowano główną klasę pluginu `pl.fepbox.klany.FepboxKlanyPlugin` odpowiedzialną za start/stop, ładowanie konfiguracji, inicjalizację bazy SQLite, serwisów domenowych (profile, punkty, klany), rejestrację eventów, komend oraz integracji z PlaceholderAPI.
- Dodano warstwę konfiguracji (`config` package: `PluginConfig`, `LimitsConfig`, `FilterConfig`, `PointsConfig`, `UIConfig`, `TitlesConfig`, `RankingConfig`, `PlaceholderConfig`, `StorageConfig`, `PluginConfigLoader`) z walidacją i sensownymi wartościami domyślnymi zgodnie ze specyfikacją.
- Zaimplementowano warstwę storage SQLite w `db.DatabaseManager` z tworzeniem katalogów, tabel (`players`, `clans`, `clan_members`) i zarządzaniem połączeniem.
- Dodano system profili graczy (`player.PlayerProfile`, `PlayerProfileService`, `PlayerProfileServiceImpl`) z cache w pamięci, inicjalizacją nowych graczy do 1000 punktów, aktualizacją nickname'ów i asynchronicznym zapisem do DB.
- Zaimplementowano system punktów PvP (`points.PointsService`, `PointsServiceImpl`, `KillResult`) z cache, algorytmem skalowania nagrody zależnym od różnicy rankingów (parametry w configu), clampowaniem do min/max, obsługą śmierci własnej (per przyczyna) oraz asynchronicznym zapisem zmian do DB.
- Zaimplementowano podstawowy system klanów (`clan.Clan`, `ClanRole`, `ClanService`, `ClanServiceImpl`) z ładowaniem klanów i członków z DB, tworzeniem klanu (leader, UUID, tag, nazwa, kolor), dołączaniem, opuszczaniem, wyrzucaniem członków, wyliczaniem średnich punktów klanu oraz rankingiem klanów po średniej.
- Dodano listenery (`listener.PlayerConnectionListener`, `PlayerCombatListener`) obsługujące inicjalizację profilu przy joinie oraz logikę PvP/śmierci: naliczanie punktów, wyświetlanie tytułów (Title + Subtitle z symbolem 💀 z configu) z prawidłowymi wartościami zdobytych/straconych punktów, customowe wiadomości na czacie dla zabójstwa i śmierci własnej (różne dla przyczyn).
- Zintegrowano PlaceholderAPI poprzez `placeholder.FepboxKlanyPlaceholderExpansion` z placeholderami: `%fepbox_klan_tag%`, `%fepbox_klan_name%`, `%fepbox_klan_color%`, `%fepbox_klan_display%`, `%fepbox_points%`, `%fepbox_points_formatted%`, `%fepbox_clan_points%` (obsługa braku klanu przez `noClanText` z configu).
- Zaimplementowano komendy `/klan` i `/fepboxklany` z executorami i tab-completion (`command.ClanCommand`, `FepboxKlanyAdminCommand`) obejmujące m.in. tworzenie klanu, podgląd informacji o klanie, punkty PvP gracza oraz administracyjne ustawianie/dodawanie punktów.
- Upewniono się, że `.gitignore` ignoruje typowe artefakty generowane (`target/`, logi, katalogi buildowe), aby buildy nie zaśmiecały repozytorium.

### Testowanie

- Próba uruchomienia `mvn package` zakończyła się błędem, ponieważ w środowisku nie jest dostępne polecenie `mvn`; struktura i konfiguracja projektu są jednak standardowe i powinny budować się poprawnie na maszynie z Mavenem i zainstalowanym API Spigot/Paper w lokalnym repozytorium.
- Logika została zweryfikowana statycznie (spójność typów, importów, zależności między serwisami i eventami).

### Największe wyzwania

- Ograniczenia środowiska (brak Mavena) uniemożliwiły pełne zbudowanie i uruchomienie pluginu z poziomu tego zadania; projekt został przygotowany tak, by był gotowy do kompilacji po stronie użytkownika.
- Konieczność podziału dużych zmian na wiele mniejszych patchy z powodu ograniczeń narzędzia `apply_patch` w środowisku Windows (błąd "The filename or extension is too long") wymagała ostrożnego wprowadzania plików krok po kroku.

