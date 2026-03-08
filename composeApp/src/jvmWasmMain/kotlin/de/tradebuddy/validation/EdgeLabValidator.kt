package de.tradebuddy.validation

import de.tradebuddy.domain.model.BacktestEdgeValidation
import de.tradebuddy.domain.model.BacktestEdgeValidationStatus
import de.tradebuddy.domain.model.BacktestResult
import java.time.Instant

interface EdgeLabValidator {
    suspend fun validate(result: BacktestResult): BacktestEdgeValidation
}

class NoOpEdgeLabValidator(
    private val note: String = "Edge Lab Validator ist nicht konfiguriert."
) : EdgeLabValidator {
    override suspend fun validate(result: BacktestResult): BacktestEdgeValidation =
        BacktestEdgeValidation(
            status = BacktestEdgeValidationStatus.Unavailable,
            edgeScore = 0.0,
            sampleCount = result.trades.size,
            cpcvSplits = 0,
            cpcvPaths = 0,
            notes = listOf(note),
            generatedAt = Instant.now()
        )
}
