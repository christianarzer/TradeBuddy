# Beitragen zu TradeBuddy

Danke fuer deinen Beitrag.
Dieses Dokument beschreibt den empfohlenen Ablauf fuer Issues, Branches und Pull Requests.

## Voraussetzungen

- JDK 17
- Gradle Wrapper (`./gradlew`)
- Fuer Android-Checks: Android SDK
- Fuer iOS-Checks: Xcode auf macOS

## Branching

- `main` ist immer der stabile Stand.
- Arbeite fuer neue Aufgaben in einem eigenen Branch:
  - `feature/<kurze-beschreibung>`
  - `fix/<kurze-beschreibung>`
  - `chore/<kurze-beschreibung>`

## Commits

- Kleine, logisch zusammenhaengende Commits.
- Aussagekraeftige Commit-Messages.
- Empfohlenes Format:
  - `feat: ...`
  - `fix: ...`
  - `docs: ...`
  - `chore: ...`
  - `refactor: ...`
  - `test: ...`

## Pull Requests

- Beschreibe:
  - Was wurde geaendert?
  - Warum wurde es geaendert?
  - Wie kann man es testen?
- Verlinke zugehoerige Issues (`Closes #123`).
- Halte PRs moeglichst klein und fokussiert.

## Lokale Checks vor dem PR

- Android-Build:
  - `./gradlew :androidApp:assembleDebug`
- Desktop-Build/Run:
  - `./gradlew :composeApp:run`
- Web-Build:
  - `./gradlew :composeApp:wasmJsBrowserDistribution`

Wenn ein Check lokal nicht moeglich ist, bitte im PR begruenden.

## Releases und Tags

- Release-Tags folgen dem Schema `vMAJOR.MINOR.PATCH` (z. B. `v1.2.0`).
- Nach Push eines solchen Tags erstellt GitHub Actions automatisch einen Release
  mit Build-Artefakten.
