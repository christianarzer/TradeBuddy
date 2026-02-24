package de.tradebuddy.data

import de.tradebuddy.domain.model.City

interface CityDataSource {
    fun cities(): List<City>
}

object DefaultCityDataSource : CityDataSource {
    override fun cities(): List<City> = listOf(
        City("New York", "Vereinigte Staaten", "US", "America/New_York", 40.7128, -74.0060),
        City("Chicago", "Vereinigte Staaten", "US", "America/Chicago", 41.8781, -87.6298),
        City("Los Angeles", "Vereinigte Staaten", "US", "America/Los_Angeles", 34.0522, -118.2437),
        City("Toronto", "Kanada", "CA", "America/Toronto", 43.6532, -79.3832),
        City("Mexiko-Stadt", "Mexiko", "MX", "America/Mexico_City", 19.4326, -99.1332),
        City("Sao Paulo", "Brasilien", "BR", "America/Sao_Paulo", -23.5505, -46.6333),
        City("London", "Vereinigtes Koenigreich", "GB", "Europe/London", 51.5074, -0.1278),
        City("Frankfurt", "Deutschland", "DE", "Europe/Berlin", 50.1109, 8.6821),
        City("Paris", "Frankreich", "FR", "Europe/Paris", 48.8566, 2.3522),
        City("Zuerich", "Schweiz", "CH", "Europe/Zurich", 47.3769, 8.5417),
        City("Amsterdam", "Niederlande", "NL", "Europe/Amsterdam", 52.3676, 4.9041),
        City("Madrid", "Spanien", "ES", "Europe/Madrid", 40.4168, -3.7038),
        City("Mailand", "Italien", "IT", "Europe/Rome", 45.4642, 9.1900),
        City("Wien", "Oesterreich", "AT", "Europe/Vienna", 48.2082, 16.3738),
        City("Stockholm", "Schweden", "SE", "Europe/Stockholm", 59.3293, 18.0686),
        City("Dubai", "Vereinigte Arabische Emirate", "AE", "Asia/Dubai", 25.2048, 55.2708),
        City("Tokyo", "Japan", "JP", "Asia/Tokyo", 35.6762, 139.6503),
        City("Shanghai", "China", "CN", "Asia/Shanghai", 31.2304, 121.4737),
        City("Hong Kong", "Hong Kong", "HK", "Asia/Hong_Kong", 22.3193, 114.1694),
        City("Singapur", "Singapur", "SG", "Asia/Singapore", 1.3521, 103.8198),
        City("Seoul", "Suedkorea", "KR", "Asia/Seoul", 37.5665, 126.9780),
        City("Mumbai", "Indien", "IN", "Asia/Kolkata", 19.0760, 72.8777),
        City("Bangkok", "Thailand", "TH", "Asia/Bangkok", 13.7563, 100.5018),
        City("Sydney", "Australien", "AU", "Australia/Sydney", -33.8688, 151.2093)
    )
}
