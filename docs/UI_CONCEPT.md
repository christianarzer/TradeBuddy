# UI Concept (TradeBuddy)

## Ziel
Ein einheitliches, wartbares UI-Design-System mit Light/Dark als Single Source of Truth fuer alle Screens.

## Figma Source of Truth
- Snow Dashboard UI Kit (Main): `mLbS7BWhBCSfsbd3Xllhqx`
  - `98469:145264`, `98469:145265`, `98469:145267`, `98469:145268`
  - Color system node: `98469:150106`
  - Icon pack node: `91676:15494`
- Targets/Tasks Referenzen: `EdTVmdJSC48YTHBHJlqKJf`
  - `678:3987`, `678:7342`
- Design System Referenzquelle: `jbUTg7T99q4Xov1v7cPOee`
  - Typografie/Token Referenz u. a. `60755:5488`

## Design Tokens
- Farben:
  - Light: `#FFFFFF`, `#F9F9FA`, `#EDEEFC`, `#E6F1FD`, `#000000` + Alpha-Layer
  - Dark/Night: `#333333` + White-Alpha Layer (`0A`, `1A`, `33`, `66`)
  - Akzentfarben: `#ADADFB`, `#7DBBFF`, `#71DD8C`, `#FFCC00`, `#FFB55B`, `#FF4747`, `#B899EB`, `#A0BCE8`
- Typography:
  - TradeBuddy/Inter-orientierte Skala auf Material-Rollen gemappt
  - Fokus: 12/14/20/24 mit klaren Rollen fuer Label, Body, Title
- Spacing:
  - 4er-Basis: `4, 8, 12, 16, 20, 24, 28, 40, 48, 80`
- Radius:
  - Kernradius `8`, groessere Shell-Radien `24`, spezielle Rundwerte `80` nur tokenisiert
- Elevation/Borders:
  - Flache Card-Elevation (0dp) + definierte Divider/Borders ueber semantische Tokens

## MCP Recheck (Figma)
- Node `98469:150106` bestaetigt u. a.:
  - Spacing: `4, 8, 16, 20, 24, 28, 40, 48`
  - Radius: `8`
  - Core colors: `Primary/Background #FFFFFF`, `Primary/Brand #1C1C1C`, `Secondary/Indigo #95A4FC`, `Secondary/Blue #B1E3FF`
- Node `91676:15494` bestaetigt:
  - Radius token `80`
  - Typografie-Referenz `Inter Regular 64/78`

## Komponenten-Katalog
- Shell:
  - Sidebar + Header + Content + Footer (einheitlich fuer Desktop/Mobile)
- Basisbausteine:
  - `SnowIcons` (Phosphor-only)
  - `SnowLineChart`, `SnowBarChart`, `SnowSparkline`, `SnowChartLegend`, `SnowChartEmptyState` (Vico)
  - Karten ueber zentrale Card-Stile (`appBlockCardColors`, `appFlatCardElevation`)
- Feature-Bausteine:
  - Markttermine: KPI-Cards, Filter-Chips, Tagesgruppen, Event-Tabelle
  - Portfolio: KPI-Cards, Range-Chips, Tabellen/Allokation, Vico-Charts
  - Tasks: Board-Spalten, Drag-Reorder-Karten, Editor-Dialog mit DatePicker

## Do / Don’t
- Do:
  - Nur Token und zentrale Komponenten nutzen
  - Strings nur ueber Resources
  - Datumsformat in DE (`dd.MM.yyyy` / `HH:mm`)
  - Icons nur ueber `SnowIcons`
- Don’t:
  - Keine hardcoded Farben in Screens
  - Keine gemischten Icon-Packs
  - Keine zweite Chart-Implementierung neben Vico
  - Keine manuell gepflegten, nicht-tokenisierten Abstaende

## Mapping Figma -> Code
- Color System (98469:150106) -> `ui/theme/AppPalette.kt`, `ui/theme/Color.kt`, `ui/theme/ExtendedColors.kt`
- Typography (60755:5488) -> `ui/theme/Typography.kt`
- Card/Spacing/Radius (98469:14526x) -> `ui/theme/Tokens.kt`, `ui/theme/Shapes.kt`, `ui/theme/CardStyles.kt`
- Icon Pack (91676:15494) -> `ui/icons/SnowIcons.kt`
- Targets Layout (678:3987/7342) -> `ui/screens/TasksScreen.kt`

## Offene Punkte / TODO
- Einige Dashboard-Kleintexte in Legacy-Screens wurden bereits migriert, aber visuelle Feinabstaende koennen noch weiter auf Node-Detailniveau abgeglichen werden.
- Falls in Figma ein Wert uneindeutig ist, wird nur token-basiert approximiert und nicht screen-lokal hart kodiert.
