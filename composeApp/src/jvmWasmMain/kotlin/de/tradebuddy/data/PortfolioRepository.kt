package de.tradebuddy.data

import de.tradebuddy.domain.model.AssetCategory
import de.tradebuddy.domain.model.PortfolioGroup
import de.tradebuddy.domain.model.PortfolioHistoryPoint
import de.tradebuddy.domain.model.PortfolioPosition
import de.tradebuddy.domain.model.PortfolioSnapshot
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

interface PortfolioRepository {
    suspend fun loadSnapshot(): PortfolioSnapshot
    suspend fun saveGroup(group: PortfolioGroup)
    suspend fun deleteGroup(groupId: String)
    suspend fun savePosition(position: PortfolioPosition)
    suspend fun deletePosition(positionId: String)
    suspend fun reset()
}

class DefaultPortfolioRepository(
    private val document: StorageDocument
) : PortfolioRepository {

    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
    }

    override suspend fun loadSnapshot(): PortfolioSnapshot {
        val payload = readPayload()
        return payload.toDomain()
    }

    override suspend fun saveGroup(group: PortfolioGroup) {
        mutate { payload ->
            val upserted = payload.groups
                .filterNot { it.id == group.id }
                .plus(group.toDto())
                .sortedBy { it.name.lowercase() }
            payload.copy(groups = upserted)
        }
    }

    override suspend fun deleteGroup(groupId: String) {
        mutate { payload ->
            payload.copy(
                groups = payload.groups.filterNot { it.id == groupId },
                positions = payload.positions.filterNot { it.groupId == groupId }
            )
        }
    }

    override suspend fun savePosition(position: PortfolioPosition) {
        mutate { payload ->
            val upserted = payload.positions
                .filterNot { it.id == position.id }
                .plus(position.toDto())
                .sortedBy { it.name.lowercase() }
            payload.copy(positions = upserted)
        }
    }

    override suspend fun deletePosition(positionId: String) {
        mutate { payload ->
            payload.copy(
                positions = payload.positions.filterNot { it.id == positionId }
            )
        }
    }

    override suspend fun reset() {
        document.clear()
    }

    private suspend fun mutate(transform: (PortfolioPayload) -> PortfolioPayload) {
        val current = readPayload()
        val changed = transform(current)
        val withHistory = changed.withRefreshedHistory()
        document.writeText(json.encodeToString(PortfolioPayload.serializer(), withHistory))
    }

    private suspend fun readPayload(): PortfolioPayload {
        val raw = document.readText() ?: return PortfolioPayload()
        return runCatching { json.decodeFromString(PortfolioPayload.serializer(), raw) }
            .getOrElse { PortfolioPayload() }
    }

    private fun PortfolioPayload.withRefreshedHistory(): PortfolioPayload {
        val totalValue = positions.sumOf { it.quantity * it.currentPrice }
        val today = LocalDate.now(ZoneOffset.UTC)
        val todayEpochMillis = today.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val existing = history.associateBy { it.dateEpochMillis }
        val merged = existing + (todayEpochMillis to PortfolioHistoryPointDto(todayEpochMillis, totalValue))
        return copy(
            updatedAtEpochMillis = Instant.now().toEpochMilli(),
            history = merged.values.sortedBy { it.dateEpochMillis }.takeLast(365)
        )
    }

    private fun PortfolioPayload.toDomain(): PortfolioSnapshot = PortfolioSnapshot(
        groups = groups.map { it.toDomain() }.sortedBy { it.name.lowercase() },
        positions = positions.mapNotNull { it.toDomainOrNull() }.sortedBy { it.name.lowercase() },
        history = history.mapNotNull { it.toDomainOrNull() }.sortedBy { it.date },
        updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis)
    )
}

@Serializable
private data class PortfolioPayload(
    val schemaVersion: Int = 1,
    val updatedAtEpochMillis: Long = Instant.now().toEpochMilli(),
    val groups: List<PortfolioGroupDto> = emptyList(),
    val positions: List<PortfolioPositionDto> = emptyList(),
    val history: List<PortfolioHistoryPointDto> = emptyList()
)

@Serializable
private data class PortfolioGroupDto(
    val id: String,
    val name: String,
    val colorHex: String,
    val createdAtEpochMillis: Long
)

@Serializable
private data class PortfolioPositionDto(
    val id: String,
    val groupId: String,
    val name: String,
    val symbol: String,
    val category: String,
    val quantity: Double,
    val averagePrice: Double,
    val currentPrice: Double,
    val currency: String,
    val tags: List<String>,
    val createdAtEpochMillis: Long,
    val updatedAtEpochMillis: Long
)

@Serializable
private data class PortfolioHistoryPointDto(
    val dateEpochMillis: Long,
    val totalValue: Double
)

private fun PortfolioGroup.toDto(): PortfolioGroupDto = PortfolioGroupDto(
    id = id,
    name = name,
    colorHex = colorHex,
    createdAtEpochMillis = createdAt.toEpochMilli()
)

private fun PortfolioGroupDto.toDomain(): PortfolioGroup = PortfolioGroup(
    id = id,
    name = name,
    colorHex = colorHex,
    createdAt = Instant.ofEpochMilli(createdAtEpochMillis)
)

private fun PortfolioPosition.toDto(): PortfolioPositionDto = PortfolioPositionDto(
    id = id,
    groupId = groupId,
    name = name,
    symbol = symbol,
    category = category.name,
    quantity = quantity,
    averagePrice = averagePrice,
    currentPrice = currentPrice,
    currency = currency,
    tags = tags.sorted(),
    createdAtEpochMillis = createdAt.toEpochMilli(),
    updatedAtEpochMillis = updatedAt.toEpochMilli()
)

private fun PortfolioPositionDto.toDomainOrNull(): PortfolioPosition? {
    val categoryEnum = runCatching { AssetCategory.valueOf(category) }.getOrNull() ?: return null
    return PortfolioPosition(
        id = id,
        groupId = groupId,
        name = name,
        symbol = symbol,
        category = categoryEnum,
        quantity = quantity,
        averagePrice = averagePrice,
        currentPrice = currentPrice,
        currency = currency,
        tags = tags.toSet(),
        createdAt = Instant.ofEpochMilli(createdAtEpochMillis),
        updatedAt = Instant.ofEpochMilli(updatedAtEpochMillis)
    )
}

private fun PortfolioHistoryPointDto.toDomainOrNull(): PortfolioHistoryPoint? {
    val dateValue = runCatching {
        Instant.ofEpochMilli(dateEpochMillis).atZone(ZoneOffset.UTC).toLocalDate()
    }.getOrNull() ?: return null
    return PortfolioHistoryPoint(
        date = dateValue,
        totalValue = totalValue
    )
}
