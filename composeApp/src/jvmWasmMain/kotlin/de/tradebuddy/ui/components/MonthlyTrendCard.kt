package de.tradebuddy.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import de.tradebuddy.domain.model.MonthTrend
import de.tradebuddy.presentation.MonthTrendUiState
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt
import de.tradebuddy.ui.theme.appElevatedCardColors
import org.jetbrains.compose.resources.stringResource
import trade_buddy.composeapp.generated.resources.Res
import trade_buddy.composeapp.generated.resources.action_next_month
import trade_buddy.composeapp.generated.resources.action_prev_month
import trade_buddy.composeapp.generated.resources.format_month_year
import trade_buddy.composeapp.generated.resources.trend_axis_degree
import trade_buddy.composeapp.generated.resources.trend_empty
import trade_buddy.composeapp.generated.resources.trend_hint
import trade_buddy.composeapp.generated.resources.trend_moon
import trade_buddy.composeapp.generated.resources.trend_subtitle
import trade_buddy.composeapp.generated.resources.trend_sun
import trade_buddy.composeapp.generated.resources.trend_title
import trade_buddy.composeapp.generated.resources.value_dash

@Composable
fun MonthlyTrendCard(
    state: MonthTrendUiState,
    highlightDate: LocalDate,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val sunColor = MaterialTheme.colorScheme.primary
    val moonColor = MaterialTheme.colorScheme.secondary

    ElevatedCard(Modifier.fillMaxSize(), colors = appElevatedCardColors()) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TrendHeader(
                month = state.month,
                trend = state.trend,
                sunColor = sunColor,
                moonColor = moonColor,
                onPrevMonth = onPrevMonth,
                onNextMonth = onNextMonth
            )

            if (state.isLoading) LinearProgressIndicator(Modifier.fillMaxWidth())
            state.error?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error) }

            val trend = state.trend
            if (!state.isLoading && state.error == null && trend != null && trend.days.isNotEmpty()) {
                AzimuthTrendChartWithAxes(
                    modifier = Modifier.fillMaxSize(),
                    month = state.month,
                    days = trend.days,
                    trend = trend,
                    highlightDate = highlightDate,
                    sunColor = sunColor,
                    moonColor = moonColor
                )
            } else if (!state.isLoading && state.error == null) {
                Text(stringResource(Res.string.trend_empty), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun TrendHeader(
    month: YearMonth,
    trend: MonthTrend?,
    sunColor: androidx.compose.ui.graphics.Color,
    moonColor: androidx.compose.ui.graphics.Color,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val monthPattern = stringResource(Res.string.format_month_year)
    val monthLabel = remember(month, monthPattern) {
        month.atDay(1).format(DateTimeFormatter.ofPattern(monthPattern, Locale.GERMANY))
    }
    val dash = stringResource(Res.string.value_dash)

    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        IconButton(onClick = onPrevMonth) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(Res.string.action_prev_month)
            )
        }

        Column(Modifier.weight(1f)) {
            Text(stringResource(Res.string.trend_title), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(Res.string.trend_subtitle, monthLabel, trend?.city?.label ?: dash),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        IconButton(onClick = onNextMonth) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowForward,
                contentDescription = stringResource(Res.string.action_next_month)
            )
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AssistChip(
            onClick = {},
            enabled = false,
            label = { Text(stringResource(Res.string.trend_sun)) },
            leadingIcon = { Icon(Icons.Outlined.WbSunny, contentDescription = null) },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = sunColor.copy(alpha = 0.16f),
                labelColor = sunColor,
                leadingIconContentColor = sunColor
            )
        )
        AssistChip(
            onClick = {},
            enabled = false,
            label = { Text(stringResource(Res.string.trend_moon)) },
            leadingIcon = { Icon(Icons.Outlined.DarkMode, contentDescription = null) },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = moonColor.copy(alpha = 0.16f),
                labelColor = moonColor,
                leadingIconContentColor = moonColor
            )
        )
    }
}

