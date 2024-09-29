package io.nightdavisao.multilocale

import android.Manifest
import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.LocaleList
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.text.HtmlCompat
import androidx.core.view.WindowCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import io.nightdavisao.multilocale.databinding.ActivityLocaleSettingsBinding
import io.nightdavisao.multilocale.utils.LocaleUtils
import rikka.shizuku.Shizuku
import java.util.Collections
import java.util.Locale


class LocaleSettingsActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "LocaleSettingsActivity"
    }

    private val localeRecyclerView: RecyclerView
        get() = binding.contentMain.localeRecyclerView

    private val localeRecyclerViewAdapter: LocaleRecyclerAdapter
        get() = localeRecyclerView.adapter as LocaleRecyclerAdapter

    private var userHasChangedLocales =
        false // used to show the snackbar only once if the user changes the settings

    private val dragCallback = object : ItemTouchHelper.SimpleCallback(
        ItemTouchHelper.UP or ItemTouchHelper.DOWN,
        0
    ) {
        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val adapter = localeRecyclerViewAdapter
            val from = viewHolder.adapterPosition
            val to = target.adapterPosition
            Collections.swap(adapter.localeList, from, to)
            adapter.notifyItemMoved(from, to)
            makeSnackSaveConfiguration()
            return true
        }

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            // do nothing
        }
    }

    private val itemTouchHelper = ItemTouchHelper(dragCallback)

    private lateinit var binding: ActivityLocaleSettingsBinding

    private fun addLocale(locale: Locale) {
        val adapter = localeRecyclerViewAdapter
        adapter.localeList.add(locale)
        adapter.notifyItemInserted(adapter.localeList.size - 1)
        makeSnackSaveConfiguration()
    }

    private fun makeSnackSaveConfiguration() {
        if (userHasChangedLocales) return
        Snackbar.make(
            binding.root,
            "Do you want to save this locale configuration?",
            Snackbar.LENGTH_INDEFINITE
        )
            .setAction("Save") {
                saveLocaleConfiguration()
            }
            .show()
        userHasChangedLocales = true
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyIfAvailable(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityLocaleSettingsBinding.inflate(layoutInflater)
        localeRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@LocaleSettingsActivity)
            // extract locales from the configuration and add them to a list
            val locales = mutableListOf<Locale>()
            for (i in 0 until resources.configuration.locales.size()) {
                locales.add(resources.configuration.locales[i])
            }

            adapter = LocaleRecyclerAdapter(localeRecyclerView, locales, false)
        }
        localeRecyclerView.addItemDecoration(
            DividerItemDecoration(
                this,
                DividerItemDecoration.VERTICAL
            )
        )
        // drag and drop to reorder locales
        itemTouchHelper.attachToRecyclerView(localeRecyclerView)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.baseline_close_24)

        binding.fab.setOnClickListener {
            if (localeRecyclerViewAdapter.deleteMode) {
                // if delete mode is enabled, delete all checked locales
                val adapter = localeRecyclerViewAdapter
                val localesToDelete = mutableListOf<Locale>()
                for (i in 0 until adapter.localeList.size) {
                    val holder =
                        localeRecyclerView.findViewHolderForAdapterPosition(i) as LocaleRecyclerAdapter.ViewHolder
                    if (holder.deleteCheckBox.isChecked) {
                        localesToDelete.add(adapter.localeList[i])
                    }
                }
                // if ALL of the locales were selected, tell the user they can't delete all of them
                if (localesToDelete.size == adapter.localeList.size) {
                    Toast.makeText(
                        this,
                        "You can't delete all of the locales!",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }
                adapter.localeList.removeAll(localesToDelete)
                adapter.notifyDataSetChanged()
                toggleDeleteMode()
                makeSnackSaveConfiguration()
            } else {
                val dialog = LanguageDialogFragment { locale ->
                    addLocale(locale)
                }
                dialog.show(supportFragmentManager, "LanguageDialogFragment")
            }
        }

        // register callback for back button presses
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (localeRecyclerViewAdapter.deleteMode) {
                    toggleDeleteMode()
                } else {
                    finish()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    private fun toggleDeleteMode() {
        val adapter = localeRecyclerViewAdapter
        adapter.deleteMode = !adapter.deleteMode
        adapter.notifyDataSetChanged()
        // hide and show stuff depending on if delete mode is enabled
        binding.fab.setImageResource(if (adapter.deleteMode) R.drawable.baseline_done_24 else R.drawable.baseline_add_24)
        binding.toolbar.menu.findItem(R.id.action_save_locale_config).isVisible =
            !adapter.deleteMode
        binding.toolbar.menu.findItem(R.id.action_delete_locale).isVisible = !adapter.deleteMode
        // change the title of the toolbar depending on if delete mode is enabled
        binding.toolbar.title =
            if (adapter.deleteMode) "Select locales to delete" else applicationInfo.loadLabel(
                packageManager
            )
        // show an X button in the toolbar if delete mode is enabled
        supportActionBar?.setDisplayHomeAsUpEnabled(adapter.deleteMode)
        // toggle drag and drop
        itemTouchHelper.attachToRecyclerView(if (adapter.deleteMode) null else localeRecyclerView)
        // uncheck all checkboxes when delete mode is disabled
        if (!adapter.deleteMode) {
            for (i in 0 until adapter.localeList.size) {
                val holder =
                    localeRecyclerView.findViewHolderForAdapterPosition(i)
                if (holder is LocaleRecyclerAdapter.ViewHolder) holder.deleteCheckBox.isChecked = false
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        toggleDeleteMode()
        return true
    }

    private fun saveLocaleConfiguration() {
        if (LocaleUtils.areAllPermissionsGranted(this)) {
            val adapter = localeRecyclerViewAdapter
            val locales = adapter.localeList
            val localeList = LocaleList(*locales.toTypedArray())
            // set the application locale list first
            Log.d(TAG, "Setting locale list for the application: $localeList")
            resources.configuration.setLocales(localeList)
            resources.updateConfiguration(resources.configuration, resources.displayMetrics)
            Log.d(TAG, "Saving locale list: $localeList")
            try {
                LocaleUtils.setLocaleList(localeList)
                Snackbar.make(binding.root, "Locale list saved", Snackbar.LENGTH_LONG).show()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save locale list", e)
                Snackbar.make(binding.root, "Failed to save locale list", Snackbar.LENGTH_LONG)
                    .show()
            }
            userHasChangedLocales = false
        } else {
            Toast.makeText(this, "Permissions are not granted (⊙_⊙)？", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_save_locale_config -> {
                saveLocaleConfiguration()
                true
            }

            R.id.action_delete_locale -> {
                toggleDeleteMode()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }
}