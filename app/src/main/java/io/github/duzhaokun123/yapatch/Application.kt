package io.github.duzhaokun123.yapatch

import android.annotation.SuppressLint
import com.google.android.material.color.DynamicColors
import io.github.duzhaokun123.yapatch.utils.Settings


@SuppressLint("StaticFieldLeak")
lateinit var app: Application

class Application : android.app.Application() {
    init {
        app = this
    }

    override fun onCreate() {
        super.onCreate()
        Settings.init(this)
        DynamicColors.applyToActivitiesIfAvailable(this)
    }
}