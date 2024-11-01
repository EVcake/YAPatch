package io.github.duzhaokun123.yapatch

import android.annotation.SuppressLint
import com.google.android.material.color.DynamicColors
import com.google.gson.Gson
import io.github.duzhaokun123.yapatch.utils.Settings


@SuppressLint("StaticFieldLeak")
lateinit var app: Application

val gson by lazy { Gson() }

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