# Backtesting-Modul

## Überblick
Das Backtesting-Modul ist als erweiterbare, plattformübergreifende KMP-Implementierung aufgebaut und läuft ohne bezahlte APIs.
Die UI blockiert nicht: Datenladen und Simulation laufen asynchron, mit Fortschritt und Abbruch.

## Architektur
- `data/`
  - `BacktestingRepository`: Historie, Candle-Cache, CSV-Import, Provider-Orchestrierung.
  - `CandleDataProvider`: Abstraktion für Datenquellen.
  - `BinanceKlineDataProvider`: Binance Spot Public REST (`/api/v3/klines`), inkl. Retry/Backoff.
  - `BybitKlineDataProvider`: Bybit Spot Public REST (`/v5/market/kline`), inkl. Retry/Backoff.
  - `OkxKlineDataProvider`: OKX Spot Public REST (`/api/v5/market/history-candles`), inkl. Retry/Backoff.
- `engine/`
  - `BacktestingEngine`: deterministische Bar-by-Bar-Simulation.
  - Unterstützt Market/Limit, Maker/Taker Fees, Slippage, Fill-Ratio, Volumen-Partizipation, Leverage/Margin, Funding, Risk-Exits.
  - `validation/EdgeLabValidator`: Phase-1 Edge-Check (CPCV/SPA) mit Python-Sidecar auf JVM.
- `metrics/`
  - `BacktestingMetrics`: Kennzahlen, Drawdown-Serie, Monatsrenditen, Walk-Forward, Monte-Carlo.
- `presentation/`
  - `BacktestingViewModel`, `BacktestingUiState`: State, Validierung, Run/Cancel, Export, Historie/Compare.
- `ui/`
  - `BacktestingScreen`: Konfiguration, Run-Steuerung, Ergebnisse, Analyse, Trades, Historie.

## Datenquellen und Cache
- Live-Quellen (kostenfrei, ohne API-Key):
  - Binance Spot REST
  - Bybit Spot REST
  - OKX Spot REST
- Symbolhinweis:
  - Binance/Bybit erwarten typischerweise `BTCUSDT`.
  - OKX nutzt `BTC-USDT`; bei Eingabe `BTCUSDT` wird automatisch normalisiert.
- CSV-Import: `importCandlesCsv(...)` für lokale/extern vorbereitete Kerzendaten.
- Cache-Key: `exchange|symbol|timeframe`.
- Cache enthält normalisierte OHLCV-Candles und wird bei Remote-Antworten zusammengeführt.
- Datenqualitäts-Hinweise: Lücken/Unvollständigkeit/Duplikate.

## Ausführungsmodell (Annahmen)
- Signalberechnung auf aktueller Kerze, Fill standardmäßig auf nächster Kerze.
- Market-Fill: nächster Open-Preis plus/minus Slippage.
- Limit-Fill: nächster Open ± Offset; Fill nur bei Touch (High/Low).
- Gebühren: separates Maker/Taker-Modell in bps.
- Funding: bps/Tag auf offene Positionen.
- Risk-Logik:
  - Stop Loss, Take Profit, Trailing Stop
  - Daily-Loss-Limit
  - Kill-Switch auf Max-Drawdown

## Aktuell unterstützte Funktionen
- Strategien: SMA Crossover, RSI Mean Reversion, Breakout, SMA+RSI Confluence, ATR Volatility Breakout, Sun/Mond/Astro Flow, Custom Rule-Engine.
- Sun/Mond/Astro Flow:
  - Stadt-/Zeitzonenwahl (gleiche Städtebasis wie im Astro-Kalender).
  - Aktivierbare Signalquellen: Sonnenereignisse, Mondereignisse, Mondphasen, Aspekte.
  - Aspekt-Scope: nur Mond-Aspekte oder alle Planeten-Aspekte.
  - Signalfenster in Kerzen (`astroSignalWindowBars`) zur robusteren Zuordnung astronomischer Zeitpunkte auf Candle-Indizes.
  - Konfliktauflösung pro Kerze nach Distanz zum Event und Signalpriorität.
- Custom Rule-Engine (JSON):
  - Entry/Exit für Long/Short
  - `all`/`any`/`groups` (verschachtelt)
  - Operatoren: `GT`, `GTE`, `LT`, `LTE`, `EQ`, `CROSS_UP`, `CROSS_DOWN`
  - Operanden: `open/high/low/close/volume/sma/ema/rsi/atr/value`
- Positionsgrößen: Fixbetrag, % Equity, ATR-Risk, Volatility-Target.
- Ordermodell: Market, Limit.
- Direction: Long-only oder Long/Short, optional Hedging-Flag.
- Ergebnisse:
  - Kennzahlen (Win Rate, Return, CAGR, DD, Sharpe, Sortino, PF, Expectancy, Volatilität, Exposure, Trades).
  - Kurschart mit Entry/Exit-Markern und Trade-Segmenten (Entry -> Exit).
  - Trade-Fokus: Klick auf Trade in Tabelle fokussiert Chart auf den betreffenden Zeitraum.
  - Equity/Drawdown/Trade-PnL Charts.
  - Trades-Tabelle mit Filter/Sort.
  - Analysebereich (Monatsrenditen, Walk-Forward, Monte-Carlo).
  - Edge Lab (Phase 1): Edge Score + Status (`Passed`, `Warning`, `Failed`, `Unavailable`) inkl. CPCV/SPA-Metriken.
  - Edge Gate mit Schwellwerten für robuste Strategie-Selektion (Edge Score, SPA p-Wert, Positive Pfade, Trades, OOS Sharpe).
- Optimierung:
  - Grid Search und Random Search (kostenfrei lokal).
  - Konfigurierbare Parameter-Ranges (SMA fast/slow, RSI-Periode, Breakout-Lookback).
  - Zielmetrik wählbar: Balanced, Total Return, Sharpe, Profit Factor, Calmar.
  - Top-Runs inkl. Score + Parameterlabel.
- Export:
  - Trades als CSV
  - Summary als JSON
  - Optimierungsergebnis als CSV
- Historie lokal gespeichert, inkl. 2-Run-Vergleich.

## Erweiterung
### Neuen Datenprovider hinzufügen
1. `CandleDataProvider` implementieren.
2. In `AppContainer` in `providers` registrieren.
3. Optional eigene Cache-Validierung ergänzen.

### Neue Strategie hinzufügen
1. `BacktestStrategyTemplate` erweitern.
2. Signal-Logik in `BacktestingEngine.generateSignal(...)` ergänzen.
3. Parameter in `BacktestStrategyConfig`, `BacktestingUiState`, `BacktestingViewModel`, `BacktestingScreen` anbinden.

## Tests
- `BacktestingEngineTest`: deterministischer Run auf fixem Candle-Set.
- `BacktestingMetricsTest`: Win-Rate/Drawdown korrekt.

## Bekannte Grenzen
- Quellen liefern OHLCV unterschiedlich granular (Exchange-seitige Limits/Range-Fenster bleiben relevant).
- Kein Tick-/Orderbook-Level Fill-Modell (OHLCV-basiert).
- Keine echte L2/L3-Orderbook-Simulation (Queue-Position/Latency nur approximierbar auf OHLCV).
- Python-Sidecar benötigt eine verfügbare Python-Runtime (`python3`, `python` oder `py -3`; optional `TRADEBUDDY_PYTHON`).
