package de.tradebuddy.data

import java.io.File
import java.util.Locale

actual fun appStorageDir(appName: String): File {
    val osName = System.getProperty("os.name") ?: ""
    val os = osName.lowercase(Locale.ROOT)
    val userHome = System.getProperty("user.home") ?: "."
    return when {
        "win" in os -> {
            val base = System.getenv("LOCALAPPDATA")
                ?: System.getenv("APPDATA")
                ?: userHome
            File(base, appName)
        }
        "mac" in os -> File(File(userHome, "Library/Application Support"), appName)
        else -> {
            val base = System.getenv("XDG_CONFIG_HOME")
                ?: File(userHome, ".config").path
            File(base, appName)
        }
    }
}
