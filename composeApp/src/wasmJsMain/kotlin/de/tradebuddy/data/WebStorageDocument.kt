package de.tradebuddy.data

import de.tradebuddy.logging.AppLog
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WebStorageDocument(
    private val key: String,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : StorageDocument {
    override val location: String = "browser://localStorage/$key"

    override suspend fun readText(): String? = withContext(dispatcher) {
        runCatching { window.localStorage.getItem(key) }
            .onFailure { error ->
                AppLog.error(
                    tag = "WebStorageDocument",
                    message = "Failed to read localStorage key $key",
                    throwable = error
                )
            }
            .getOrNull()
    }

    override suspend fun writeText(value: String) {
        withContext(dispatcher) {
            runCatching { window.localStorage.setItem(key, value) }
                .onFailure { error ->
                    AppLog.error(
                        tag = "WebStorageDocument",
                        message = "Failed to write localStorage key $key",
                        throwable = error
                    )
                }
        }
    }

    override suspend fun clear() {
        withContext(dispatcher) {
            runCatching { window.localStorage.removeItem(key) }
                .onFailure { error ->
                    AppLog.warn(
                        tag = "WebStorageDocument",
                        message = "Failed to clear localStorage key $key",
                        throwable = error
                    )
                }
        }
    }
}

