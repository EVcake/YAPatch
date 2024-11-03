package io.github.duzhaokun123.yapatch.ui

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.duzhaokun123.yapatch.BuildConfig
import io.github.duzhaokun123.yapatch.R
import io.github.duzhaokun123.yapatch.bases.BaseActivity
import io.github.duzhaokun123.yapatch.bases.BaseSimpleAdapter
import io.github.duzhaokun123.yapatch.databinding.ActivityMainBinding
import io.github.duzhaokun123.yapatch.databinding.ItemAppBinding
import io.github.duzhaokun123.yapatch.gson
import io.github.duzhaokun123.yapatch.patch.Metadata
import io.github.duzhaokun123.yapatch.patch.Versions
import io.github.duzhaokun123.yapatch.utils.TipUtil

class MainActivity: BaseActivity<ActivityMainBinding>(ActivityMainBinding::class.java, Config.NO_BACK) {
    val patchedAppList = mutableListOf<ApplicationInfo>()
    val modulesLst = mutableListOf<ApplicationInfo>()

    override fun initViews() {
        baseBinding.rvPatched.adapter = PatchedAdapter(this, patchedAppList)
        baseBinding.rvPatched.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        baseBinding.rvModules.adapter = ModulesAdapter(this, modulesLst)
        baseBinding.rvModules.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
    }

    override fun initData() {
        refreshAppList()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.refresh -> {
                refreshAppList()
                true
            }
            R.id.add -> {
                startActivity(Intent(this, NewPatchActivity::class.java))
                true
            }
            R.id.about -> {
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.about)
                    .setMessage(
                        """
                            |Manager Version: ${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})
                            |YAPatch Version: ${Versions.yapatch}
                            |Pine Version: ${Versions.pine}
                            |PineXposed Version: ${Versions.pineXposed}
                        """.trimMargin()
                    )
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun refreshAppList() {
        val pm = packageManager
        patchedAppList.clear()
        modulesLst.clear()
        pm.getInstalledApplications(PackageManager.GET_META_DATA).forEach { applicationInfo ->
            if (applicationInfo.metaData?.getString("yapatch") != null) {
                patchedAppList.add(applicationInfo)
            }
            if ((applicationInfo.metaData?.getInt("xposedminversion", -1) ?: -1) != -1) {
                modulesLst.add(applicationInfo)
            }
        }
        baseBinding.rvPatched.adapter?.notifyDataSetChanged()
        baseBinding.rvModules.adapter?.notifyDataSetChanged()
    }

    class PatchedAdapter(context: Context, val patchedAppList: List<ApplicationInfo>) :
        BaseSimpleAdapter<ItemAppBinding>(context, ItemAppBinding::class.java) {

        override fun initViews(baseBinding: ItemAppBinding, position: Int) {
            val app = patchedAppList[position]
            val metaData = gson.fromJson(app.metaData.getString("yapatch"), Metadata::class.java)
            baseBinding.ivIcon.setImageDrawable(app.loadIcon(context.packageManager))
            baseBinding.tvName.text = app.loadLabel(context.packageManager)
            baseBinding.tvDescription.text = "loader: ${metaData.loader}"
        }

        override fun initEvents(baseBinding: ItemAppBinding, position: Int) {
            val app = patchedAppList[position]
            val metaData = gson.fromJson(app.metaData.getString("yapatch"), Metadata::class.java)
            baseBinding.root.setOnClickListener {
                MaterialAlertDialogBuilder(context)
                    .setTitle(app.loadLabel(context.packageManager))
                    .setIcon(app.loadIcon(context.packageManager))
                    .setMessage("""
                        |loader: ${metaData.loader}
                        |load modules:
                        ${metaData.modules.joinToString("\n") { "| - $it" }}
                    """.trimMargin())
                    .setPositiveButton(android.R.string.ok, null)
                    .show()
            }
        }

        override fun getItemCount() = patchedAppList.size

        override fun onCreateRootLayoutParam() =
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    class ModulesAdapter(context: Context, val modulesList: List<ApplicationInfo>) :
        BaseSimpleAdapter<ItemAppBinding>(context, ItemAppBinding::class.java) {

        override fun initViews(baseBinding: ItemAppBinding, position: Int) {
            val module = modulesList[position]
            baseBinding.ivIcon.setImageDrawable(module.loadIcon(context.packageManager))
            baseBinding.tvName.text = module.loadLabel(context.packageManager)
            baseBinding.tvDescription.text = "xposed: ${module.metaData?.getInt("xposedminversion", -1)}"
        }

        override fun initEvents(baseBinding: ItemAppBinding, position: Int) {
            val module = modulesList[position]
            baseBinding.root.setOnClickListener {
                var intent = Intent()
                intent.action = Intent.ACTION_MAIN
                intent.addCategory("de.robv.android.xposed.category.MODULE_SETTINGS")
                intent.setPackage(module.packageName)
                val ris = context.packageManager.queryIntentActivities(intent, 0)
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (ris.isNotEmpty()) {
                    intent.setClassName(ris[0].activityInfo.packageName, ris[0].activityInfo.name)
                }
                try {
                    context.startActivity(intent)
                } catch (ignored: Exception) {
                    TipUtil.showTip(context, "No module settings")
                }
            }
        }

        override fun getItemCount() = modulesList.size

        override fun onCreateRootLayoutParam() =
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}