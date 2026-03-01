# TradeBuddy Design System Mapping (TradeBuddy + Targets)

## Source of truth
- TradeBuddy Design System Referenzfile: `jbUTg7T99q4Xov1v7cPOee`
- Nodes used: `60755:5423`, `62698:47054`, `60755:5518`, `60755:5458`, `60755:5488`, `60755:5600`, `60755:5540`, `60755:5387`, `60755:5375`, `60755:5380`, `60755:5362`
- Targets/Dashboard layout file: `EdTVmdJSC48YTHBHJlqKJf`
- Nodes used: `678:7342` (dark), `678:3987` (light)
- Extraction method: Figma MCP (`get_variable_defs`, `get_design_context`)

## Core principles
- Exactly two app modes: `Light` and `Dark`.
- All semantic colors are centralized in `ui/theme/Color.kt`, `ui/theme/ExtendedColors.kt`, `ui/theme/AppPalette.kt`.
- No screen-level hardcoded hex colors.
- Card style uses single-layer blocks: no stacked/double elevation.
- Border/divider system uses one shared semantic line color per mode.

## Token mapping
### Spacing
- Base scale: `4, 8, 12, 16, 20, 24, 28, 40, 48, 80`
- Implemented in `AppSpacing`.

### Radius
- Core radius: `8`
- Large shell radius: `24`
- Implemented in `AppRadius` + `AppShapes`.

### Typography
- Family reference: `Inter`
- Core pairs used: `12/16`, `14/20`, `16/24`, `18/28`, `24/32`, `32/40`, `48/56`, `64/72`
- App maps this to Material text roles in `Typography.kt`.

### Color tokens (semantic)
- Base light: `#FFFFFF`, `#F9F9FA`, `#EDEEFC`, `#E6F1FD`, `#000000`
- Base dark: `#333333`, `#FFFFFF` alpha layers (`0A`, `1A`, `33`, `66`)
- Accent set: `#ADADFB`, `#7DBBFF`, `#71DD8C`, `#FFCC00`, `#FFB55B`, `#FF4747`, `#B899EB`, `#A0BCE8`
- Implemented in `AppPalette`.

## Shell and layout rules
- Shell structure mirrors the Figma dashboard:
  - left sidebar
  - top header
  - content area
  - right/top utility rhythm via compact icon groups
- Sidebar now uses Figma-like grouping:
  - `Favorites`
  - `Dashboards`
  - `Pages`
- Brand label updated to `TradeBuddy` in navigation shell.

## Feature mapping
### Market Events
- Uses live remote source (`FredMarketEventsDataSource`) through repository abstraction.
- Dummy/fallback generated events were removed (only echte API-Daten + Error/Empty states).
- Behavior is now:
  - remote success -> show events
  - remote empty -> show empty state
  - remote failure -> show error state

### Portfolio
- Allocation/legend/group color palette now uses centralized TradeBuddy palette tokens.
- Old hardcoded portfolio colors were removed.

### Tasks / Targets
- New persistent board with three columns:
  - `Backlog`
  - `In Progress`
  - `Completed`
- Layout and grouping follow the Targets-page card board pattern.
- Data layer is repository-based with local persistence (`tasks.json` / localStorage key).

## Assumptions
- Inter font is embedded in `composeResources/font` and wired centrally in `Typography.kt`.
- Community kit placeholders (icon stubs/content placeholders) were mapped to app semantics rather than copied literally.
