# Fepbox-Klany

System klanów i ranking punktów PvP dla serwerów Minecraft Paper/Spigot 1.21.4.  
Plugin zapewnia produkcyjny system klanów, punkty PvP (Elo‑like), integrację z PlaceholderAPI, rozbudowaną konfigurację, tytuły po zabójstwie i śmierci oraz sojusze między klanami.

## Wymagania

- **Serwer**: Paper/Spigot 1.21.4 (lub kompatybilny z `api-version: 1.21`)
- **Java**: 21
- **Opcjonalnie**: [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) – placeholdery klanów i punktów

## Funkcje

- Klany z tagiem, nazwą, kolorem i rolami (leader/officer/member).
- Punkty PvP dla graczy (start domyślnie 1000), ranking graczy i klanów.
- Punkty klanu liczone jako średnia punktów członków (spójne z danymi).
- Algorytm zmiany punktów zależny od różnicy rankingów (skalowanie, clamp min/max).
- Kary za śmierć bez zabójcy (upadek, lawa, utonięcie itd.) z osobnymi komunikatami.
- Tytuły (Title + Subtitle) po zabójstwie i śmierci z emotką 💀 (konfigurowalną).
- Integracja z PlaceholderAPI (tag/nazwa/kolor/średnia klanu, punkty gracza, formatowane punkty).
- Sojusze między klanami (dwustronne, komenda `/klan sojusz`).
- Zmiana koloru klanu komendą `/klan kolor`.
- Storage SQLite (`plugins/Fepbox-Klany/data.db`), wszystkie operacje DB asynchronicznie, z cache w pamięci.

## Instalacja (serwer)

1. Zbuduj plugin:
   ```bash
   mvn clean package
   ```
   Wynik: `target/Fepbox-Klany-1.0.0.jar`
2. Skopiuj `Fepbox-Klany-1.0.0.jar` do `plugins/` serwera.
3. (Opcjonalnie) Zainstaluj PlaceholderAPI.
4. Uruchom serwer – plugin utworzy:
   - `plugins/Fepbox-Klany/config.yml`
   - `plugins/Fepbox-Klany/data.db`

## Budowanie (deweloper)

Najważniejsze z `pom.xml`:

- `groupId`: `pl.fepbox`
- `artifactId`: `Fepbox-Klany`
- `version`: `1.0.0`
- Zależności:
  - `org.spigotmc:spigot-api:1.21.4-R0.1-SNAPSHOT` (scope `provided`)
  - `me.clip:placeholderapi:2.11.6` (scope `provided`, repo: `https://repo.extendedclip.com/content/repositories/placeholderapi/`)
  - `org.xerial:sqlite-jdbc` (shade + relokacja)

Wymagane jest lokalne API Spigot/Paper 1.21.4.

## Konfiguracja

Najważniejsze sekcje `config.yml`:

- `limits` – limity:
  - `tagMaxLength` – długość tagu (domyślnie 4)
  - `nameMaxLength` – długość nazwy klanu (domyślnie 16)
- `filter` – walidacja:
  - `allowedTagRegex`, `allowedNameRegex`
  - `blockedWords` – lista zbanowanych słów
- `points`:
  - `startPoints`, `minPoints`, `maxPoints`
  - `killScaling.baseReward`, `factor`, `minChange`, `maxChange`
  - `selfDeathLoss.defaultLoss`, `selfDeathLoss.causes.<DAMAGE_CAUSE>`
- `ui`:
  - `skullSymbol` – emotka (domyślnie 💀)
  - `titles.kill`, `titles.selfDeath`, `titles.timings`
  - `messages.kill`, `messages.selfDeath`
- `ranking`:
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
  Tworzy nowy klan (walidacja + blokada duplikatów).

- `/klan info [klan|tag]`  
  Informacje o klanie:
  - bez argumentu – klan gracza,
  - z argumentem – klan po tagu lub nazwie.

- `/klan punkty [gracz]`  
  Punkty PvP:
  - bez argumentu – własne,
  - z argumentem – wybranego, online gracza.

