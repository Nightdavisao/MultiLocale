package io.nightdavisao.multilocale.utils

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.IPackageManager
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import android.os.RemoteException
import android.permission.IPermissionManager
import androidx.annotation.RequiresApi
import org.lsposed.hiddenapibypass.HiddenApiBypass
import rikka.shizuku.ShizukuBinderWrapper
import rikka.shizuku.SystemServiceHelper

object LocaleUtils {
    private val iPackageManager by lazy {
        IPackageManager.Stub.asInterface(
            ShizukuBinderWrapper(SystemServiceHelper.getSystemService("package"))
        )
    }

    @delegate:RequiresApi(Build.VERSION_CODES.R)
    private val iPermissionManager by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            HiddenApiBypass.addHiddenApiExemptions(
                "Landroid/permission"
            )
        }
        IPermissionManager.Stub.asInterface(
            ShizukuBinderWrapper(SystemServiceHelper.getSystemService("permissionmgr"))
        )
    }
    private fun grantRuntimePermission(packageName: String, permissionName: String, userId: Int) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                iPermissionManager.grantRuntimePermission(packageName, permissionName, userId)
            } else {
                iPackageManager.grantRuntimePermission(packageName, permissionName, userId)
            }
        } catch (tr: RemoteException) {
            throw RuntimeException(tr.message, tr)
        }
    }

    fun grantConfigurationPermissionShizuku(context: Context) {
        val userHandle = android.os.Process.myUserHandle().hashCode()
        grantRuntimePermission(context.packageName, "android.permission.CHANGE_CONFIGURATION", userHandle)
    }

    fun setLocaleList(locales: LocaleList?): Boolean {
        @SuppressLint("PrivateApi") val activityManagerNative =
            Class.forName("android.app.ActivityManagerNative")

        // ActivityManagerNative.getDefault();
        val getDefault = activityManagerNative.getMethod("getDefault")
        val am = getDefault.invoke(activityManagerNative)

        // am.getConfiguration();
        val getConfiguration = am.javaClass.getMethod("getConfiguration")
        val config = getConfiguration.invoke(am) as Configuration
        config.setLocales(locales)
        val field = config.javaClass.getField("userSetLocale")
        field[config] = true

        // am.updateConfiguration(config);
        val updateConfiguration = am.javaClass.getMethod(
            "updatePersistentConfiguration", *arrayOf<Class<*>>(
                Configuration::class.java
            )
        )
        updateConfiguration.invoke(am, *arrayOf<Any>(config))
        return true
    }
}