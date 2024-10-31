package io.github.duzhaokun123.yapatch

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.github.duzhaokun123.test_board.bases.BaseActivity
import io.github.duzhaokun123.yapatch.databinding.ActivityMainBinding
import io.github.duzhaokun123.yapatch.patch.Versions

class MainActivity: BaseActivity<ActivityMainBinding>(ActivityMainBinding::class.java, Config.NO_BACK) {
    @SuppressLint("SetTextI18n")
    override fun initViews() {
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.add -> {
                startActivity(Intent(this, NewPatchActivity::class.java))
                true
            }
            R.id.about -> {
                MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.about)
                    .setMessage(
                        """
                            |App Version: ${BuildConfig.VERSION_NAME}(${BuildConfig.VERSION_CODE})
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
}