- `/klan zapros <gracz>`  
  Lider zaprasza (w tej wersji: od razu dodaje) gracza do swojego klanu.

- `/klan opusc`  
  Gracz opuszcza swój klan. Lider nie może wyjść, jeśli są inni członkowie (najpierw przekazanie lidera lub `/klan rozwiaz`).

- `/klan wyrzuc <gracz>`  
  Lider wyrzuca gracza ze swojego klanu (nie można wyrzucić siebie).

- `/klan rozwiaz`  
  Lider rozwiązuje swój klan – usuwa członków, sojusze i rekord klanu z bazy.

- `/klan sojusz <tag|nazwa>`  
  Lider przełącza (toggle) sojusz z innym klanem:
  - jeśli sojusz istnieje – jest zrywany,
  - jeśli nie istnieje – jest tworzony.

- `/klan kolor <kod>`  
  Lider ustawia kolor klanu, np. `&a`, `&b`, `&c`.  
  Kolor jest przechowywany jako `§x` i używany m.in. w placeholderze `%fepbox_klan_display%`.

### Admin (`/fepboxklany admin`)

- `/fepboxklany admin setpoints <gracz> <wartosc>`  
  Ustawia dokładną liczbę punktów PvP.

- `/fepboxklany admin addpoints <gracz> <wartosc>`  
  Dodaje / odejmuje punkty PvP.

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

- `%fepbox_klan_tag%` – tag klanu gracza (lub `noClanText`).
- `%fepbox_klan_name%` – nazwa klanu.
- `%fepbox_klan_color%` – kolor klanu (kod `§x`).
- `%fepbox_klan_display%` – tag klanu w nawiasach `[` `]` z kolorem, np. `&a[TEST]`.
- `%fepbox_points%` – punkty PvP gracza.
- `%fepbox_points_formatted%` – punkty z formatowaniem tysiącowym.
- `%fepbox_clan_points%` – średnia punktów klanu gracza (zaokrąglona).

## System punktów i tytułów

- Nowy gracz startuje z `points.startPoints` (domyślnie 1000).
- Zabójstwo:
  - zabójca: `+delta`, ofiara: `-delta`,
  - `delta` zależy od różnicy rankingów (parametry `killScaling`),
  - Title (np. `💀 ZABÓJSTWO 💀`) + Subtitle z dokładną liczbą zdobytych/straconych punktów,
  - wiadomości czatu dla zabójcy i ofiary z aktualnym stanem punktów.
- Śmierć bez zabójcy:
  - kara `selfDeathLoss` (per przyczyna lub `defaultLoss`),
  - Title (np. `💀 ŚMIERĆ 💀`) + Subtitle z utraconymi punktami,
  - wiadomości czatu zależne od przyczyny (z fallbackiem).

## Dane i wydajność

- **Storage**: SQLite (`plugins/Fepbox-Klany/data.db`).
- Tabele:
  - `players` – UUID, nazwa, punkty, czas utworzenia,
  - `clans` – UUID klanu, tag, nazwa, kolor, owner UUID, czas utworzenia,
  - `clan_members` – przypisanie graczy do klanów + rola,
  - `clan_allies` – relacje sojuszu (`clan_id`, `ally_clan_id`).
- **Cache**:
  - Punkty graczy i dane klanów trzymane w pamięci, zapisy do DB asynchronicznie (bez blokowania main-thread).

## Status funkcjonalności

Aktualna wersja implementuje:

- System punktów PvP + tytuły/wiadomości po śmierci/zabójstwie.
- System klanów z tworzeniem, opuszczaniem, wyrzucaniem, rozwiązaniem i średnią punktów.
- Proste zapraszanie (`/klan zapros`) i sojusze (`/klan sojusz`).
- Zmianę koloru klanu (`/klan kolor`).
- Integrację z PlaceholderAPI.
- Storage SQLite z cache i asynchronicznymi operacjami.

## Licencja

Brak jawnie określonej licencji – traktuj jako kod prywatny, chyba że zdecydujesz się opublikować go z wybraną licencją.
