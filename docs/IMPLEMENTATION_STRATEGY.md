# Implementation Strategy

## Audit Snapshot
- Charts:
  - Nutzung auf Vico konsolidiert:
    - `ui/charts/SnowCharts.kt`
    - `ui/screens/PortfolioScreen.kt`
    - `ui/components/MonthlyTrendCard.kt`
    - `ui/components/StatisticsCard.kt`
  - Ziel: keine parallelen Chart-Libraries in UI-Screens
- Icons:
  - Appweit auf `SnowIcons` (Phosphor) umgestellt
  - Material Icons in Compose-UI entfernt
- Hotspots fuer Inkonsistenz:
  - `ui/screens/TasksScreen.kt`
  - `ui/screens/MarketEventsScreen.kt`
  - `ui/screens/PortfolioScreen.kt`
  - `ui/AppRoot.kt`

## Migrationsplan
1. Theme/Tokens als zentrale Quelle absichern (Light/Dark).
2. Icon-Layer finalisieren (`SnowIcons` only).
3. Chart-Layer finalisieren (Vico Wrapper only).
4. Tasks Data-Layer fuer persistentes Reorder erweitern.
5. Tasks UI neu mit Drag & Drop + DatePicker + DE-Strings.
6. MarketEvents UI lokalisieren (DE), Datumsdarstellung korrigieren.
7. Portfolio Resttexte und Enum-Labels lokalisieren.
8. Navigation/Sidebar auf konsistente Menues und Texte bringen.
9. Cleanup: harte Texte/Farben/inkonsistente Werte entfernen.
10. Build- und Smoke-Checks ausfuehren.

## Risiken und Fallback
- Risiko: API/CORS in WASM fuer MarketEvents.
  - Fallback: Fehlerzustand klar anzeigen; Repository-Architektur bleibt API-ready.
- Risiko: Reorder-Migration alter Task-Daten ohne Reihenfolge.
  - Fallback: automatische Reihenfolgenormalisierung pro Spalte beim Laden/Speichern.
- Risiko: UI-Rewrite kann Imports/Resource-Keys brechen.
  - Fallback: schrittweise Compile-Checks pro Modul und Dateigruppe.

## QA / Testing Checklist
- Build:
  - `:composeApp:compileKotlinDesktop`
  - `:composeApp:compileKotlinWasmJs` (wenn Netzwerk/Caches verfuegbar)
- Funktional:
  - Tasks: Erstellen/Bearbeiten/Loeschen
  - Tasks: Long-Press Drag-Reorder und Persistenz nach Reload
  - Tasks: DatePicker + DE-Datumsanzeige
  - MarketEvents: Laden, Filter, Watchlist/Reminder, DE-Zeitformat
  - Portfolio: Karten, Tabellen, Bereichsfilter, Dialoge
- Konsistenz:
  - Keine direkten `Icons.*` in Screens
  - Keine neue Fremd-Chart-Library neben Vico
  - Keine neuen hardcoded Screen-Texte
