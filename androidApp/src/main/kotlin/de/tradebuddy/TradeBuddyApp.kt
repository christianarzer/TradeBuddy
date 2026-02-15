package de.tradebuddy

import android.app.Application
import de.tradebuddy.AndroidContextHolder

class TradeBuddyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AndroidContextHolder.init(this)
    }
}

