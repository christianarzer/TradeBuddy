package java.util

class Locale private constructor(
    val languageTag: String
) {
    companion object {
        val ROOT: Locale = Locale("en-US")
        val GERMANY: Locale = Locale("de-DE")
        val US: Locale = Locale("en-US")
    }
}

