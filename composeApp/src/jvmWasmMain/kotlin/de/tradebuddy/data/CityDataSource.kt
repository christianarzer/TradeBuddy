package de.tradebuddy.data

import de.tradebuddy.domain.model.City

interface CityDataSource {
    fun cities(): List<City>
}

object DefaultCityDataSource : CityDataSource {
    override fun cities(): List<City> = listOf(
        City("New York", "Vereinigte Staaten", "US", "America/New_York", 40.7128, -74.0060),
        City("Toronto", "Kanada", "CA", "America/Toronto", 43.6532, -79.3832),
        City("Mexiko-Stadt", "Mexiko", "MX", "America/Mexico_City", 19.4326, -99.1332),
        City("Sao Paulo", "Brasilien", "BR", "America/Sao_Paulo", -23.5505, -46.6333),
        City("Buenos Aires", "Argentinien", "AR", "America/Argentina/Buenos_Aires", -34.6037, -58.3816),
        City("Santiago", "Chile", "CL", "America/Santiago", -33.4489, -70.6693),
        City("Lima", "Peru", "PE", "America/Lima", -12.0464, -77.0428),
        City("Bogota", "Kolumbien", "CO", "America/Bogota", 4.7110, -74.0721),
        City("London", "Vereinigtes Königreich", "GB", "Europe/London", 51.5074, -0.1278),
        City("Frankfurt", "Deutschland", "DE", "Europe/Berlin", 50.1109, 8.6821),
        City("Paris", "Frankreich", "FR", "Europe/Paris", 48.8566, 2.3522),
        City("Zürich", "Schweiz", "CH", "Europe/Zurich", 47.3769, 8.5417),
        City("Amsterdam", "Niederlande", "NL", "Europe/Amsterdam", 52.3676, 4.9041),
        City("Stockholm", "Schweden", "SE", "Europe/Stockholm", 59.3293, 18.0686),
        City("Oslo", "Norwegen", "NO", "Europe/Oslo", 59.9139, 10.7522),
        City("Kopenhagen", "Dänemark", "DK", "Europe/Copenhagen", 55.6761, 12.5683),
        City("Madrid", "Spanien", "ES", "Europe/Madrid", 40.4168, -3.7038),
        City("Mailand", "Italien", "IT", "Europe/Rome", 45.4642, 9.1900),
        City("Wien", "Österreich", "AT", "Europe/Vienna", 48.2082, 16.3738),
        City("Brüssel", "Belgien", "BE", "Europe/Brussels", 50.8503, 4.3517),
        City("Dublin", "Irland", "IE", "Europe/Dublin", 53.3498, -6.2603),
        City("Lissabon", "Portugal", "PT", "Europe/Lisbon", 38.7223, -9.1393),
        City("Helsinki", "Finnland", "FI", "Europe/Helsinki", 60.1699, 24.9384),
        City("Warschau", "Polen", "PL", "Europe/Warsaw", 52.2297, 21.0122),
        City("Prag", "Tschechien", "CZ", "Europe/Prague", 50.0755, 14.4378),
        City("Budapest", "Ungarn", "HU", "Europe/Budapest", 47.4979, 19.0402),
        City("Athen", "Griechenland", "GR", "Europe/Athens", 37.9838, 23.7275),
        City("Istanbul", "Türkei", "TR", "Europe/Istanbul", 41.0082, 28.9784),
        City("Moskau", "Russland", "RU", "Europe/Moscow", 55.7558, 37.6173),
        City("Bukarest", "Rumänien", "RO", "Europe/Bucharest", 44.4268, 26.1025),
        City("Sofia", "Bulgarien", "BG", "Europe/Sofia", 42.6977, 23.3219),
        City("Luxemburg", "Luxemburg", "LU", "Europe/Luxembourg", 49.6116, 6.1319),
        City("Dubai", "Vereinigte Arabische Emirate", "AE", "Asia/Dubai", 25.2048, 55.2708),
        City("Riad", "Saudi-Arabien", "SA", "Asia/Riyadh", 24.7136, 46.6753),
        City("Doha", "Katar", "QA", "Asia/Qatar", 25.2854, 51.5310),
        City("Tel Aviv", "Israel", "IL", "Asia/Jerusalem", 32.0853, 34.7818),
        City("Kairo", "Ägypten", "EG", "Africa/Cairo", 30.0444, 31.2357),
        City("Johannesburg", "Südafrika", "ZA", "Africa/Johannesburg", -26.2041, 28.0473),
        City("Lagos", "Nigeria", "NG", "Africa/Lagos", 6.5244, 3.3792),
        City("Nairobi", "Kenia", "KE", "Africa/Nairobi", -1.2921, 36.8219),
        City("Casablanca", "Marokko", "MA", "Africa/Casablanca", 33.5731, -7.5898),
        City("Tokio", "Japan", "JP", "Asia/Tokyo", 35.6762, 139.6503),
        City("Shanghai", "China", "CN", "Asia/Shanghai", 31.2304, 121.4737),
        City("Hongkong", "Hongkong", "HK", "Asia/Hong_Kong", 22.3193, 114.1694),
        City("Singapur", "Singapur", "SG", "Asia/Singapore", 1.3521, 103.8198),
        City("Seoul", "Südkorea", "KR", "Asia/Seoul", 37.5665, 126.9780),
        City("Taipeh", "Taiwan", "TW", "Asia/Taipei", 25.0330, 121.5654),
        City("Mumbai", "Indien", "IN", "Asia/Kolkata", 19.0760, 72.8777),
        City("Bangkok", "Thailand", "TH", "Asia/Bangkok", 13.7563, 100.5018),
        City("Kuala Lumpur", "Malaysia", "MY", "Asia/Kuala_Lumpur", 3.1390, 101.6869),
        City("Jakarta", "Indonesien", "ID", "Asia/Jakarta", -6.2088, 106.8456),
        City("Manila", "Philippinen", "PH", "Asia/Manila", 14.5995, 120.9842),
        City("Ho-Chi-Minh-Stadt", "Vietnam", "VN", "Asia/Ho_Chi_Minh", 10.8231, 106.6297),
        City("Sydney", "Australien", "AU", "Australia/Sydney", -33.8688, 151.2093),
        City("Auckland", "Neuseeland", "NZ", "Pacific/Auckland", -36.8485, 174.7633)
    )
}

