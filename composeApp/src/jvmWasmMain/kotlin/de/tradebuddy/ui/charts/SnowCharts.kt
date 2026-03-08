package de.tradebuddy.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.multiplatform.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.multiplatform.cartesian.axis.HorizontalAxis.Companion.rememberBottom
import com.patrykandpatrick.vico.multiplatform.cartesian.axis.VerticalAxis.Companion.rememberStart
import com.patrykandpatrick.vico.multiplatform.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.multiplatform.cartesian.data.columnSeries
import com.patrykandpatrick.vico.multiplatform.cartesian.data.lineSeries
import com.patrykandpatrick.vico.multiplatform.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.multiplatform.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.multiplatform.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.multiplatform.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.multiplatform.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.multiplatform.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.multiplatform.common.Fill
import com.patrykandpatrick.vico.multiplatform.common.component.rememberLineComponent
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt
import trade_buddy.composeapp.generated.resources.Res
import trade_buddy.composeapp.generated.resources.chart_empty_default

data class SnowLineSeries(
    val name: String,
    val values: List<Double>,
    val color: Color,
)

data class SnowBarEntry(
    val label: String,
    val value: Double,
    val color: Color,
)

data class SnowCandleEntry(
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val timeLabel: String = ""
)

data class SnowChartMarker(
    val index: Int,
    val value: Double,
    val color: Color,
    val isBuy: Boolean,
    val sizeScale: Float = 1f
)

data class SnowChartSegment(
    val startIndex: Int,
    val startValue: Double,
    val endIndex: Int,
    val endValue: Double,
    val color: Color,
    val strokeWidth: Float = 2.2f
)

data class SnowChartAnnotation(
    val index: Int,
    val text: String,
    val color: Color
)

@Composable
fun SnowLineChart(
    series: List<SnowLineSeries>,
    modifier: Modifier = Modifier,
    showAxes: Boolean = true,
    smooth: Boolean = true,
) {
    if (series.isEmpty() || series.all { it.values.isEmpty() }) {
        SnowChartEmptyState(modifier = modifier)
        return
    }

    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(series) {
        modelProducer.runTransaction {
            lineSeries {
                series.forEach { candidate ->
                    series(candidate.values)
                }
            }
        }
    }

    val lines = series.map { candidate ->
        LineCartesianLayer.rememberLine(
            fill = LineCartesianLayer.LineFill.single(Fill(candidate.color)),
            pointConnector = if (smooth) LineCartesianLayer.PointConnector.cubic() else LineCartesianLayer.PointConnector.Sharp
        )
    }

    val lineLayer = rememberLineCartesianLayer(
        lineProvider = LineCartesianLayer.LineProvider.series(lines)
    )

    val chart = rememberCartesianChart(
        lineLayer,
        startAxis = if (showAxes) rememberStart() else null,
        bottomAxis = if (showAxes) rememberBottom() else null,
    )

    CartesianChartHost(
        chart = chart,
        modelProducer = modelProducer,
        modifier = modifier,
    )
}

@Composable
fun SnowSparkline(
    values: List<Double>,
    color: Color,
    modifier: Modifier = Modifier,
) {
    SnowLineChart(
        series = listOf(SnowLineSeries(name = "spark", values = values, color = color)),
        modifier = modifier,
        showAxes = false,
        smooth = true,
    )
}

