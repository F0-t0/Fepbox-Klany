# Fepbox-Klany

System klanów i ranking punktów PvP dla serwerów Minecraft Paper/Spigot 1.21.4.  
Plugin zapewnia produkcyjny system klanów, punkty PvP (Elo‑like), integrację z PlaceholderAPI, rozbudowaną konfigurację i tytuły po zabójstwie/śmierci.

## Wymagania

- **Serwer**: Paper/Spigot 1.21.4 (lub kompatybilny z `api-version: 1.21`)
- **Java**: 21 (zalecane, zgodne z `maven-compiler-plugin`)
- **Opcjonalnie**: [PlaceholderAPI](https://www.spigotmc.org/resources/placeholderapi.6245/) – dla placeholderów klanów i punktów

## Funkcje

- Klany z tagiem, nazwą, kolorem i rolami (leader/officer/member)
- Punkty PvP dla graczy (start domyślnie 1000), ranking graczy i klanów
- Punkty klanu liczone jako średnia punktów członków (zawsze spójna z danymi)
- Algorytm zmiany punktów zależny od różnicy rankingów (skalowanie, clamp min/max)
- Kara punktowa za śmierć bez zabójcy (upadek, lawa, utonięcie itd.) z osobnymi komunikatami
- Tytuły (Title + Subtitle) po zabójstwie i śmierci z prawdziwą emotką 💀 (konfigurowalną)
- Integracja z PlaceholderAPI (tag/nazwa/kolor/średnia klanu, punkty gracza, formatowane punkty)
- Storage SQLite (`plugins/Fepbox-Klany/data.db`), wszystkie operacje DB asynchronicznie

## Instalacja (serwer)

1. Zbuduj plugin lub pobierz gotowy `.jar`:
   - Build lokalnie: `mvn clean package`
   - Gotowy plik znajdziesz w `target/Fepbox-Klany-1.0.0.jar`
2. Wrzuć `Fepbox-Klany-1.0.0.jar` do `plugins/` na serwerze Paper/Spigot.
3. (Opcjonalnie) Zainstaluj PlaceholderAPI i ewentualne expansiony, jeśli chcesz używać placeholderów.
4. Uruchom serwer – plugin utworzy:
   - `plugins/Fepbox-Klany/config.yml`
   - `plugins/Fepbox-Klany/data.db`

## Budowanie (deweloper)

Projekt jest oparty o Maven:

- `pom.xml`:
  - `groupId`: `pl.fepbox`
  - `artifactId`: `Fepbox-Klany`
  - `version`: `1.0.0`
  - `spigot-api` (scope `provided`)
  - `placeholderapi` (scope `provided`) z repozytorium:
    - `https://repo.extendedclip.com/content/repositories/placeholderapi/`
  - `sqlite-jdbc` jako zależność osadzona (shade + relokacja)

Komenda do zbudowania:

```bash
mvn clean package
```

Wymagane jest zainstalowane lokalnie API Spigot/Paper dla wersji 1.21.4 (standardowo przez `BuildTools` / `paperweight`).

## Konfiguracja

Plik `config.yml` jest generowany automatycznie przy pierwszym uruchomieniu. Kluczowe sekcje:

- `limits` – limity długości:
  - `tagMaxLength` – maksymalna długość taga (domyślnie 4)
  - `nameMaxLength` – maksymalna długość nazwy klanu (domyślnie 16)
- `filter` – walidacja nazw:
  - `allowedTagRegex` – regex dla taga
  - `allowedNameRegex` – regex dla nazwy
  - `blockedWords` – lista zbanowanych słów (case‑insensitive)
- `points` – punkty PvP:
  - `startPoints` – startowa liczba punktów nowego gracza
  - `minPoints`/`maxPoints` – clamp globalny
  - `killScaling` – algorytm przy zabójstwie:
    - `baseReward`, `factor`, `minChange`, `maxChange`
  - `selfDeathLoss` – kary za śmierć własną:
    - `defaultLoss`
    - `causes.<DAMAGE_CAUSE>` – per przyczyna (np. `FALL`, `LAVA`, `DROWNING`, …)
- `ui` – tytuły i wiadomości:
  - `skullSymbol` – emotka używana w tytułach (domyślnie `💀`)
  - `titles.kill` / `titles.selfDeath` – wzory Title/SubTitle z placeholderami
  - `titles.timings` – `fadeIn`, `stay`, `fadeOut` (ticki)
  - `messages.kill` / `messages.selfDeath` – wiadomości czatu (kolory &-kowe)
- `ranking` – top-listy:
  - `pageSize`
  - `playerFormat`, `playerFormatSelf`, `clanFormat`, `clanFormatSelf`
- `placeholders`:
  - `noClanText` – tekst gdy gracz nie ma klanu
- `storage`:
  - `type: sqlite`
  - `file: plugins/Fepbox-Klany/data.db`

## Komendy

### Gracz (`/klan`)

- `/klan zaloz <TAG> <NAZWA>`  
  Tworzy nowy klan (tag/nazwa walidowane, duplikaty blokowane).

- `/klan info [klan|tag]`  
  Informacje o klanie:
  - bez argumentu – klan gracza
  - z argumentem – klan po tagu/nazwie

- `/klan punkty [gracz]`  
  Podgląd punktów PvP:
  - bez argumentu – własne punkty
  - z argumentem – inny gracz (online)

> W specyfikacji są też: `/klan zapros`, `/klan dolacz`, `/klan opusc`, `/klan wyrzuc`, `/klan top`, `/klan reload` – szkielet pluginu uwzględnia logikę klanów i rankingów, ale nie wszystkie powyższe komendy są jeszcze w pełni rozpisane w aktualnej wersji executora.

### Admin (`/fepboxklany admin`)

- `/fepboxklany admin setpoints <gracz> <wartosc>`  
  Ustawia dokładną wartość punktów PvP dla gracza.

- `/fepboxklany admin addpoints <gracz> <wartosc>`  
  Dodaje (lub odejmuje, jeśli wartość ujemna) punkty PvP graczowi.

(Prosty help pokazuje też `/fepboxklany reload` jako koncepcję; logika przeładowania configu może być rozwinięta w kolejnych wersjach.)

## Permisje

Zdefiniowane w `plugin.yml`:

- `fepboxklany.use` – podstawowy dostęp do komendy `/klan`  
  (domyślnie: `true`)

- `fepboxklany.clan.*` – wszystkie komendy klanowe  
  (domyślnie: `op`)  
  - `fepboxklany.clan.create`
  - `fepboxklany.clan.invite`
  - `fepboxklany.clan.join`
  - `fepboxklany.clan.leave`
  - `fepboxklany.clan.kick`
  - `fepboxklany.clan.info`
  - `fepboxklany.clan.top`
  - `fepboxklany.clan.points`

- `fepboxklany.admin.*` – wszystkie komendy administracyjne  
  (domyślnie: `op`)  
  - `fepboxklany.admin.base`
  - `fepboxklany.admin.forcejoin`
  - `fepboxklany.admin.forcekick`
  - `fepboxklany.admin.rename`
  - `fepboxklany.admin.retag`
  - `fepboxklany.admin.setpoints`
  - `fepboxklany.admin.addpoints`
  - `fepboxklany.admin.recalc`
  - `fepboxklany.reload`

- `fepboxklany.reload` – uprawnienie do przeładowania konfiguracji

## Placeholdery (PlaceholderAPI)

Identifier: `fepbox`  
Przykłady użycia w PlaceholderAPI:

- `%fepbox_klan_tag%` – tag klanu gracza (lub `noClanText`)
- `%fepbox_klan_name%` – nazwa klanu gracza
- `%fepbox_klan_color%` – kolor klanu (np. kod `§a`, `&a` – zależnie od użycia)
- `%fepbox_klan_display%` – sformatowany klan, np. `[TAG] Nazwa` z kolorem
- `%fepbox_points%` – aktualne punkty PvP gracza
- `%fepbox_points_formatted%` – punkty z formatowaniem tysiącowym
- `%fepbox_clan_points%` – średnia punktów klanu gracza (zaokrąglona)

Dla graczy bez klanu placeholdery klanowe zwracają wartość z `placeholders.noClanText`.

## System punktów i tytułów

- Każdy nowy gracz startuje z `points.startPoints` (domyślnie 1000).
- Zabójstwo innego gracza:
  - Zabójca dostaje `+delta` punktów, ofiara traci `-delta`.
  - `delta` zależy od różnicy rankingów (`baseReward` + `factor * (victim - killer)`), z clampem do `[minChange, maxChange]`.
  - Pokazywany jest Title `💀 ZABÓJSTWO 💀` (domyślnie) z dokładną liczbą punktów zdobytych/straconych.
- Śmierć bez zabójcy:
  - Gracz traci `selfDeathLoss` (per przyczyna lub domyślne).
  - Pokazywany jest Title `💀 ŚMIERĆ 💀` z dokładną liczbą utraconych punktów.
  - Czaty śmierci mają osobne wiadomości dla różnych przyczyn (fallback na `default`).

## Dane i wydajność

- **Storage**: SQLite (pliki w `plugins/Fepbox-Klany/`).
- Tabele:
  - `players` – UUID, nazwa, punkty, data utworzenia
  - `clans` – UUID klanu, tag, nazwa, kolor, owner UUID, data utworzenia
  - `clan_members` – przypisanie graczy do klanów + rola
- **Cache**:
  - Punkty graczy i dane klanów trzymane w pamięci dla szybkich odczytów.
  - Zmiany zapisywane asynchronicznie, aby nie blokować głównego wątku serwera.

## Status funkcjonalności

Ta wersja pluginu implementuje główne wymagania z punktów:

- System punktów PvP + tytuły/wiadomości po śmierci/zabójstwie
- Podstawowy system klanów (tworzenie, średnia punktów, podstawowe info)
- Integracja z PlaceholderAPI
- SQLite + cache + async DB

Niektóre bardziej zaawansowane elementy (np. pełny system zaproszeń `/klan zapros` + `/klan dolacz` tylko po zaproszeniu, paginowane top‑listy `/klan top gracze|klany`, kompletna administracja klanów) mogą wymagać dalszego rozwinięcia zgodnie z Twoimi potrzebami serwera.

## Licencja

Brak jawnie określonej licencji – traktuj jako kod prywatny do użytku na Twoim serwerze, chyba że postanowisz opublikować go z konkretną licencją (np. MIT/GPL) we własnym zakresie.

