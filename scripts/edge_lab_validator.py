#!/usr/bin/env python3
"""
TradeBuddy Edge Lab validator (Phase 1).

Input (stdin JSON):
{
  "runId": "...",
  "trades": [{"exitTimeEpochMillis": 0, "pnlPercent": 0.0}, ...],
  "metrics": {"totalReturnPercent": 0.0, "maxDrawdownPercent": 0.0, "sharpeRatio": 0.0}
}

Output (stdout JSON):
{
  "status": "passed|warning|failed|unavailable",
  "edgeScore": 0.0,
  "sampleCount": 0,
  "cpcvSplits": 0,
  "cpcvPaths": 0,
  "oosSharpe": null,
  "oosReturnPercent": null,
  "oosPositivePathPercent": null,
  "spaPValue": null,
  "notes": []
}
"""

from __future__ import annotations

import itertools
import json
import math
import random
import statistics
import sys
from typing import Dict, Iterable, List, Optional, Sequence, Tuple


def _mean(values: Sequence[float]) -> float:
    return float(sum(values)) / float(len(values)) if values else 0.0


def _stdev(values: Sequence[float]) -> float:
    if len(values) < 2:
        return 0.0
    return float(statistics.pstdev(values))


def _sharpe(values: Sequence[float]) -> Optional[float]:
    if len(values) < 2:
        return None
    mu = _mean(values)
    sigma = _stdev(values)
    if sigma <= 1e-12:
        if abs(mu) <= 1e-12:
            return 0.0
        return 10.0 if mu > 0 else -10.0
    return (mu / sigma) * math.sqrt(float(len(values)))


def _compound_return_percent(values: Sequence[float]) -> Optional[float]:
    if not values:
        return None
    total = 1.0
    for value in values:
        total *= 1.0 + (float(value) / 100.0)
    return (total - 1.0) * 100.0