@Composable
private fun AzimuthTrendChartWithAxes(
    modifier: Modifier,
    month: YearMonth,
    days: List<LocalDate>,
    trend: MonthTrend,
    highlightDate: LocalDate,
    sunColor: androidx.compose.ui.graphics.Color,
    moonColor: androidx.compose.ui.graphics.Color
) {
    val highlightIndex = remember(month, days, highlightDate) {
        if (YearMonth.from(highlightDate) != month) null
        else days.indexOf(highlightDate).takeIf { it >= 0 }
    }

    val ticks = remember { listOf(360.0, 270.0, 180.0, 90.0, 0.0) }

    val dayCellDp = 26.dp
    val axisWidth = 56.dp
    val chartHeight = 260.dp

    val scroll = rememberScrollState()
    val chartWidth = dayCellDp * days.size

    val outline = MaterialTheme.colorScheme.outline

    Column(modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier
                    .width(axisWidth)
                    .height(chartHeight),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                ticks.forEach { v ->
                    Text(
                        stringResource(Res.string.trend_axis_degree, v.roundToInt()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(modifier = Modifier.horizontalScroll(scroll)) {
                Canvas(
                    modifier = Modifier
                        .width(chartWidth)
                        .height(chartHeight)
                ) {
                    val w = size.width
                    val h = size.height
                    val cellPx = (w / max(1, days.size)).coerceAtLeast(1f)

                    fun xCenter(i: Int): Float = i * cellPx + cellPx / 2f
                    fun y(v: Double): Float {
                        val t = ((v - 0.0) / 360.0).toFloat().coerceIn(0f, 1f)
                        return h - t * h
                    }

                    ticks.forEach { v ->
                        val gy = y(v)
                        drawLine(
                            color = outline.copy(alpha = 0.35f),
                            start = Offset(0f, gy),
                            end = Offset(w, gy),
                            strokeWidth = 1.0f
                        )
                    }

                    for (i in days.indices) {
                        val gx = xCenter(i)
                        drawLine(
                            color = outline.copy(alpha = 0.18f),
                            start = Offset(gx, 0f),
                            end = Offset(gx, h),
                            strokeWidth = 1.0f
                        )
                    }

                    highlightIndex?.let { idx ->
                        val hx = xCenter(idx)
                        drawLine(
                            color = outline.copy(alpha = 0.85f),
                            start = Offset(hx, 0f),
                            end = Offset(hx, h),
                            strokeWidth = 2.0f
                        )
                    }

                    drawSeriesSmoothCircular(
                        values = trend.sun.values,
                        xCenter = ::xCenter,
                        y = ::y,
                        color = sunColor
                    )
                    drawSeriesSmoothCircular(
                        values = trend.moon.values,
                        xCenter = ::xCenter,
                        y = ::y,
                        color = moonColor
                    )

                    highlightIndex?.let { idx ->
                        trend.sun.values.getOrNull(idx)?.let { v ->
                            drawCircle(sunColor, radius = 5f, center = Offset(xCenter(idx), y(v)))
                        }
                        trend.moon.values.getOrNull(idx)?.let { v ->
                            drawCircle(moonColor, radius = 5f, center = Offset(xCenter(idx), y(v)))
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Spacer(Modifier.width(axisWidth))

            Row(
                modifier = Modifier
                    .horizontalScroll(scroll)
                    .width(chartWidth)
            ) {
                for (d in 1..days.size) {
                    Box(
                        modifier = Modifier.width(dayCellDp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            d.toString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            stringResource(Res.string.trend_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSeriesSmoothCircular(
    values: List<Double?>,
    xCenter: (Int) -> Float,
    y: (Double) -> Float,
    color: androidx.compose.ui.graphics.Color,
    breakJumpDeg: Double = 180.0
) {
    val stroke = Stroke(
        width = 3.0f,
        cap = StrokeCap.Round,
        join = StrokeJoin.Round
    )

    val segment = ArrayList<Offset>(values.size)
    var lastV: Double? = null

    fun flush() {
        if (segment.size >= 2) {
            val path = buildSmoothPath(segment)
            drawPath(path, color = color, style = stroke)
        }
        segment.clear()
    }

    for (i in values.indices) {
        val v = values[i]
        if (v == null) {
            lastV = null
            flush()
            continue
        }

        val lastValue = lastV
        if (lastValue != null && abs(v - lastValue) > breakJumpDeg) {
            flush()
        }

        segment += Offset(xCenter(i), y(v))
        lastV = v
    }
    flush()
}

private fun buildSmoothPath(points: List<Offset>, tension: Float = 1f): Path {
    val path = Path()
    if (points.isEmpty()) return path
    if (points.size == 1) {
        path.moveTo(points[0].x, points[0].y)
        return path
    }

    path.moveTo(points[0].x, points[0].y)

    fun get(i: Int): Offset = points[i.coerceIn(0, points.lastIndex)]

    for (i in 0 until points.lastIndex) {
        val p0 = get(i - 1)
        val p1 = get(i)
        val p2 = get(i + 1)
        val p3 = get(i + 2)

        val cp1 = Offset(
            p1.x + (p2.x - p0.x) * (tension / 6f),
            p1.y + (p2.y - p0.y) * (tension / 6f)
        )
        val cp2 = Offset(
            p2.x - (p3.x - p1.x) * (tension / 6f),
            p2.y - (p3.y - p1.y) * (tension / 6f)
        )

        path.cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, p2.x, p2.y)
    }

    return path
}


