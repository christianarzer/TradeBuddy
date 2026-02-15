# TradeBuddy

[![Quality Checks](https://github.com/christianarzer/TradeBuddy/actions/workflows/quality-checks.yml/badge.svg)](https://github.com/christianarzer/TradeBuddy/actions/workflows/quality-checks.yml)
[![Build Android](https://github.com/christianarzer/TradeBuddy/actions/workflows/build-android.yml/badge.svg)](https://github.com/christianarzer/TradeBuddy/actions/workflows/build-android.yml)
[![Build Desktop](https://github.com/christianarzer/TradeBuddy/actions/workflows/build-desktop.yml/badge.svg)](https://github.com/christianarzer/TradeBuddy/actions/workflows/build-desktop.yml)
[![Build Web](https://github.com/christianarzer/TradeBuddy/actions/workflows/build-web.yml/badge.svg)](https://github.com/christianarzer/TradeBuddy/actions/workflows/build-web.yml)

Trading-orientierte Zeitplanung mit Sonne, Mond und Astro-Kalender auf Kotlin Multiplatform.

- Live Web: https://christianarzer.github.io/TradeBuddy/
- Stack: Kotlin Multiplatform, Compose Multiplatform, Material 3, Coroutines/StateFlow

![Screenshots der App](images/screenshots.png)

## Highlights

- Sun/Moon Tagesansicht fuer Sonnenaufgang, Sonnenuntergang, Mondaufgang, Monduntergang
- Kompakt-Zeitstrahl mit lokaler Zeit, optional UTC, Azimut und Status fuer bereits vergangene Events
- Astro-Kalender mit Aspektliste, Orb-Steuerung, Planet-/Aspektfiltern und Countdown
- Mondphasenansicht pro Monat
- Statistik mit Up/Down-Bewertung, Offset in Minuten, Filtern und Kennzahlen
- Settings mit moderner Theme-Auswahl, City-Filter, Log-Konsole und Time-Optimizer
- Time-Optimizer fuer Sun/Moon/Astro Offsets mit Monatsvorschau

## Platform Status

- Desktop (JVM): voll funktionsfaehig
- Android: voll funktionsfaehig
- Web (Wasm): online verfuegbar
- iOS: Host vorhanden, Feature-Umfang noch reduziert

## Repository Structure

- `composeApp/`: gemeinsame Domain-, Data- und UI-Logik (Desktop/Android/iOS/Web Targets)
- `androidApp/`: Android App Modul
- `iosApp/`: iOS Host Projekt
- `.github/workflows/`: Build-, Quality-, Deploy- und Release-Pipelines

## Requirements

- JDK 17+ (empfohlen: Temurin), `JAVA_HOME` entsprechend gesetzt
- Android Studio fuer Android Builds
- Xcode fuer iOS Builds (macOS)

## Quick Start

### Desktop Run

```bash
./gradlew :composeApp:desktopRun
```

### Android Debug Build

```bash
./gradlew :androidApp:assembleDebug
```

### Web Production Distribution

```bash
./gradlew :composeApp:wasmJsBrowserDistribution
```

Output:

- `composeApp/build/dist/wasmJs/productionExecutable`

### iOS Build (Xcode CLI)

```bash
cd iosApp
xcodebuild -scheme iosApp -configuration Debug -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO
```

## CI and Release

- Quality Checks: `.github/workflows/quality-checks.yml`
- Platform Builds: Android/Desktop/iOS/Web in separaten Workflows
- Web Deploy: `.github/workflows/deploy-web-pages.yml`
- Tag Release (`v*`): `.github/workflows/release-tag.yml`

Beispiel Tag Release:

```bash
git tag -a v1.0.0 -m "Release v1.0.0"
git push origin v1.0.0
```

## Collaboration Files

- `CONTRIBUTING.md`
- `.github/ISSUE_TEMPLATE/`
- `.github/pull_request_template.md`
- `.github/dependabot.yml`

## License

Apache-2.0 (`LICENSE`)
