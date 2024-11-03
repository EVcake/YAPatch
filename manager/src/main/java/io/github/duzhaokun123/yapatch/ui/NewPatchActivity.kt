package io.github.duzhaokun123.yapatch.ui

import android.R
import android.content.Context
import android.content.Intent
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RelativeLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.duzhaokun123.yapatch.bases.BaseActivity
import io.github.duzhaokun123.yapatch.bases.BaseSimpleAdapter
import io.github.duzhaokun123.yapatch.databinding.ActivityNewPatchBinding
import io.github.duzhaokun123.yapatch.databinding.ItemAddModuleBinding
import java.io.File

class NewPatchActivity: BaseActivity<ActivityNewPatchBinding>(ActivityNewPatchBinding::class.java) {
    val openFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@registerForActivityResult
        contentResolver.openInputStream(uri)?.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        baseBinding.tvFileName.text = uri.path
    }
    val selectModuleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        result.data?.getStringExtra("module")?.let {
            modules.add(it)
            baseBinding.rvModules.adapter?.notifyItemInserted(modules.size - 1)
        }
    }

    val tempFile by lazy { File(cacheDir, "app.apk").also { it.delete() } }

    val modules = mutableListOf<String>()

    override fun initViews() {
        baseBinding.rvModules.adapter = ModuleAdapter(this, modules)
        baseBinding.rvModules.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
    }

    override fun initEvents() {
        baseBinding.btnOpenFile.setOnClickListener {
            openFileLauncher.launch("application/vnd.android.package-archive")
        }
        baseBinding.btnAddModule.setOnClickListener {
            selectModuleLauncher.launch(Intent(this, SelectModuleActivity::class.java))
        }
        baseBinding.btnPatch.setOnClickListener {
            val commandLine = mutableListOf<String>()
            commandLine.add("-o")
            commandLine.add(cacheDir.absolutePath)
            modules.forEach {
                commandLine.add("-m")
                commandLine.add(it)
            }
            commandLine.add("-l")
            commandLine.add(baseBinding.spSignatureBypass.selectedItemPosition.toString())
            commandLine.add(tempFile.absolutePath)
            startActivity(Intent(this, PatchActivity::class.java).apply {
                putExtra("commandLine", commandLine.toTypedArray())
            })
            finish()
        }
    }

    override fun onApplyWindowInsetsCompat(insets: WindowInsetsCompat) {
        super.onApplyWindowInsetsCompat(insets)
        baseBinding.btnPatch.updateLayoutParams<RelativeLayout.LayoutParams> {
            bottomMargin = insets.systemGestureInsets.bottom
        }
    }

    class ModuleAdapter(context: Context, val modules: MutableList<String>): BaseSimpleAdapter<ItemAddModuleBinding>(context,
        ItemAddModuleBinding::class.java
    ) {
        override fun initData(baseBinding: ItemAddModuleBinding, position: Int) {
            val module = modules[position]
            val moduleInfo = context.packageManager.getApplicationInfo(module, 0)
            baseBinding.tvName.text = moduleInfo.loadLabel(context.packageManager)
            baseBinding.ivIcon.setImageDrawable(moduleInfo.loadIcon(context.packageManager))
        }

        override fun initEvents(baseBinding: ItemAddModuleBinding, position: Int) {
            baseBinding.ibRemove.setOnClickListener {
                modules.removeAt(position)
                notifyItemRemoved(position)
            }
        }

        override fun getItemCount() = modules.size
        
        override fun onCreateRootLayoutParam() =
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}