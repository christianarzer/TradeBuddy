package de.tradebuddy.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import de.tradebuddy.domain.model.SunMoonTimes
import de.tradebuddy.domain.util.azimuthToCardinalIndex
import de.tradebuddy.domain.util.formatOffsetDiff
import de.tradebuddy.domain.util.formatUtcOffset
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import org.jetbrains.compose.resources.stringResource
import trade_buddy.composeapp.generated.resources.Res
import trade_buddy.composeapp.generated.resources.azimuth_value
import trade_buddy.composeapp.generated.resources.city_no_events
import trade_buddy.composeapp.generated.resources.city_title
import trade_buddy.composeapp.generated.resources.city_local_time
import trade_buddy.composeapp.generated.resources.city_timezone_info
import trade_buddy.composeapp.generated.resources.city_user_time
import trade_buddy.composeapp.generated.resources.event_moonrise
import trade_buddy.composeapp.generated.resources.event_moonset
import trade_buddy.composeapp.generated.resources.event_sunrise
import trade_buddy.composeapp.generated.resources.event_sunset
import trade_buddy.composeapp.generated.resources.label_azimuth
import trade_buddy.composeapp.generated.resources.value_dash

@Composable
fun CityCard(
    r: SunMoonTimes,
    userZone: ZoneId,
    timeFmt: DateTimeFormatter,
    showSun: Boolean,
    showMoon: Boolean,
    showRise: Boolean,
    showSet: Boolean
) {
    val cityZone = remember(r.city.zoneId) { ZoneId.of(r.city.zoneId) }

    val anchorInstant = remember(r.date, cityZone) { r.date.atTime(12, 0).atZone(cityZone).toInstant() }
    val cityOffset = remember(anchorInstant, cityZone) { cityZone.rules.getOffset(anchorInstant) }
    val userOffset = remember(anchorInstant, userZone) { userZone.rules.getOffset(anchorInstant) }

    val diffSeconds = cityOffset.totalSeconds - userOffset.totalSeconds
    val diffLabel = remember(diffSeconds) { formatOffsetDiff(diffSeconds) }
    val dash = stringResource(Res.string.value_dash)

    fun fmtCity(z: ZonedDateTime?) = z?.format(timeFmt) ?: dash
    fun fmtUser(z: ZonedDateTime?) = z?.withZoneSameInstant(userZone)?.format(timeFmt) ?: dash

    ElevatedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Column {
                Text(
                    stringResource(Res.string.city_title, r.city.label, r.city.country),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    stringResource(
                        Res.string.city_timezone_info,
                        r.city.zoneId,
                        formatUtcOffset(cityOffset),
                        formatUtcOffset(userOffset),
                        diffLabel
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(Modifier.fillMaxWidth()) {
                Spacer(Modifier.weight(1f))
                Text(
                    stringResource(Res.string.city_local_time),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.width(90.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    stringResource(Res.string.city_user_time),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.width(90.dp)
                )
            }

            HorizontalDivider()

            val showSunRise = showSun && showRise
            val showSunSet = showSun && showSet
            val showMoonRise = showMoon && showRise
            val showMoonSet = showMoon && showSet
            val showSunGroup = showSunRise || showSunSet
            val showMoonGroup = showMoonRise || showMoonSet

            if (!showSunGroup && !showMoonGroup) {
                Text(
                    stringResource(Res.string.city_no_events),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                if (showSunRise) {
                    EventRow(
                        stringResource(Res.string.event_sunrise),
                        r.sunriseAzimuthDeg,
                        fmtCity(r.sunrise),
                        fmtUser(r.sunrise)
                    )
                }
                if (showSunSet) {
                    EventRow(
                        stringResource(Res.string.event_sunset),
                        r.sunsetAzimuthDeg,
                        fmtCity(r.sunset),
                        fmtUser(r.sunset)
                    )
                }

                if (showSunGroup && showMoonGroup) {
                    HorizontalDivider()
                }

                if (showMoonRise) {
                    EventRow(
                        stringResource(Res.string.event_moonrise),
                        r.moonriseAzimuthDeg,
                        fmtCity(r.moonrise),
                        fmtUser(r.moonrise)
                    )
                }
                if (showMoonSet) {
                    EventRow(
                        stringResource(Res.string.event_moonset),
                        r.moonsetAzimuthDeg,
                        fmtCity(r.moonset),
                        fmtUser(r.moonset)
                    )
                }
            }
        }
    }
}

@Composable
private fun EventRow(
    label: String,
    azimuth: Double?,
    cityTime: String,
    userTime: String
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(label, style = MaterialTheme.typography.bodyMedium)

            if (azimuth != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = stringResource(Res.string.label_azimuth),
                    modifier = Modifier
                        .size(18.dp)
                        .rotate((azimuth - 90.0).toFloat())
                )
                Text(
                    stringResource(
                        Res.string.azimuth_value,
                        azimuth.roundToInt(),
                        cardinalLabel(azimuthToCardinalIndex(azimuth))
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    stringResource(Res.string.value_dash),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(cityTime, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(90.dp))
        Spacer(Modifier.width(8.dp))
        Text(userTime, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(90.dp))
    }
}