def _split_contiguous_blocks(values: Sequence[float], splits: int) -> List[List[float]]:
    if not values:
        return []
    safe_splits = max(2, min(splits, len(values)))
    block_size = max(1, len(values) // safe_splits)
    blocks: List[List[float]] = []
    start = 0
    while start < len(values):
        end = min(len(values), start + block_size)
        blocks.append(list(values[start:end]))
        start = end
    if len(blocks) > safe_splits:
        tail = []
        while len(blocks) > safe_splits:
            tail = blocks.pop() + tail
        if tail:
            blocks[-1].extend(tail)
    return [block for block in blocks if block]


def _cpcv_like_metrics(returns_pct: Sequence[float]) -> Dict[str, Optional[float]]:
    n = len(returns_pct)
    splits = 6 if n >= 60 else 5 if n >= 40 else 4 if n >= 24 else 3
    blocks = _split_contiguous_blocks(returns_pct, splits=splits)
    if len(blocks) < 2:
        return {
            "splits": len(blocks),
            "paths": 0,
            "oos_sharpe": None,
            "oos_return_pct": None,
            "oos_positive_paths_pct": None,
        }

    test_block_count = 2 if len(blocks) >= 4 else 1
    combinations = list(itertools.combinations(range(len(blocks)), test_block_count))
    # Keep runtime bounded for very long runs.
    if len(combinations) > 120:
        step = max(1, len(combinations) // 120)
        combinations = combinations[::step]

    path_sharpes: List[float] = []
    path_returns: List[float] = []
    positive_count = 0

    for combo in combinations:
        test: List[float] = []
        for idx in combo:
            test.extend(blocks[idx])
        if len(test) < 2:
            continue
        test_sharpe = _sharpe(test)
        test_return = _compound_return_percent(test)
        if test_sharpe is not None:
            path_sharpes.append(test_sharpe)
        if test_return is not None:
            path_returns.append(test_return)
            if test_return > 0.0:
                positive_count += 1

    path_count = max(0, len(path_returns))
    positive_ratio = (100.0 * positive_count / path_count) if path_count > 0 else None
    return {
        "splits": len(blocks),
        "paths": path_count,
        "oos_sharpe": _mean(path_sharpes) if path_sharpes else None,
        "oos_return_pct": _mean(path_returns) if path_returns else None,
        "oos_positive_paths_pct": positive_ratio,
    }


def _bootstrap_spa_proxy_pvalue(returns_pct: Sequence[float], iterations: int = 1500) -> Optional[float]:
    n = len(returns_pct)
    if n < 8:
        return None

    observed_mean = _mean(returns_pct)
    centered = [value - observed_mean for value in returns_pct]
    rng = random.Random(42)
    block_len = max(2, min(8, n // 6))
    exceed = 0

    for _ in range(iterations):
        sample: List[float] = []
        while len(sample) < n:
            start = rng.randrange(0, n)
            for step in range(block_len):
                sample.append(centered[(start + step) % n])
                if len(sample) >= n:
                    break
        boot_mean = _mean(sample)
        if boot_mean >= observed_mean:
            exceed += 1

    return float(exceed) / float(iterations)


def _clamp(value: float, lo: float, hi: float) -> float:
    return max(lo, min(hi, value))


def _score_edge(
    oos_sharpe: Optional[float],
    oos_return_pct: Optional[float],
    positive_paths_pct: Optional[float],
    spa_p_value: Optional[float],
    max_drawdown_pct: float,
) -> float:
    score = 50.0
    if oos_sharpe is not None:
        score += _clamp(oos_sharpe * 10.0, -25.0, 25.0)
    if oos_return_pct is not None:
        score += _clamp(oos_return_pct * 0.25, -20.0, 20.0)
    if positive_paths_pct is not None:
        score += _clamp((positive_paths_pct - 50.0) * 0.30, -15.0, 15.0)
    if spa_p_value is not None:
        score += _clamp((0.10 - spa_p_value) * 150.0, -20.0, 20.0)
    score += _clamp((20.0 - max_drawdown_pct) * 0.50, -20.0, 10.0)
    return _clamp(score, 0.0, 100.0)


def _decide_status(
    edge_score: float,
    sample_count: int,
    oos_sharpe: Optional[float],
    positive_paths_pct: Optional[float],
    spa_p_value: Optional[float],
) -> Tuple[str, List[str]]:
    notes: List[str] = []
    if sample_count < 20:
        notes.append("Sample-Groesse < 20 Trades: geringe statistische Aussagekraft.")
    if oos_sharpe is None or spa_p_value is None:
        notes.append("CPCV/SPA konnten nicht vollstaendig berechnet werden.")
        return "unavailable", notes

    if oos_sharpe < 0.0:
        notes.append("OOS Sharpe < 0: Signal ist instabil.")
    if positive_paths_pct is not None and positive_paths_pct < 45.0:
        notes.append("Zu wenige positive CPCV-Pfade.")
    if spa_p_value > 0.20:
        notes.append("SPA p-Wert > 0.20: kein robustes Edge-Signal.")

    if notes:
        return "failed", notes

    if sample_count < 20:
        return "warning", notes

    if edge_score >= 70.0 and spa_p_value <= 0.10 and oos_sharpe > 0.20 and (positive_paths_pct or 0.0) >= 55.0:
        notes.append("Edge erfuellt die Phase-1 Schwellenwerte.")
        return "passed", notes

    notes.append("Edge ist teilweise robust, aber Schwellenwerte nicht voll erreicht.")
    return "warning", notes


def _build_result(payload: Dict[str, object]) -> Dict[str, object]:
    trades = list(payload.get("trades") or [])
    metrics = dict(payload.get("metrics") or {})
    parsed_trades: List[Tuple[int, float]] = []
    for raw in trades:
        if not isinstance(raw, dict):
            continue
        exit_ms = raw.get("exitTimeEpochMillis")
        pnl_pct = raw.get("pnlPercent")
        if not isinstance(exit_ms, int):
            continue
        if not isinstance(pnl_pct, (int, float)):
            continue
        parsed_trades.append((exit_ms, float(pnl_pct)))
    parsed_trades.sort(key=lambda item: item[0])
    returns_pct = [item[1] for item in parsed_trades]
    sample_count = len(returns_pct)

    if sample_count == 0:
        return {
            "status": "unavailable",
            "edgeScore": 0.0,
            "sampleCount": 0,
            "cpcvSplits": 0,
            "cpcvPaths": 0,
            "oosSharpe": None,
            "oosReturnPercent": None,
            "oosPositivePathPercent": None,
            "spaPValue": None,
            "notes": ["Keine Trades gefunden."],
        }

    cpcv = _cpcv_like_metrics(returns_pct)
    spa_p = _bootstrap_spa_proxy_pvalue(returns_pct)
    max_drawdown = float(metrics.get("maxDrawdownPercent") or 0.0)
    edge_score = _score_edge(
        oos_sharpe=cpcv["oos_sharpe"],
        oos_return_pct=cpcv["oos_return_pct"],
        positive_paths_pct=cpcv["oos_positive_paths_pct"],
        spa_p_value=spa_p,
        max_drawdown_pct=max_drawdown,
    )
    status, notes = _decide_status(
        edge_score=edge_score,
        sample_count=sample_count,
        oos_sharpe=cpcv["oos_sharpe"],
        positive_paths_pct=cpcv["oos_positive_paths_pct"],
        spa_p_value=spa_p,
    )

    return {
        "status": status,
        "edgeScore": edge_score,
        "sampleCount": sample_count,
        "cpcvSplits": int(cpcv["splits"] or 0),
        "cpcvPaths": int(cpcv["paths"] or 0),
        "oosSharpe": cpcv["oos_sharpe"],
        "oosReturnPercent": cpcv["oos_return_pct"],
        "oosPositivePathPercent": cpcv["oos_positive_paths_pct"],
        "spaPValue": spa_p,
        "notes": notes,
    }


def main() -> int:
    try:
        raw = sys.stdin.read()
        payload = json.loads(raw or "{}")
        if not isinstance(payload, dict):
            raise ValueError("Payload muss ein JSON-Objekt sein.")
        result = _build_result(payload)
        sys.stdout.write(json.dumps(result, ensure_ascii=True))
        return 0
    except Exception as error:  # pragma: no cover - defensive path
        failure = {
            "status": "unavailable",
            "edgeScore": 0.0,
            "sampleCount": 0,
            "cpcvSplits": 0,
            "cpcvPaths": 0,
            "oosSharpe": None,
            "oosReturnPercent": None,
            "oosPositivePathPercent": None,
            "spaPValue": None,
            "notes": [f"Edge Lab validator error: {error}"],
        }
        sys.stdout.write(json.dumps(failure, ensure_ascii=True))
        return 0


if __name__ == "__main__":
    raise SystemExit(main())
