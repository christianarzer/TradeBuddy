# Markttermine: Kostenfreie Quellenstrategie

Stand: 1. März 2026

## Ziel
Stabile, kostenlose und rechtlich saubere Markttermine mit robuster Fallback-Strategie und niedriger API-Last.

## Umgesetzte Reihenfolge im Code
1. TradingEconomics (guest) als breite Basis
2. BLS Release Schedule Seiten (offizielle BLS-Kalenderseiten)
3. BEA Release Dates (offizielles JSON)
4. ECB Statistics Calendar (offizielle Seite)
5. Eurostat Release Calendar (offizielle iCal-Quelle)
6. FRED API (offizielle US-Releases)
7. FMP (optional per API-Key, nur wenn Endpoint im Plan verfügbar)

## FMP: Was im Free-Plan realistisch ist
- Laut FMP Free-Plan sind Premium-Endpoints ausgeschlossen; es gibt nur eingeschränkten Zugriff.
- Der Economic Calendar ist bei vielen Free-Keys gesperrt (`HTTP 402 Restricted Endpoint`).
- Daher versucht die App FMP-Endpunkte in dieser Reihenfolge:
1. `https://financialmodelingprep.com/api/v3/economic_calendar` (Legacy)
2. `https://financialmodelingprep.com/stable/economic-calendar` (Stable)
- Falls ein Endpoint gesperrt ist, wird nur dieser Endpoint für den Key deaktiviert; die App nutzt automatisch die übrigen Quellen.

## BLS: Warum Bot-Schutz und wie korrekt fetchen
- BLS blockt automatisierte Abrufe, die nicht zur Usage Policy passen.
- Deshalb nutzt die App keine aggressiven Feed-/ICS-Abfragen, sondern offizielle BLS-Release-Schedule-Seiten.
- Wenn ein Endpoint temporär `403/404` liefert, wird er für die Session ausgesetzt, statt ihn ständig erneut zu pollen.

## Mapping-Regeln
- `actual`, `previous`, `forecast`, `consensus` nur anzeigen, wenn Quelle die Felder wirklich liefert.
- Fehlende Werte werden als `n/a` behandelt.
- Keine erfundenen Forecast-/Consensus-Werte.

## Cache-Strategie
- Refresh nur bei Bedarf (`refreshIfNeeded`), nicht bei jedem Screen-Wechsel.
- In-Memory + persistenter Cache aktiv.
- Standard-TTL: 2 Stunden.
- Auch leere Ergebnisse werden zwischengespeichert (Reduktion unnötiger Requests).

## Offizielle Quellen
- TradingEconomics Calendar: https://docs.tradingeconomics.com/economic_calendar/snapshot/
- BLS Developers: https://www.bls.gov/developers/
- BLS Usage Policy: https://www.bls.gov/bls/usage-policy.htm
- BEA Release Dates JSON: https://apps.bea.gov/API/signup/release_dates.json
- ECB Statistical Calendar: https://www.ecb.europa.eu/events/calendar/statscal/html/index.en.html
- Eurostat Release Calendar: https://ec.europa.eu/eurostat/web/main/news/release-calendar
- FRED API: https://fred.stlouisfed.org/docs/api/fred/
- FMP Free Plan: https://site.financialmodelingprep.com/developer/docs/pricing
- FMP FAQ (Free vs Premium): https://site.financialmodelingprep.com/faqs?code=APIAccess
