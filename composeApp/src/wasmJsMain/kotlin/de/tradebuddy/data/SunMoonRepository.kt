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
import kotlinx.coroutines.yield

interface SunMoonRepository {
    fun cities(): List<City>
    suspend fun loadDaily(
        date: LocalDate,
        onProgress: ((completed: Int, total: Int) -> Unit)? = null
    ): List<SunMoonTimes>
    suspend fun loadCityDay(date: LocalDate, city: City): SunMoonTimes
    suspend fun loadMonthlyTrend(month: YearMonth, city: City): MonthTrend
}

class DefaultSunMoonRepository(
    private val calculator: AstroCalculator,
    private val cityDataSource: CityDataSource,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : SunMoonRepository {

    override fun cities(): List<City> = cityDataSource.cities()

    override suspend fun loadDaily(
        date: LocalDate,
        onProgress: ((completed: Int, total: Int) -> Unit)?
    ): List<SunMoonTimes> = withContext(dispatcher) {
        val cityList = cities()
        val total = cityList.size.coerceAtLeast(1)
        if (cityList.isEmpty()) {
            onProgress?.invoke(1, 1)
            return@withContext emptyList()
        }

        val out = ArrayList<SunMoonTimes>(cityList.size)
        cityList.forEachIndexed { index, city ->
            out += calculator.calculate(date, city)
            onProgress?.invoke(index + 1, total)
            if ((index + 1) % 2 == 0) {
                yield()
            }
        }
        out
    }

    override suspend fun loadCityDay(date: LocalDate, city: City): SunMoonTimes = withContext(dispatcher) {
        calculator.calculate(date, city)
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