@Composable
fun SnowLineChartWithMarkers(
    values: List<Double>,
    lineColor: Color,
    markers: List<SnowChartMarker>,
    segments: List<SnowChartSegment> = emptyList(),
    modifier: Modifier = Modifier
) {
    if (values.isEmpty()) {
        SnowChartEmptyState(modifier = modifier)
        return
    }

    val boundedMarkers = markers.filter { it.index in values.indices }
    val boundedSegments = segments.filter { it.startIndex in values.indices && it.endIndex in values.indices }
    val minValue = minOf(values.minOrNull() ?: 0.0, boundedMarkers.minOfOrNull { it.value } ?: Double.MAX_VALUE)
    val maxValue = maxOf(values.maxOrNull() ?: 0.0, boundedMarkers.maxOfOrNull { it.value } ?: Double.MIN_VALUE)
    val valueRange = (maxValue - minValue).takeIf { it > 0.0 } ?: 1.0

    Box(modifier = modifier) {
        SnowLineChart(
            series = listOf(SnowLineSeries(name = "price", values = values, color = lineColor)),
            modifier = Modifier.fillMaxSize(),
            showAxes = false,
            smooth = false
        )
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (values.size < 2) return@Canvas
            boundedSegments.forEach { segment ->
                val sx = (segment.startIndex.toFloat() / values.lastIndex.coerceAtLeast(1).toFloat()) * size.width
                val sy = (((maxValue - segment.startValue) / valueRange).toFloat() * size.height).coerceIn(0f, size.height)
                val ex = (segment.endIndex.toFloat() / values.lastIndex.coerceAtLeast(1).toFloat()) * size.width
                val ey = (((maxValue - segment.endValue) / valueRange).toFloat() * size.height).coerceIn(0f, size.height)
                drawLine(
                    color = segment.color.copy(alpha = 0.7f),
                    start = Offset(sx, sy),
                    end = Offset(ex, ey),
                    strokeWidth = segment.strokeWidth
                )
            }
            boundedMarkers.forEach { marker ->
                val x = (marker.index.toFloat() / (values.lastIndex).coerceAtLeast(1).toFloat()) * size.width
                val y = (((maxValue - marker.value) / valueRange).toFloat() * size.height).coerceIn(0f, size.height)
                val tip = 8f * marker.sizeScale.coerceAtLeast(0.6f)
                val wing = 6f * marker.sizeScale.coerceAtLeast(0.6f)
                val path = Path().apply {
                    if (marker.isBuy) {
                        moveTo(x, y - tip)
                        lineTo(x - wing, y + wing)
                        lineTo(x + wing, y + wing)
                    } else {
                        moveTo(x, y + tip)
                        lineTo(x - wing, y - wing)
                        lineTo(x + wing, y - wing)
                    }
                    close()
                }
                drawPath(path = path, color = marker.color)
            }
        }
    }
}

