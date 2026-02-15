package de.tradebuddy.domain.util

import de.tradebuddy.domain.model.City

fun City.key(): String = "${label}|${countryCode}|${zoneId}|${latitude}|${longitude}"

