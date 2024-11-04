package io.github.duzhaokun123.yapatch.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import io.github.duzhaokun123.yapatch.R
import io.github.duzhaokun123.yapatch.bases.BaseActivity
import io.github.duzhaokun123.yapatch.databinding.ActivityPatchBinding
import io.github.duzhaokun123.yapatch.patch.PatchKt
import io.github.duzhaokun123.yapatch.patch.utils.Logger
import io.github.duzhaokun123.yapatch.utils.runIO
import io.github.duzhaokun123.yapatch.utils.runMain
import io.github.duzhaokun123.yapatch.utils.runNewThread
import java.io.File

class PatchActivity: BaseActivity<ActivityPatchBinding>(ActivityPatchBinding::class.java, Config.NO_BACK) {
    val saveFileLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/vnd.android.package-archive")) { uri ->
        uri ?: return@registerForActivityResult
        File(cacheDir, "app_yapatched.apk").inputStream().use {
            contentResolver.openOutputStream(uri)?.use { output ->
                it.copyTo(output)
            }
        }
        logger.info("Saved to $uri")
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

        override fun onProgress(progress: Int, total: Int) {
            runMain {
                baseBinding.piProgress.isIndeterminate = total == 0
                baseBinding.piProgress.max = total
                baseBinding.piProgress.progress = progress
            }
        }
    }

    override fun initEvents() {
        baseBinding.btnSave.setOnClickListener {
            saveFileLauncher.launch("app_yapatched.apk")
        }
        baseBinding.btnInstall.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(
                    FileProvider.getUriForFile(this@PatchActivity, "$packageName.provider", File(cacheDir, "app_yapatched.apk")).also {
                        logger.info("Install uri: $it")
                    },
                    "application/vnd.android.package-archive"
                )
                setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            })
        }
    }

    override fun initData() {
        runNewThread {
            var ok = false
            try {
                PatchKt(logger, *commandLine!!).run()
                ok = true
            } catch (e: Exception) {
                logger.error(e.stackTraceToString())
            }
            runMain {
                baseBinding.piProgress.visibility = View.GONE
                baseBinding.btnSave.isEnabled = ok
                baseBinding.btnInstall.isEnabled = ok
                baseBinding.svLog.fullScroll(View.FOCUS_DOWN)
                supportActionBar?.apply {
                    setDisplayHomeAsUpEnabled(true)
                    setDisplayShowHomeEnabled(true)
                    setHomeAsUpIndicator(R.drawable.ic_arrow_back_24)
                }
            }
        }
    }

    override fun onApplyWindowInsetsCompat(insets: WindowInsetsCompat) {
        super.onApplyWindowInsetsCompat(insets)
        baseBinding.llButtons.updatePadding(bottom = insets.systemGestureInsets.bottom)
    }
}