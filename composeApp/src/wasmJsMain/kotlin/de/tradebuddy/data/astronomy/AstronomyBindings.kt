@file:JsModule("astronomy-engine")
@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)

package de.tradebuddy.data.astronomy

external object Body {
    val Sun: String
    val Moon: String
    val Mercury: String
    val Venus: String
    val Earth: String
    val Mars: String
    val Jupiter: String
    val Saturn: String
    val Uranus: String
    val Neptune: String
    val Pluto: String
}

external class Observer(latitude: Double, longitude: Double, height: Double)

external class AstroTime {
    val date: JsDate
}

external class MoonQuarter {
    val quarter: Int
    val time: AstroTime
}

external class EquatorialCoordinates {
    val ra: Double
    val dec: Double
}

external class HorizontalCoordinates {
    val azimuth: Double
}

external class Vector

external class EclipticCoordinates {
    val elon: Double
}

external fun SearchRiseSet(
    body: String,
    observer: Observer,
    direction: Int,
    dateStart: JsDate,
    limitDays: Double,
    metersAboveGround: Double = definedExternally
): AstroTime?

external fun SearchMoonQuarter(dateStart: JsDate): MoonQuarter
external fun NextMoonQuarter(mq: MoonQuarter): MoonQuarter

external fun Equator(
    body: String,
    date: JsDate,
    observer: Observer,
    ofdate: Boolean,
    aberration: Boolean
): EquatorialCoordinates

external fun Horizon(
    date: JsDate,
    observer: Observer,
    ra: Double,
    dec: Double,
    refraction: String = definedExternally
): HorizontalCoordinates

external fun GeoVector(body: String, date: JsDate, aberration: Boolean): Vector
external fun Ecliptic(eqj: Vector): EclipticCoordinates
