package de.tradebuddy

import android.content.Context

object AndroidContextHolder {
    @Volatile
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun requireAppContext(): Context =
        checkNotNull(appContext) { "AndroidContextHolder not initialized." }
}
