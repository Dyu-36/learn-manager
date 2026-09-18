package com.learnmanager.android

import android.content.Context

object AndroidAppServices {
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    fun context(): Context = checkNotNull(appContext) {
        "AndroidAppServices.initialize(context) must be called before platform services are used"
    }
}
