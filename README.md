# TradeBuddy

TradeBuddy ist eine Kotlin-Multiplatform-Anwendung fuer Trading-orientierte Zeitplanung mit Sonne, Mond und Astro-Kalender.
Der Fokus liegt auf schnellen Tagesuebersichten, filterbaren Ereignissen und lokaler Auswertung.

![Screenshots der App](images/screenshots.png)

## Aktueller Projektstatus

- Voll funktionsfaehig: Desktop (JVM) und Android
- iOS: aktuell Platzhalteransicht (kein voller Feature-Umfang wie Desktop/Android)

## Wichtigste Features (aktuell)

- Tagesansicht fuer Sonnenaufgang, Sonnenuntergang, Mondaufgang und Monduntergang
- Weltweite Markt-/Stadtauswahl mit Suchfunktion und Aktiv/Inaktiv-Filter
- Kompakt-Zeitstrahl mit:
  - lokaler Zeit und optional UTC
  - Azimut-Richtung (optional)
  - Markierung bereits vergangener Ereignisse (am aktuellen Tag)
  - schneller Kopierfunktion der Zeitwerte
- Statistik-Modul mit:
  - manueller Trendbewertung (Up/Down) je Ereignis
  - Offset in Minuten
  - Filter (Suche, Richtung, Event-Typ, Zeitraum, Sortierung)
  - Kennzahlen und Trendverteilung
- Monats-Trendansicht (Azimut-Verlauf) fuer Sonne und Mond
- Astro-Kalender mit:
  - Aspektliste und 7-Tage-Uebersicht
  - Planeten-/Aspekt-Filter
  - konfigurierbaren Orb-Grenzen
  - Countdown und Status (bevorstehend/exakt/vorbei)
- Mondphasen-Tab fuer den gewaehlten Monat
- Umfangreiche Theme-Konfiguration:
  - Tag/Nacht-Modus
  - mehrere visuelle Styles
- Persistente lokale Speicherung:
  - `settings.properties` fuer App-Einstellungen
  - `stats.csv` fuer Statistik-Eintraege

## Technologie-Stack

- Kotlin Multiplatform
- Compose Multiplatform + Material 3
- Kotlin Coroutines + StateFlow
- Astronomy Engine (`com.github.cosinekitty:astronomy`) fuer astronomische Berechnungen
- Gradle (Kotlin DSL)

## Projektstruktur

- `composeApp/` Gemeinsame App-Logik und UI (inkl. Desktop/Android/iOS target code)
- `androidApp/` Android-App-Modul
- `iosApp/` iOS-Hostprojekt
- `.github/workflows/` CI-Workflows

## Voraussetzungen

- JDK 17 (empfohlen: Temurin)
- Android Studio fuer Android-Builds
- Xcode fuer iOS-Builds (macOS)

## Lokale Entwicklung

### Desktop starten

```bash
./gradlew :composeApp:run
```

### Android Debug-Build

```bash
./gradlew :androidApp:assembleDebug
```

### iOS in Xcode bauen

```bash
cd iosApp
xcodebuild -scheme iosApp -configuration Debug -destination 'platform=iOS Simulator,name=iPhone 16' CODE_SIGNING_ALLOWED=NO
```

## Release und spaeteres Hosting

- Quellcode-Hosting: GitHub Repository
- Artefakte: GitHub Releases (z. B. Desktop-Builds, Android APK/AAB)
- Die Desktop-Distributionen koennen ueber Compose/Gradle erzeugt werden.

## GitHub-Standarddateien

Dieses Repo enthaelt wichtige Kollaborationsdateien:

- `CONTRIBUTING.md` Richtlinien fuer Commits und Pull Requests
- `.github/ISSUE_TEMPLATE/` Vorlagen fuer Bug- und Feature-Issues
- `.github/pull_request_template.md` PR-Checkliste
- `.github/dependabot.yml` automatische Dependency-Updates
- `.github/workflows/` CI fuer Android und iOS

## Lizenz

Apache-2.0 (siehe `LICENSE`)
