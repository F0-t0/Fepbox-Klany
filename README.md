# Fepbox-Klany

System klanów i ranking punktów PvP dla serwerów Minecraft Paper/Spigot 1.21.4.  
Plugin zapewnia produkcyjny system klanów, punkty PvP (Elo‑like), integrację z PlaceholderAPI, rozbudowaną konfigurację oraz tytuły po zabójstwie i śmierci, z obsługą sojuszy między klanami.

## Wymagania

- **Serwer**: Paper/Spigot 1.21.4 (lub kompatybilny z `api-version: 1.21`)
- **Java**: 21 (zgodnie z konfiguracją Mavena)
- **Opcjonalnie**: [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) – placeholdery klanów i punktów

## Funkcje

- Klany z tagiem, nazwą, kolorem i rolami (leader/officer/member).
- Punkty PvP dla graczy (start domyślnie 1000), ranking graczy i klanów.
- Punkty klanu liczone jako średnia punktów członków (zawsze spójna z danymi).
- Algorytm zmiany punktów zależny od różnicy rankingów (skalowanie, clamp min/max).
- Kary za śmierć bez zabójcy (upadek, lawa, utonięcie itd.) z osobnymi komunikatami.
- Tytuły (Title + Subtitle) po zabójstwie i śmierci z prawdziwą emotką 💀 (konfigurowalną).
- Integracja z PlaceholderAPI (tag/nazwa/kolor/średnia klanu, punkty gracza, formatowane punkty).
- Sojusze między klanami (dwustronne, zapisywane w bazie, komenda `/klan sojusz`).
- Storage SQLite (`plugins/Fepbox-Klany/data.db`), wszystkie operacje DB asynchronicznie, z cache w pamięci.

## Instalacja (serwer)

1. Zbuduj plugin lub pobierz gotowy `.jar`:
   - Build lokalnie: `mvn clean package`
   - Wynik znajdziesz w `target/Fepbox-Klany-1.0.0.jar`
2. Skopiuj `Fepbox-Klany-1.0.0.jar` do katalogu `plugins/` serwera.
3. (Opcjonalnie) Zainstaluj PlaceholderAPI, jeśli chcesz używać placeholderów.
4. Uruchom serwer – plugin utworzy:
   - `plugins/Fepbox-Klany/config.yml`
   - `plugins/Fepbox-Klany/data.db`

## Budowanie (deweloper)

Projekt oparty jest o Maven.

Najważniejsze informacje z `pom.xml`:

- `groupId`: `pl.fepbox`
- `artifactId`: `Fepbox-Klany`
- `version`: `1.0.0`
- Zależności:
  - `org.spigotmc:spigot-api:1.21.4-R0.1-SNAPSHOT` (scope `provided`)
  - `me.clip:placeholderapi:2.11.6` (scope `provided`, repozytorium `https://repo.extendedclip.com/content/repositories/placeholderapi/`)
  - `org.xerial:sqlite-jdbc` (wpakowane przez maven-shade-plugin, z relokacją)

Budowanie:

```bash
mvn clean package
```

Wymagane jest lokalne API Spigot/Paper 1.21.4 (np. przez BuildTools lub paperweight).

## Konfiguracja

Plik `config.yml` generuje się przy pierwszym uruchomieniu. Kluczowe sekcje:

- `limits` – limity długości:
  - `tagMaxLength` – maksymalna długość tagu (domyślnie 4)
  - `nameMaxLength` – maksymalna długość nazwy klanu (domyślnie 16)
- `filter` – walidacja nazw:
  - `allowedTagRegex` – regex dopuszczalnych znaków taga
  - `allowedNameRegex` – regex nazwy klanu
  - `blockedWords` – lista zbanowanych słów (case‑insensitive)
- `points` – punkty PvP:
  - `startPoints` – punkty startowe nowego gracza
  - `minPoints` / `maxPoints` – clamp globalny
  - `killScaling` – algorytm nagrody za zabójstwo:
    - `baseReward`, `factor`, `minChange`, `maxChange`
  - `selfDeathLoss` – kary za śmierć własną:
    - `defaultLoss`
    - `causes.<DAMAGE_CAUSE>` – per przyczyna (np. `FALL`, `LAVA`, `DROWNING`, ...)
- `ui` – tytuły i wiadomości:
  - `skullSymbol` – emotka używana w tytułach (domyślnie 💀)
  - `titles.kill` / `titles.selfDeath` – szablony Title / Subtitle
  - `titles.timings` – `fadeIn`, `stay`, `fadeOut` (ticki)
  - `messages.kill` / `messages.selfDeath` – wiadomości czatu (kolory `&`)
- `ranking` – top-listy:
  - `pageSize`
  - `playerFormat`, `playerFormatSelf`, `clanFormat`, `clanFormatSelf`
- `placeholders`:
  - `noClanText` – tekst dla gracza bez klanu
- `storage`:
  - `type: sqlite`
  - `file: plugins/Fepbox-Klany/data.db`

## Komendy

### Gracz (`/klan`)

- `/klan zaloz <TAG> <NAZWA>`  
  Tworzy nowy klan (walidacja tagu/nazwy, blokada duplikatów).

- `/klan info [klan|tag]`  
  Informacje o klanie:
  - bez argumentu – klan gracza,
  - z argumentem – klan po tagu lub nazwie.

- `/klan punkty [gracz]`  
  Pokazuje punkty PvP:
  - bez argumentu – własne punkty,
  - z argumentem – punkty wskazanego, online gracza.

