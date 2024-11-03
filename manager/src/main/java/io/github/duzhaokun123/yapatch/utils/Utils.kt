package io.github.duzhaokun123.yapatch.utils

import android.content.res.Resources
import android.util.TypedValue
import android.widget.Toast
import androidx.annotation.AttrRes
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import io.github.duzhaokun123.yapatch.app
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.random.Random

val random = Random(System.currentTimeMillis())

@Deprecated("Use TipUtil.showToast instead")
fun toast(msg: String) {
    Toast.makeText(app, msg, Toast.LENGTH_SHORT).show()
}

val WindowInsetsCompat.maxSystemBarsDisplayCutout
    get() = getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())

val WindowInsetsCompat.maxSystemBarsDisplayCutoutIme
    get() = getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout() or WindowInsetsCompat.Type.ime())

fun RecyclerView.resetAdapter() {
    this.adapter = adapter
}

inline fun <T> Result<T>.onException(action: (exception: Exception) -> Unit): Result<T> =
    this.onFailure { t ->
        if (t is Error) throw t
        action(t as Exception)
    }

fun runMain(block: suspend CoroutineScope.() -> Unit) =
    GlobalScope.launch(Dispatchers.Main, block = block)

fun runIO(block: suspend CoroutineScope.() -> Unit) =
    GlobalScope.launch(Dispatchers.IO, block = block)

fun Resources.Theme.getAttr(@AttrRes id: Int) =
    TypedValue().apply { resolveAttribute(id, this, true) }