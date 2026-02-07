package app.luma

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.os.Build
import android.os.UserHandle
import app.luma.helper.HomeCleanupHelper
import app.luma.helper.PackageRemovedReceiver

class LumaApplication : Application() {
    private val packageRemovedReceiver = PackageRemovedReceiver()

    private val launcherAppsCallback =
        object : LauncherApps.Callback() {
            override fun onPackageRemoved(
                packageName: String,
                user: UserHandle,
            ) {
                HomeCleanupHelper.cleanupRemovedPackage(this@LumaApplication, packageName, user)
            }

            override fun onPackageAdded(
                packageName: String,
                user: UserHandle,
            ) {}

            override fun onPackageChanged(
                packageName: String,
                user: UserHandle,
            ) {}

            override fun onPackagesAvailable(
                packageNames: Array<String>,
                user: UserHandle,
                replacing: Boolean,
            ) {}

            override fun onPackagesUnavailable(
                packageNames: Array<String>,
                user: UserHandle,
                replacing: Boolean,
            ) {}
        }

    override fun onCreate() {
        super.onCreate()
        registerPackageRemovedReceiver()
        registerLauncherAppsCallback()
    }

    private fun registerPackageRemovedReceiver() {
        val filter =
            IntentFilter(Intent.ACTION_PACKAGE_REMOVED).apply {
                addDataScheme("package")
            }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(packageRemovedReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            registerReceiver(packageRemovedReceiver, filter)
        }
    }

    private fun registerLauncherAppsCallback() {
        val launcherApps = getSystemService(LauncherApps::class.java)
        launcherApps.registerCallback(launcherAppsCallback)
    }
}
