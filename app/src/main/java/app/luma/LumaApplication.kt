package app.luma

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import app.luma.helper.PackageRemovedReceiver

class LumaApplication : Application() {
    private val packageRemovedReceiver = PackageRemovedReceiver()

    override fun onCreate() {
        super.onCreate()
        registerPackageRemovedReceiver()
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
}
