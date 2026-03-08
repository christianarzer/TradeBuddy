package de.tradebuddy.validation

import de.tradebuddy.domain.model.BacktestEdgeValidation
import de.tradebuddy.domain.model.BacktestEdgeValidationStatus
import de.tradebuddy.domain.model.BacktestResult
import java.time.Instant

class WasmEdgeLabValidator : EdgeLabValidator {
    override suspend fun validate(result: BacktestResult): BacktestEdgeValidation =
        BacktestEdgeValidation(
            status = BacktestEdgeValidationStatus.Unavailable,
            edgeScore = 0.0,
            sampleCount = result.trades.size,
            cpcvSplits = 0,
            cpcvPaths = 0,
            notes = listOf("Edge Lab ist im Web/Wasm-Build nicht verfügbar."),
            generatedAt = Instant.now()
        )
}
