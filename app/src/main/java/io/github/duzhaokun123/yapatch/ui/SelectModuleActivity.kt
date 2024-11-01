package io.github.duzhaokun123.yapatch.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import io.github.duzhaokun123.yapatch.bases.BaseActivity
import io.github.duzhaokun123.yapatch.bases.BaseSimpleAdapter
import io.github.duzhaokun123.yapatch.databinding.ActivitySelectModuleBinding
import io.github.duzhaokun123.yapatch.databinding.ItemAppBinding

class SelectModuleActivity :
    BaseActivity<ActivitySelectModuleBinding>(ActivitySelectModuleBinding::class.java) {
    val modulesLst = mutableListOf<ApplicationInfo>()

    override fun initViews() {
        baseBinding.rvModules.adapter = ModulesAdapter(this, modulesLst)
        baseBinding.rvModules.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
    }

    override fun initData() {
        refreshAppList()
    }

     private fun refreshAppList() {
        val pm = packageManager
        modulesLst.clear()
        pm.getInstalledApplications(PackageManager.GET_META_DATA).forEach { applicationInfo ->
            if ((applicationInfo.metaData?.getInt("xposedminversion", -1) ?: -1) != -1) {
                modulesLst.add(applicationInfo)
            }
        }
        baseBinding.rvModules.adapter?.notifyDataSetChanged()
    }

    class ModulesAdapter(val activity: Activity, val modulesList: List<ApplicationInfo>) :
        BaseSimpleAdapter<ItemAppBinding>(activity, ItemAppBinding::class.java) {
        override fun initData(baseBinding: ItemAppBinding, position: Int) {
            val module = modulesList[position]
            baseBinding.ivIcon.setImageDrawable(module.loadIcon(context.packageManager))
            baseBinding.tvName.text = module.loadLabel(context.packageManager)
        }

        override fun initEvents(baseBinding: ItemAppBinding, position: Int) {
            val module = modulesList[position]
            baseBinding.root.setOnClickListener {
                activity.setResult(RESULT_OK, Intent().putExtra("module", module.packageName))
                activity.finish()
            }
        }

        override fun getItemCount() = modulesList.size

        override fun onCreateRootLayoutParam() =
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
    }
}