package de.tradebuddy.data

interface StorageDocument {
    val location: String
    suspend fun readText(): String?
    suspend fun writeText(value: String)
    suspend fun clear()
}