@Composable
fun SnowCandleChartWithMarkers(
    candles: List<SnowCandleEntry>,
    markers: List<SnowChartMarker>,
    segments: List<SnowChartSegment> = emptyList(),
    modifier: Modifier = Modifier,
    upColor: Color = Color(0xFF1FA27A),
    downColor: Color = Color(0xFFCF4A4A),
    wickColor: Color = Color.Unspecified,
    crosshairIndex: Int? = null,
    crosshairValue: Double? = null,
) {
    if (candles.isEmpty()) {
        SnowChartEmptyState(modifier = modifier)
        return
    }
    val boundedMarkers = markers.filter { it.index in candles.indices }
    val boundedSegments = segments.filter { it.startIndex in candles.indices && it.endIndex in candles.indices }
    val low = minOf(
        candles.minOf { it.low },
        boundedMarkers.minOfOrNull { it.value } ?: Double.MAX_VALUE,
        boundedSegments.minOfOrNull { minOf(it.startValue, it.endValue) } ?: Double.MAX_VALUE
    )
    val high = maxOf(
        candles.maxOf { it.high },
        boundedMarkers.maxOfOrNull { it.value } ?: Double.MIN_VALUE,
        boundedSegments.maxOfOrNull { maxOf(it.startValue, it.endValue) } ?: Double.MIN_VALUE
    )
    val range = (high - low).takeIf { it > 0.0 } ?: 1.0
    val resolvedWickColor = if (wickColor == Color.Unspecified) {
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
    } else {
        wickColor
    }
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
    val crosshairVerticalColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
    val crosshairHorizontalColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.42f)

    Canvas(modifier = modifier) {
        if (candles.isEmpty()) return@Canvas
        val count = candles.size.coerceAtLeast(1)
        val slot = size.width / count.toFloat()
        val bodyWidth = (slot * 0.62f).coerceIn(1f, 18f)

        fun y(value: Double): Float =
            (((high - value) / range).toFloat() * size.height).coerceIn(0f, size.height)

        // Subtle horizontal guides.
        repeat(4) { idx ->
            val ratio = idx / 3f
            val yy = ratio * size.height
            drawLine(
                color = gridColor,
                start = Offset(0f, yy),
                end = Offset(size.width, yy),
                strokeWidth = 1f
            )
        }

        boundedSegments.forEach { segment ->
            val startX = (segment.startIndex + 0.5f) * slot
            val endX = (segment.endIndex + 0.5f) * slot
            drawLine(
                color = segment.color.copy(alpha = 0.72f),
                start = Offset(startX, y(segment.startValue)),
                end = Offset(endX, y(segment.endValue)),
                strokeWidth = segment.strokeWidth
            )
        }

        candles.forEachIndexed { index, candle ->
            val centerX = (index + 0.5f) * slot
            val wickTop = y(candle.high)
            val wickBottom = y(candle.low)
            drawLine(
                color = resolvedWickColor,
                start = Offset(centerX, wickTop),
                end = Offset(centerX, wickBottom),
                strokeWidth = 1.1f
            )

            val openY = y(candle.open)
            val closeY = y(candle.close)
            val top = minOf(openY, closeY)
            val bottom = maxOf(openY, closeY)
            val color = if (candle.close >= candle.open) upColor else downColor
            val bodyHeight = (bottom - top).coerceAtLeast(1f)
            drawRect(
                color = color,
                topLeft = Offset(centerX - bodyWidth / 2f, top),
                size = androidx.compose.ui.geometry.Size(bodyWidth, bodyHeight)
            )
        }

        boundedMarkers.forEach { marker ->
            val centerX = (marker.index + 0.5f) * slot
            val centerY = y(marker.value)
            val tip = 7f * marker.sizeScale.coerceAtLeast(0.6f)
            val wing = 5.5f * marker.sizeScale.coerceAtLeast(0.6f)
            val path = Path().apply {
                if (marker.isBuy) {
                    moveTo(centerX, centerY - tip)
                    lineTo(centerX - wing, centerY + wing)
                    lineTo(centerX + wing, centerY + wing)
                } else {
                    moveTo(centerX, centerY + tip)
                    lineTo(centerX - wing, centerY - wing)
                    lineTo(centerX + wing, centerY - wing)
                }
                close()
            }
            val fill = if (marker.isBuy) {
                lerp(marker.color, Color.White, 0.08f)
            } else {
                lerp(marker.color, Color.Black, 0.08f)
            }
            drawPath(path = path, color = fill)
        }

        if (crosshairIndex != null && crosshairIndex in candles.indices) {
            val cx = (crosshairIndex + 0.5f) * slot
            drawLine(
                color = crosshairVerticalColor,
                start = Offset(cx, 0f),
                end = Offset(cx, size.height),
                strokeWidth = 1f
            )
            val value = crosshairValue ?: candles[crosshairIndex].close
            val cy = y(value)
            drawLine(
                color = crosshairHorizontalColor,
                start = Offset(0f, cy),
                end = Offset(size.width, cy),
                strokeWidth = 1f
            )
        }
    }
}

