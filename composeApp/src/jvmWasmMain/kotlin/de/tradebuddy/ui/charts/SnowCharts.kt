package de.tradebuddy.ui.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
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
