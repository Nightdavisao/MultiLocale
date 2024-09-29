package io.nightdavisao.multilocale

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.topjohnwu.superuser.Shell
import io.nightdavisao.multilocale.databinding.ActivityPermissionCheckBinding
import io.nightdavisao.multilocale.utils.LocaleUtils
import rikka.shizuku.Shizuku
import rikka.shizuku.shared.BuildConfig


class PermissionCheckActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPermissionCheckBinding
    companion object {
        private const val TAG = "PermissionCheckActivity"
        private const val SHIZUKU_CODE = 69
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyIfAvailable(this)
        super.onCreate(savedInstanceState)
        binding = ActivityPermissionCheckBinding.inflate(layoutInflater)
        binding.grantOptionRadioGroup.setOnCheckedChangeListener { radioGroup, i ->
            binding.grantSecurePermission.isEnabled = true
        }

        binding.grantSecurePermission.setOnClickListener {
            // get the selected radio button
            val selectedRadioButton = binding.grantOptionRadioGroup.checkedRadioButtonId

            when (selectedRadioButton) {
                R.id.radio_shizuku -> {
                    if (Shizuku.pingBinder()) {
                        if (checkShizukuPermission(SHIZUKU_CODE)) {
                            LocaleUtils.grantConfigurationPermissionShizuku(this)
                        }
                    } else {
                        Toast.makeText(this, "Shizuku is not running or isn't installed.", Toast.LENGTH_SHORT).show()
                    }
                }
                R.id.radio_root -> {
                    // there's no programmatic way to check if root is available, so we just try to run a command and hope for the best
                    if (!LocaleUtils.grantConfigurationPermissionRoot(this)) {
                        Toast.makeText(this, "Failed to grant permission with root", Toast.LENGTH_LONG).show()
                    }
                }
                R.id.radio_adb -> showADBDialog()
            }
            checkPermissions()
        }
        checkPermissions()
        setContentView(binding.root)
        Shizuku.addRequestPermissionResultListener(this::onRequestPermissionResult)
    }

    override fun onDestroy() {
        Shizuku.removeRequestPermissionResultListener(this::onRequestPermissionResult)
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    private fun checkPermissions() {
        // check if the user has granted the permission(s)
        if (LocaleUtils.isChangeConfigurationPermissionGranted(this)) {
            showWriteSettingsDialog()
        } else {
            binding.grantOptionRadioGroup.clearCheck()
            binding.grantSecurePermission.isEnabled = false
        }
        if (LocaleUtils.areAllPermissionsGranted(this)) {
            startLocaleSettingsActivity()
        }
    }

    private fun startLocaleSettingsActivity() {
        val intent = Intent(this, LocaleSettingsActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }

    private fun showADBDialog() {
        val adbCommand = "adb shell pm grant $packageName android.permission.CHANGE_CONFIGURATION"
        val message = "You must grant the CHANGE_CONFIGURATION permission to this app. " +
                "Please run the following command in an ADB shell:<br><br>" +
                "<b>$adbCommand</b>" +
                "<br><br>Clicking \"OK\" will copy this command to your clipboard.<br>" +
                "Close this dialog and grant the permission manually, after you have done so, restart the app."
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Permission required")
            .setMessage(HtmlCompat.fromHtml(message, HtmlCompat.FROM_HTML_MODE_LEGACY))
            .setPositiveButton("OK") { _, _ ->
                val clipboardManager = applicationContext.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
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
    }

    private fun showWriteSettingsDialog() {
        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Permission required")
            .setMessage(
                "Now you must grant the WRITE_SETTINGS permission to this app. " +
                        "Since we can't do this ourselves, you will be taken to the settings page to grant it."
                )
            .setPositiveButton("OK") { _, _ ->
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q) {
                    intent.data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
            .create()
        dialog.setCancelable(false)
        dialog.show()
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

    private fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
        val granted = grantResult == PERMISSION_GRANTED
        if (requestCode == SHIZUKU_CODE) {
            if (granted) {
                Log.i(TAG, "Shizuku permission granted")
                LocaleUtils.grantConfigurationPermissionShizuku(this)
            } else {
                Log.e(TAG, "Shizuku permission denied")
            }
        }
    }
}