@Composable
fun SnowInteractiveCandleChart(
    candles: List<SnowCandleEntry>,
    markers: List<SnowChartMarker>,
    segments: List<SnowChartSegment> = emptyList(),
    annotations: List<SnowChartAnnotation> = emptyList(),
    modifier: Modifier = Modifier,
    upColor: Color = Color(0xFF1FA27A),
    downColor: Color = Color(0xFFCF4A4A),
) {
    if (candles.isEmpty()) {
        SnowChartEmptyState(modifier = modifier)
        return
    }
    val minVisible = 18
    var visibleCount by remember(candles.size) {
        mutableStateOf(candles.size.coerceIn(minVisible, 240))
    }
    var startIndex by remember(candles.size, visibleCount) {
        mutableStateOf((candles.size - visibleCount).coerceAtLeast(0))
    }
    var selectedIndex by remember(candles.size) { mutableStateOf<Int?>(null) }
    var chartWidthPx by remember { mutableStateOf(1f) }
    val density = LocalDensity.current
    val maxStart = (candles.size - visibleCount).coerceAtLeast(0)
    if (startIndex > maxStart) {
        startIndex = maxStart
    }

    val viewCandles = candles.subList(startIndex, (startIndex + visibleCount).coerceAtMost(candles.size))
    val viewMarkers = markers
        .filter { it.index in startIndex until (startIndex + visibleCount).coerceAtMost(candles.size) }
        .map { it.copy(index = it.index - startIndex) }
    val viewSegments = segments
        .filter {
            it.startIndex in startIndex until (startIndex + visibleCount).coerceAtMost(candles.size) ||
                it.endIndex in startIndex until (startIndex + visibleCount).coerceAtMost(candles.size)
        }
        .map { segment ->
            val endExclusive = (startIndex + visibleCount).coerceAtMost(candles.size)
            val mappedStart = segment.startIndex.coerceIn(startIndex, endExclusive - 1) - startIndex
            val mappedEnd = segment.endIndex.coerceIn(startIndex, endExclusive - 1) - startIndex
            segment.copy(startIndex = mappedStart, endIndex = mappedEnd)
        }

    val selectedInView = selectedIndex?.takeIf { it in startIndex until startIndex + visibleCount }?.minus(startIndex)
    val selectedCandle = selectedIndex?.takeIf { it in candles.indices }?.let { candles[it] }
    val axisLow = minOf(
        viewCandles.minOfOrNull { it.low } ?: 0.0,
        viewMarkers.minOfOrNull { it.value } ?: Double.MAX_VALUE,
        viewSegments.minOfOrNull { minOf(it.startValue, it.endValue) } ?: Double.MAX_VALUE
    )
    val axisHigh = maxOf(
        viewCandles.maxOfOrNull { it.high } ?: 0.0,
        viewMarkers.maxOfOrNull { it.value } ?: Double.MIN_VALUE,
        viewSegments.maxOfOrNull { maxOf(it.startValue, it.endValue) } ?: Double.MIN_VALUE
    )
    val axisRange = (axisHigh - axisLow).takeIf { it > 0.0 } ?: 1.0
    val axisTicks = List(5) { idx ->
        val ratio = idx / 4.0
        axisHigh - axisRange * ratio
    }
    val startTimeLabel = viewCandles.firstOrNull()?.timeLabel?.takeIf { it.isNotBlank() } ?: "#${startIndex + 1}"
    val midLocalIndex = viewCandles.lastIndex / 2
    val midTimeLabel = viewCandles.getOrNull(midLocalIndex)?.timeLabel?.takeIf { it.isNotBlank() }
        ?: "#${startIndex + midLocalIndex + 1}"
    val endTimeLabel = viewCandles.lastOrNull()?.timeLabel?.takeIf { it.isNotBlank() }
        ?: "#${startIndex + viewCandles.size}"

    Column(modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth().height(240.dp)) {
            Box(modifier = Modifier.fillMaxSize()) {
                SnowCandleChartWithMarkers(
                    candles = viewCandles,
                    markers = viewMarkers,
                    segments = viewSegments,
                    upColor = upColor,
                    downColor = downColor,
                    crosshairIndex = selectedInView,
                    crosshairValue = selectedCandle?.close,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 62.dp)
                        .onSizeChanged {
                            chartWidthPx = it.width.toFloat().coerceAtLeast(1f)
                        }
                        .pointerInput(candles.size, startIndex, visibleCount, chartWidthPx) {
                            detectTapGestures { offset ->
                                val count = visibleCount.coerceAtLeast(1)
                                val local = ((offset.x / chartWidthPx) * count).toInt().coerceIn(0, count - 1)
                                val global = (startIndex + local).coerceIn(0, candles.lastIndex)
                                selectedIndex = if (selectedIndex == global) null else global
                            }
                        }
                        .pointerInput(candles.size, startIndex, visibleCount, chartWidthPx) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val shift = ((-dragAmount.x / chartWidthPx) * visibleCount).roundToInt()
                                if (shift != 0) {
                                    startIndex = (startIndex + shift).coerceIn(0, maxStart)
                                }
                            }
                        }
                )

                Row(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 6.dp, end = 68.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    ChartControlButton("<") {
                        val shift = (visibleCount / 6).coerceAtLeast(1)
                        startIndex = (startIndex - shift).coerceAtLeast(0)
                    }
                    ChartControlButton(">") {
                        val shift = (visibleCount / 6).coerceAtLeast(1)
                        startIndex = (startIndex + shift).coerceAtMost(maxStart)
                    }
                    ChartControlButton("+") {
                        val next = (visibleCount * 0.82f).roundToInt().coerceIn(minVisible, candles.size)
                        val center = startIndex + visibleCount / 2
                        visibleCount = next
                        startIndex = (center - next / 2).coerceIn(0, (candles.size - next).coerceAtLeast(0))
                    }
                    ChartControlButton("-") {
                        val next = (visibleCount * 1.22f).roundToInt().coerceIn(minVisible, candles.size)
                        val center = startIndex + visibleCount / 2
                        visibleCount = next
                        startIndex = (center - next / 2).coerceIn(0, (candles.size - next).coerceAtLeast(0))
                    }
                    ChartControlButton("R") {
                        visibleCount = candles.size.coerceIn(minVisible, 240)
                        startIndex = (candles.size - visibleCount).coerceAtLeast(0)
                        selectedIndex = null
                    }
                }

                selectedCandle?.let { candle ->
                    Surface(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp),
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                        tonalElevation = 2.dp
                    ) {
                        Text(
                            text = buildString {
                                if (candle.timeLabel.isNotBlank()) {
                                    append(candle.timeLabel)
                                    append(" | ")
                                }
                                append("O ")
                                append(candle.open.prettyCompact())
                                append(" H ")
                                append(candle.high.prettyCompact())
                                append(" L ")
                                append(candle.low.prettyCompact())
                                append(" C ")
                                append(candle.close.prettyCompact())
                            },
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }

                val visibleAnnotations = annotations.filter { it.index in startIndex until startIndex + visibleCount }
                visibleAnnotations.take(4).forEach { annotation ->
                    val count = visibleCount.coerceAtLeast(1)
                    val localIndex = (annotation.index - startIndex).coerceIn(0, count - 1)
                    val xPx = (((localIndex + 0.5f) / count.toFloat()) * chartWidthPx).roundToInt()
                    val xOffset = with(density) { (xPx - 42).coerceAtLeast(2).toDp() }
                    Text(
                        text = annotation.text,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(top = 34.dp)
                            .offset { IntOffset(with(density) { xOffset.roundToPx() }, 0) }
                            .clip(RoundedCornerShape(4.dp))
                            .background(annotation.color.copy(alpha = 0.22f))
                            .border(1.dp, annotation.color.copy(alpha = 0.58f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .width(62.dp)
                    .fillMaxHeight()
                    .align(Alignment.CenterEnd)
                    .padding(start = 4.dp, end = 2.dp, top = 6.dp, bottom = 6.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.End
            ) {
                axisTicks.forEach { tick ->
                    Text(
                        text = tick.prettyCompact(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, start = 2.dp, end = 64.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = startTimeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = midTimeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = endTimeLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ChartControlButton(
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
fun SnowBarChart(
    entries: List<SnowBarEntry>,
    modifier: Modifier = Modifier,
) {
    if (entries.isEmpty()) {
        SnowChartEmptyState(modifier = modifier)
        return
    }

    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(entries) {
        modelProducer.runTransaction {
            columnSeries {
                series(entries.map { it.value })
            }
        }
    }

    val baseColor = entries.first().color
    val columnLayer = rememberColumnCartesianLayer(
        columnProvider = ColumnCartesianLayer.ColumnProvider.series(
            rememberLineComponent(fill = Fill(baseColor))
        )
    )

    val chart = rememberCartesianChart(
        columnLayer,
        startAxis = rememberStart(),
        bottomAxis = null,
    )

    CartesianChartHost(
        chart = chart,
        modelProducer = modelProducer,
        modifier = modifier,
    )
}

@Composable
fun SnowChartLegend(
    items: List<Pair<String, Color>>,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items.forEach { (label, color) ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(
                    modifier = Modifier
                        .size(8.dp)
                        .background(color, CircleShape)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun SnowChartEmptyState(
    modifier: Modifier = Modifier,
    text: String = stringResource(Res.string.chart_empty_default)
) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(8.dp)
        )
    }
}

private fun Double.prettyCompact(): String {
    val rounded = kotlin.math.round(this * 100.0) / 100.0
    val text = rounded.toString()
    return if (text.endsWith(".0")) text.dropLast(2) else text
}
