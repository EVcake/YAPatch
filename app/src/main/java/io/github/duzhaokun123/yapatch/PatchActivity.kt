package io.github.duzhaokun123.yapatch

import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import io.github.duzhaokun123.test_board.bases.BaseActivity
import io.github.duzhaokun123.yapatch.databinding.ActivityPatchBinding
import io.github.duzhaokun123.yapatch.patch.PatchKt
import io.github.duzhaokun123.yapatch.patch.utils.Logger
import io.github.duzhaokun123.yapatch.utils.runIO
import io.github.duzhaokun123.yapatch.utils.runMain
import java.io.File

class PatchActivity: BaseActivity<ActivityPatchBinding>(ActivityPatchBinding::class.java, Config.NO_BACK) {
    val saveFileLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.android.package-archive")) { uri ->
        uri ?: return@registerForActivityResult
        File(cacheDir, "app_yapatched.apk").inputStream().use {
            contentResolver.openOutputStream(uri)?.use { output ->
                it.copyTo(output)
            }
        }
        finish()
    }

    val commandLine by lazy { startIntent.getStringArrayExtra("commandLine") }

    val logger = object : Logger {
        override fun info(message: String) {
            runMain {
                val textView = TextView(this@PatchActivity)
                textView.text = message
                textView.setTextColor(Color.WHITE)
                textView.typeface = Typeface.MONOSPACE
                addView(textView)
            }
        }

        override fun warn(message: String) {
            runMain {
                val textView = TextView(this@PatchActivity)
                textView.text = message
                textView.setTextColor(Color.YELLOW)
                textView.typeface = Typeface.MONOSPACE
                addView(textView)
            }
        }

        override fun error(message: String) {
            runMain {
                val textView = TextView(this@PatchActivity)
                textView.text = message
                textView.setTextColor(Color.RED)
                textView.typeface = Typeface.MONOSPACE
                addView(textView)
            }
        }

        fun addView(textView: TextView) {
            baseBinding.llLog.addView(textView)
            baseBinding.svLog.post {
                baseBinding.svLog.fullScroll(View.FOCUS_DOWN)
            }
        }
    }

    override fun initEvents() {
        baseBinding.btnSave.setOnClickListener {
            saveFileLauncher.launch("app_yapatched.apk")
        }
    }

    override fun initData() {
        runIO {
            var ok = false
            try {
                PatchKt(logger, *commandLine!!).run()
                ok = true
            } catch (e: Exception) {
                logger.error(e.stackTraceToString())
                e.printStackTrace()
            }
            runMain {
                baseBinding.piProgress.visibility = View.GONE
                baseBinding.btnSave.isEnabled = ok
                baseBinding.svLog.fullScroll(View.FOCUS_DOWN)
            }
        }
    }

    override fun onApplyWindowInsetsCompat(insets: WindowInsetsCompat) {
        super.onApplyWindowInsetsCompat(insets)
        baseBinding.llButtons.updateLayoutParams<LinearLayout.LayoutParams> {
            bottomMargin = insets.systemGestureInsets.bottom
        }
    }
}