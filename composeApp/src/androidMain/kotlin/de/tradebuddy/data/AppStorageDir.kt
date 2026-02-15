package de.tradebuddy.data

import de.tradebuddy.AndroidContextHolder
import java.io.File

actual fun appStorageDir(appName: String): File {
    val context = AndroidContextHolder.requireAppContext()
    return File(context.filesDir, appName)
}