- `/klan zapros <gracz>`  
  Lider zaprasza (w tej wersji: od razu dodaje) gracza do swojego klanu. Wysyłana jest wiadomość do zapraszanego.

- `/klan opusc`  
  Gracz opuszcza swój klan. Lider nie może opuścić klanu, jeśli są inni członkowie – powinien przekazać lidera lub użyć `/klan rozwiaz`.

- `/klan wyrzuc <gracz>`  
  Lider wyrzuca gracza ze swojego klanu (nie można wyrzucić siebie).

- `/klan rozwiaz`  
  Lider rozwiązuje swój klan – usuwa wszystkich członków, sojusze oraz wpis klanu z bazy danych.

- `/klan sojusz <tag|nazwa>`  
  Lider przełącza (toggle) sojusz z innym klanem: jeśli sojusz istnieje – zostaje zerwany, w przeciwnym razie zostaje zawarty. Relacja zapisywana jest dwustronnie.

### Admin (`/fepboxklany admin`)

- `/fepboxklany admin setpoints <gracz> <wartosc>`  
  Ustawia dokładną wartość punktów PvP danego gracza.

- `/fepboxklany admin addpoints <gracz> <wartosc>`  
  Dodaje (lub odejmuje, jeśli wartość jest ujemna) punkty PvP graczowi.

## Permisje

Zdefiniowane w `plugin.yml`:

- `fepboxklany.use` – podstawowy dostęp do `/klan` (domyślnie: `true`).

- `fepboxklany.clan.*` – wszystkie komendy klanowe (domyślnie: `op`):  
  - `fepboxklany.clan.create`  
  - `fepboxklany.clan.invite`  
  - `fepboxklany.clan.join`  
  - `fepboxklany.clan.leave`  
  - `fepboxklany.clan.kick`  
  - `fepboxklany.clan.info`  
  - `fepboxklany.clan.top`  
  - `fepboxklany.clan.points`

- `fepboxklany.admin.*` – wszystkie komendy administracyjne (domyślnie: `op`):  
  - `fepboxklany.admin.base`  
  - `fepboxklany.admin.forcejoin`  
  - `fepboxklany.admin.forcekick`  
  - `fepboxklany.admin.rename`  
  - `fepboxklany.admin.retag`  
  - `fepboxklany.admin.setpoints`  
  - `fepboxklany.admin.addpoints`  
  - `fepboxklany.admin.recalc`  
  - `fepboxklany.reload`

- `fepboxklany.reload` – prawo do przeładowania konfiguracji.

## Placeholdery (PlaceholderAPI)

Identifier: `fepbox`  
Przykładowe placeholdery:

- `%fepbox_klan_tag%` – tag klanu gracza (lub `noClanText`).
- `%fepbox_klan_name%` – nazwa klanu gracza.
- `%fepbox_klan_color%` – kolor klanu (np. `§a`).
- `%fepbox_klan_display%` – sformatowany klan, np. `[TAG] Nazwa` z kolorem.
- `%fepbox_points%` – aktualne punkty PvP gracza.
- `%fepbox_points_formatted%` – punkty z formatowaniem tysięcy.
- `%fepbox_clan_points%` – średnia punktów klanu gracza (zaokrąglona).

Gracz bez klanu otrzymuje tekst z `placeholders.noClanText` w placeholderach klanowych.

## System punktów i tytułów

- Nowy gracz startuje z `points.startPoints` (domyślnie 1000).
- Zabójstwo innego gracza:
  - zabójca dostaje `+delta`, ofiara traci `-delta`,
  - `delta` zależy od różnicy rankingów (`baseReward + factor*(victimPoints-killerPoints)`), z clampem do `[minChange, maxChange]`,
  - wysyłany jest Title (np. `💀 ZABÓJSTWO 💀`) oraz Subtitle z dokładną liczbą zdobytych/straconych punktów,
  - wysyłane są również wiadomości czatu dla zabójcy i ofiary z aktualnym stanem punktów.
- Śmierć bez zabójcy:
  - gracz traci `selfDeathLoss` dla danej przyczyny (lub `defaultLoss`),
  - wysyłany jest Title (np. `💀 ŚMIERĆ 💀`) oraz Subtitle z utraconymi punktami,
  - wiadomość czatu zależna od przyczyny (z fallbackiem na `default`).

## Dane i wydajność

- **Storage**: SQLite – plik `plugins/Fepbox-Klany/data.db`.
- Tabele:
  - `players` – UUID, nazwa, punkty, data utworzenia,
  - `clans` – UUID klanu, tag, nazwa, kolor, owner UUID, data utworzenia,
  - `clan_members` – przypisanie graczy do klanów + rola,
  - `clan_allies` – relacje sojuszu między klanami (`clan_id`, `ally_clan_id`).
- **Cache**:
  - Punkty graczy oraz dane klanów są trzymane w pamięci; zapis/odczyt z DB wykonywany jest asynchronicznie.

## Status funkcjonalności

Aktualna wersja pluginu zawiera:

- System punktów PvP z tytułami i komunikatami po śmierci/zabójstwie.
- Podstawowy system klanów (tworzenie, opuszczanie, wyrzucanie, rozwiązanie, średnia punktów).
- Proste zapraszanie do klanu (`/klan zapros` – natychmiastowe dołączenie).
- System sojuszy (`/klan sojusz`), przechowywany w bazie danych.
- Integrację z PlaceholderAPI.
- Storage SQLite z async I/O i cache.

## Licencja

Brak jawnie określonej licencji – traktuj jako kod prywatny do użytku na Twoim serwerze, chyba że zdecydujesz inaczej (np. publikując repozytorium z wybraną licencją).

