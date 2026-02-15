package java.util

class UUID private constructor(
    private val value: String
) {
    override fun toString(): String = value

    companion object {
        fun randomUUID(): UUID = UUID(fallbackUuid())
    }
}

private fun fallbackUuid(): String {
    fun segment(length: Int): String {
        val chars = "0123456789abcdef"
        return buildString(length) {
            repeat(length) {
                append(chars[(kotlin.random.Random.nextInt(16))])
            }
        }
    }
    return "${segment(8)}-${segment(4)}-${segment(4)}-${segment(4)}-${segment(12)}"
}
