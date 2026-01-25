package app.luma.helper

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class PackageRemovedReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != Intent.ACTION_PACKAGE_REMOVED) return

        // Skip if this is an app update (not a removal)
        if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return

        val packageName = intent.data?.schemeSpecificPart ?: return

        HomeCleanupHelper.cleanupRemovedPackage(context, packageName)
    }
}
