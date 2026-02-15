package de.tradebuddy.data

import de.tradebuddy.domain.model.City
import de.tradebuddy.domain.model.MonthTrend
import de.tradebuddy.domain.model.SunMoonTimes
import de.tradebuddy.domain.model.TrendSeries
import de.tradebuddy.domain.util.normAz360
import de.tradebuddy.domain.util.unwrapDegrees
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface SunMoonRepository {
    fun cities(): List<City>
    suspend fun loadDaily(date: LocalDate): List<SunMoonTimes>
    suspend fun loadMonthlyTrend(month: YearMonth, city: City): MonthTrend
}

class DefaultSunMoonRepository(
    private val calculator: AstroCalculator,
    private val cityDataSource: CityDataSource,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : SunMoonRepository {

    override fun cities(): List<City> = cityDataSource.cities()

    override suspend fun loadDaily(date: LocalDate): List<SunMoonTimes> = withContext(dispatcher) {
        cities().map { calculator.calculate(date, it) }
    }

    override suspend fun loadMonthlyTrend(month: YearMonth, city: City): MonthTrend = withContext(dispatcher) {
        val days = (1..month.lengthOfMonth()).map { month.atDay(it) }
        val sunAz = ArrayList<Double?>(days.size)
        val moonAz = ArrayList<Double?>(days.size)
        for (day in days) {
            val r = calculator.calculate(day, city)
            sunAz += r.sunriseAzimuthDeg?.let(::normAz360)
            moonAz += r.moonriseAzimuthDeg?.let(::normAz360)
        }
        MonthTrend(
            city = city,
            days = days,
            sun = TrendSeries("Sonne", unwrapDegrees(sunAz)),
            moon = TrendSeries("Mond", unwrapDegrees(moonAz))
        )
    }
}

