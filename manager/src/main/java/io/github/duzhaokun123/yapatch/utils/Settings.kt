package io.github.duzhaokun123.yapatch.utils

import android.content.Context
import android.content.SharedPreferences

object Settings {
    lateinit var sharedPreferences: SharedPreferences
    fun init(context: Context) {
        sharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    }

//    var uid
//        get() = sharedPreferences.getString("uid", null)
//        set(value) {
//            sharedPreferences.edit().putString("uid", value).apply()
//        }
}