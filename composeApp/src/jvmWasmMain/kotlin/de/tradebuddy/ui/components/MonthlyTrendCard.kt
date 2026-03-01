package de.tradebuddy.ui.components

import de.tradebuddy.domain.model.MonthTrend
import de.tradebuddy.ui.icons.SnowIcons
import de.tradebuddy.presentation.MonthTrendUiState
import de.tradebuddy.ui.components.shared.SnowMonthSwitchToolbar
import de.tradebuddy.ui.charts.SnowLineChart
import de.tradebuddy.ui.charts.SnowLineSeries
import de.tradebuddy.ui.theme.appElevatedCardColors
import de.tradebuddy.ui.theme.appFlatCardElevation
import de.tradebuddy.ui.theme.extended
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource
import trade_buddy.composeapp.generated.resources.Res
import trade_buddy.composeapp.generated.resources.action_next_month
import trade_buddy.composeapp.generated.resources.action_prev_month
import trade_buddy.composeapp.generated.resources.action_today
import trade_buddy.composeapp.generated.resources.format_month_year
import trade_buddy.composeapp.generated.resources.trend_axis_degree
import trade_buddy.composeapp.generated.resources.trend_empty
import trade_buddy.composeapp.generated.resources.trend_hint
import trade_buddy.composeapp.generated.resources.trend_moon
import trade_buddy.composeapp.generated.resources.trend_sun
import trade_buddy.composeapp.generated.resources.trend_title

@Composable
fun MonthlyTrendCard(
    state: MonthTrendUiState,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val ext = MaterialTheme.extended
    val sunColor = ext.planetSun
    val moonColor = ext.planetMoon
    val currentDate = remember { LocalDate.now(ZoneId.systemDefault()) }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = appElevatedCardColors(),
        elevation = appFlatCardElevation()
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TrendHeader(
                month = state.month,
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
                    modifier = Modifier.fillMaxWidth(),
                    days = trend.days,
                    trend = trend,
                    currentDate = currentDate,
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
    sunColor: androidx.compose.ui.graphics.Color,
    moonColor: androidx.compose.ui.graphics.Color,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    val monthPattern = stringResource(Res.string.format_month_year)
    val monthLabel = remember(month, monthPattern) {
        month.atDay(1).format(DateTimeFormatter.ofPattern(monthPattern, Locale.GERMANY))
    }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(stringResource(Res.string.trend_title), style = MaterialTheme.typography.titleMedium)
        SnowMonthSwitchToolbar(
            monthLabel = monthLabel,
            onPrevious = onPrevMonth,
            onNext = onNextMonth,
            previousContentDescription = stringResource(Res.string.action_prev_month),
            nextContentDescription = stringResource(Res.string.action_next_month)
        )
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        AssistChip(
            onClick = {},
            enabled = true,
            label = { Text(stringResource(Res.string.trend_sun)) },
            leadingIcon = { Icon(SnowIcons.Sun, contentDescription = null) },
            border = BorderStroke(1.dp, sunColor.copy(alpha = 0.7f)),
            colors = AssistChipDefaults.assistChipColors(
                containerColor = sunColor.copy(alpha = 0.16f),
                labelColor = sunColor,
                leadingIconContentColor = sunColor
            )
        )
        AssistChip(
            onClick = {},
            enabled = true,
            label = { Text(stringResource(Res.string.trend_moon)) },
            leadingIcon = { Icon(SnowIcons.Moon, contentDescription = null) },
            border = BorderStroke(1.dp, moonColor.copy(alpha = 0.7f)),
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
    days: List<LocalDate>,
    trend: MonthTrend,
    currentDate: LocalDate,
    sunColor: androidx.compose.ui.graphics.Color,
    moonColor: androidx.compose.ui.graphics.Color
) {
    val ticks = remember { listOf(360.0, 270.0, 180.0, 90.0, 0.0) }
    val dayCellDp = 26.dp
    val axisWidth = 56.dp
    val chartHeight = 260.dp
    val scroll = rememberScrollState()
    val chartWidth = dayCellDp * days.size
    val sunValues = remember(trend.sun.values) { trend.sun.values.fillSeriesGaps() }
    val moonValues = remember(trend.moon.values) { trend.moon.values.fillSeriesGaps() }
    val currentDayIndex = remember(days, currentDate) { days.indexOf(currentDate) }
    val markerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)

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
                SnowLineChart(
                    modifier = Modifier
                        .width(chartWidth)
                        .height(chartHeight),
                    series = listOf(
                        SnowLineSeries(
                            name = "sun",
                            values = sunValues,
                            color = sunColor
                        ),
                        SnowLineSeries(
                            name = "moon",
                            values = moonValues,
                            color = moonColor
                        )
                    ),
                    showAxes = false
                )
                if (currentDayIndex >= 0) {
                    Canvas(
                        modifier = Modifier
                            .width(chartWidth)
                            .height(chartHeight)
                    ) {
                        val dayWidthPx = size.width / days.size.toFloat()
                        val x = (currentDayIndex + 0.5f) * dayWidthPx
                        drawLine(
                            color = markerColor,
                            start = Offset(x, 0f),
                            end = Offset(x, size.height),
                            strokeWidth = 1.5.dp.toPx()
                        )
                        drawCircle(
                            color = markerColor,
                            radius = 3.dp.toPx(),
                            center = Offset(x, 8.dp.toPx())
                        )
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
                days.forEachIndexed { index, day ->
                    val isCurrentDay = day == currentDate
                    Box(
                        modifier = Modifier.width(dayCellDp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            (index + 1).toString(),
                            style = if (isCurrentDay) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodySmall,
                            color = if (isCurrentDay) sunColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        Text(
            text = if (currentDayIndex >= 0) {
                "${stringResource(Res.string.trend_hint)} ${stringResource(Res.string.action_today)}: ${currentDate.dayOfMonth}"
            } else {
                stringResource(Res.string.trend_hint)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun List<Double?>.fillSeriesGaps(): List<Double> {
    if (isEmpty()) return emptyList()
    var carry = firstOrNull { it != null } ?: 0.0
    return map { value ->
        if (value != null) {
            carry = value
            value
        } else {
            carry
        }
    }
}


