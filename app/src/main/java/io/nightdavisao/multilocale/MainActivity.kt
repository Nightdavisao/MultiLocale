package io.nightdavisao.multilocale

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
import io.nightdavisao.multilocale.databinding.ActivityMainBinding
import io.nightdavisao.multilocale.utils.LocaleUtils
import rikka.shizuku.Shizuku
import java.util.Collections
import java.util.Locale


class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val SHIZUKU_CODE = 69
    }

    private val localeRecyclerView: RecyclerView
        get() = binding.contentMain.localeRecyclerView

    private val localeRecyclerViewAdapter: LocaleRecyclerAdapter
        get() = localeRecyclerView.adapter as LocaleRecyclerAdapter

    private var userHasChangedSettings =
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

    private lateinit var binding: ActivityMainBinding

    private fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
        val granted = grantResult == PERMISSION_GRANTED
        if (requestCode == SHIZUKU_CODE) {
            if (granted) {
                Log.i(TAG, "Shizuku permission granted")
                LocaleUtils.grantConfigurationPermissionShizuku(this)
                recreate()
            } else {
                Log.e(TAG, "Shizuku permission denied")
            }
        }
    }

    private fun addLocale(locale: Locale) {
        val adapter = localeRecyclerViewAdapter
        adapter.localeList.add(locale)
        adapter.notifyItemInserted(adapter.localeList.size - 1)
        makeSnackSaveConfiguration()
    }

    private fun makeSnackSaveConfiguration() {
        if (userHasChangedSettings) return
        Snackbar.make(
            binding.root,
            "Do you want to save this locale configuration?",
            Snackbar.LENGTH_INDEFINITE
        )
            .setAction("Save") {
                saveLocaleConfiguration()
            }
            .show()
        userHasChangedSettings = true
    }

    private fun showGenericDialog(title: String, message: String, callback: (() -> Unit)? = null) {
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { _, _ ->
                callback?.invoke()
            }
            .create()
        dialog.setCancelable(false)
        dialog.show()
    }

    private fun showPermissionDialog() {
        if (!hasChangeConfigurationPermission()) {
            // if the user has shizuku installed, use that to grant the permission
            if (Shizuku.pingBinder()) {
                // tell the user we're going to use shizuku to grant the permission
                showGenericDialog(
                    "Shizuku permission required",
                    "You must grant the CHANGE_CONFIGURATION permission to this app. " +
                            "Please grant the permission to access Shizuku so we can grant this permission to this app.",
                ) {
                    checkShizukuPermission(SHIZUKU_CODE)
                }
                return
            }
            val adbCommand = "adb shell pm grant $packageName android.permission.CHANGE_CONFIGURATION"
            val message = "You must grant the CHANGE_CONFIGURATION permission to this app. " +
                    "Please run the following command in an ADB shell:<br><br>" +
                    "<b>$adbCommand</b>" +
                    "<br><br>Clicking \"OK\" will copy this command to your clipboard."
            val dialog = MaterialAlertDialogBuilder(this)
                .setTitle("Permission required")
                .setMessage(HtmlCompat.fromHtml(message, HtmlCompat.FROM_HTML_MODE_LEGACY))
                .setPositiveButton("OK") { _, _ ->
                    val clipboardManager = applicationContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clipData = ClipData.newPlainText("ADB command", adbCommand)
                    clipboardManager.setPrimaryClip(clipData)
                    Toast.makeText(this, "Please grant the permission", Toast.LENGTH_LONG).show()
                }
                .setNeutralButton("What is ADB?") { _, _ ->
                    // TODO: make an actual documentation page that tells the user what ADB is and why they need it
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.data = Uri.parse("https://developer.android.com/studio/command-line/adb")
                    startActivity(intent)
                }
                .create()
            dialog.setCancelable(false)
            dialog.show()
            return
        }
        if (!Settings.System.canWrite(this)) {
            showGenericDialog(
                "Permission required",
                "You must grant the WRITE_SETTINGS permission to this app. " +
                        "You will be taken to the settings page to grant the permission.",
            ) {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                    intent.data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
        }
    }

    private fun showPermissionDialogIfNecessary() {
        if (!hasAllPermissions()) {
            showPermissionDialog()
        }
    }

    private fun hasChangeConfigurationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            android.Manifest.permission.CHANGE_CONFIGURATION
        ) == PERMISSION_GRANTED
    }

    private fun hasAllPermissions(): Boolean {
        return hasChangeConfigurationPermission() && Settings.System.canWrite(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyIfAvailable(this)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        Shizuku.addRequestPermissionResultListener(this::onRequestPermissionResult)

        binding = ActivityMainBinding.inflate(layoutInflater)
        localeRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
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

        showPermissionDialogIfNecessary()
    }

    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(this::onRequestPermissionResult)
        super.onDestroy()
    }

    private fun checkShizukuPermission(code: Int): Boolean {
        if (Shizuku.isPreV11()) {
            return false
        }
        return if (Shizuku.checkSelfPermission() == PERMISSION_GRANTED) {
            true
        } else if (Shizuku.shouldShowRequestPermissionRationale()) {
            Toast.makeText(this, "Please grant the permission", Toast.LENGTH_LONG).show()
            false
        } else {
            Shizuku.requestPermission(code)
            false
        }
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
        if (hasAllPermissions()) {
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
            userHasChangedSettings = false
        } else {
            showPermissionDialog()
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