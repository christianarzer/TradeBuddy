package de.tradebuddy.validation

import de.tradebuddy.domain.model.BacktestEdgeValidation
import de.tradebuddy.domain.model.BacktestEdgeValidationStatus
import de.tradebuddy.domain.model.BacktestResult
import java.io.File
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PythonEdgeLabValidator : EdgeLabValidator {
    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun validate(result: BacktestResult): BacktestEdgeValidation = withContext(Dispatchers.IO) {
        if (result.trades.isEmpty()) {
            return@withContext unavailable(
                result = result,
                notes = listOf("Edge Lab: Keine Trades im Run.")
            )
        }

        val scriptFile = resolveScriptFile()
            ?: return@withContext unavailable(
                result = result,
                notes = listOf("Edge Lab Script fehlt: scripts/edge_lab_validator.py")
            )

        val payload = EdgeLabPayload(
            runId = result.runId,
            trades = result.trades.map { trade ->
                EdgeLabTradePayload(
                    exitTimeEpochMillis = trade.exitTime.toEpochMilli(),
                    pnlPercent = trade.pnlPercent
                )
            },
            metrics = EdgeLabMetricsPayload(
                totalReturnPercent = result.metrics.totalReturnPercent,
                maxDrawdownPercent = result.metrics.maxDrawdownPercent,
                sharpeRatio = result.metrics.sharpeRatio
            )
        )
        val payloadJson = json.encodeToString(payload)
        val commands = pythonCommandCandidates()
        var lastError: String? = null

        for (command in commands) {
            try {
                val process = ProcessBuilder(command + scriptFile.absolutePath).start()
                process.outputStream.bufferedWriter().use { writer ->
                    writer.write(payloadJson)
                }
                val stdout = process.inputStream.bufferedReader().use { it.readText() }
                val stderr = process.errorStream.bufferedReader().use { it.readText() }
                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    lastError = "Command `${command.joinToString(" ")}` failed ($exitCode): ${stderr.compact()}"
                    continue
                }
                val response = json.decodeFromString<EdgeLabResponse>(stdout)
                return@withContext response.toDomain(fallbackSamples = result.trades.size)
            } catch (error: Throwable) {
                lastError = "Command `${command.joinToString(" ")}` error: ${error.message.orEmpty().compact()}"
            }
        }

        val notes = buildList {
            add("Edge Lab konnte nicht ausgeführt werden (Python nicht verfügbar oder Script-Fehler).")
            lastError?.takeIf { it.isNotBlank() }?.let(::add)
        }
        unavailable(result = result, notes = notes)
    }

    private fun resolveScriptFile(): File? {
        val userDir = System.getProperty("user.dir").orEmpty()
        val cwd = File(userDir)
        val candidates = buildList {
            add(File("scripts/edge_lab_validator.py"))
            add(File(cwd, "scripts/edge_lab_validator.py"))
            cwd.parentFile?.let { add(File(it, "scripts/edge_lab_validator.py")) }
        }
        return candidates.firstOrNull { it.exists() && it.isFile }
    }

    private fun pythonCommandCandidates(): List<List<String>> {
        val envPython = System.getenv("TRADEBUDDY_PYTHON")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { listOf(it) }
        return buildList {
            envPython?.let(::add)
            add(listOf("python3"))
            add(listOf("python"))
            add(listOf("py", "-3"))
        }.distinct()
    }

    private fun unavailable(
        result: BacktestResult,
        notes: List<String>
    ): BacktestEdgeValidation = BacktestEdgeValidation(
        status = BacktestEdgeValidationStatus.Unavailable,
        edgeScore = 0.0,
        sampleCount = result.trades.size,
        cpcvSplits = 0,
        cpcvPaths = 0,
        notes = notes,
        generatedAt = Instant.now()
    )
}

@Serializable
private data class EdgeLabPayload(
    val runId: String,
    val trades: List<EdgeLabTradePayload>,
    val metrics: EdgeLabMetricsPayload
)

@Serializable
private data class EdgeLabTradePayload(
    val exitTimeEpochMillis: Long,
    val pnlPercent: Double
)

@Serializable
private data class EdgeLabMetricsPayload(
    val totalReturnPercent: Double,
    val maxDrawdownPercent: Double,
    val sharpeRatio: Double
)

@Serializable
private data class EdgeLabResponse(
    val status: String,
    val edgeScore: Double,
    val sampleCount: Int,
    val cpcvSplits: Int,
    val cpcvPaths: Int,
    val oosSharpe: Double? = null,
    val oosReturnPercent: Double? = null,
    val oosPositivePathPercent: Double? = null,
    val spaPValue: Double? = null,
    val notes: List<String> = emptyList()
) {
    fun toDomain(fallbackSamples: Int): BacktestEdgeValidation =
        BacktestEdgeValidation(
            status = when (status.trim().lowercase()) {
                "passed" -> BacktestEdgeValidationStatus.Passed
                "warning" -> BacktestEdgeValidationStatus.Warning
                "failed" -> BacktestEdgeValidationStatus.Failed
                else -> BacktestEdgeValidationStatus.Unavailable
            },
            edgeScore = edgeScore,
            sampleCount = sampleCount.takeIf { it > 0 } ?: fallbackSamples,
            cpcvSplits = cpcvSplits.coerceAtLeast(0),
            cpcvPaths = cpcvPaths.coerceAtLeast(0),
            oosSharpe = oosSharpe,
            oosReturnPercent = oosReturnPercent,
            oosPositivePathPercent = oosPositivePathPercent,
            spaPValue = spaPValue,
            notes = notes,
            generatedAt = Instant.now()
        )
}

private fun String.compact(maxLength: Int = 240): String {
    if (isBlank()) return ""
    val compact = replace(Regex("\\s+"), " ").trim()
    return if (compact.length <= maxLength) compact else compact.take(maxLength) + "..."
}
