package de.tradebuddy.data

import de.tradebuddy.logging.AppLog
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FileStorageDocument(
    private val file: File,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : StorageDocument {
    override val location: String = file.absolutePath

    override suspend fun readText(): String? = withContext(dispatcher) {
        if (!file.exists()) return@withContext null
        runCatching { file.readText(Charsets.UTF_8) }
            .onFailure { error ->
                AppLog.error(
                    tag = "FileStorageDocument",
                    message = "Failed to read ${file.absolutePath}",
                    throwable = error
                )
            }
            .getOrNull()
    }

    override suspend fun writeText(value: String) {
        withContext(dispatcher) {
            runCatching {
                file.parentFile?.mkdirs()
                file.writeText(value, Charsets.UTF_8)
            }.onFailure { error ->
                AppLog.error(
                    tag = "FileStorageDocument",
                    message = "Failed to write ${file.absolutePath}",
                    throwable = error
                )
            }
        }
    }

    override suspend fun clear() {
        withContext(dispatcher) {
            runCatching { if (file.exists()) file.delete() }
                .onFailure { error ->
                    AppLog.warn(
                        tag = "FileStorageDocument",
                        message = "Failed to clear ${file.absolutePath}",
                        throwable = error
                    )
                }
        }
    }
}

