package app.luma.helper

import android.content.Context
import app.luma.data.AppModel
import app.luma.data.Constants
import app.luma.data.GestureType
import app.luma.data.HomeLayout
import app.luma.data.Prefs
import app.luma.data.ShortcutEntry

object HomeCleanupHelper {
    private var onHomeCleanupCallback: (() -> Unit)? = null
    private var onAppListCleanupCallback: (() -> Unit)? = null

    fun setOnHomeCleanupCallback(callback: (() -> Unit)?) {
        onHomeCleanupCallback = callback
    }

    fun setOnAppListCleanupCallback(callback: (() -> Unit)?) {
        onAppListCleanupCallback = callback
    }

    fun cleanupRemovedPackage(
        context: Context,
        packageName: String,
    ) {
        val prefs = Prefs.getInstance(context)
        var needsHomeRefresh = false
        var needsAppListRefresh = false

        for (i in 0 until HomeLayout.TOTAL_SLOTS) {
            val appModel = prefs.getHomeAppModel(i)
            if (shouldClear(appModel, packageName)) {
                prefs.setHomeAppModel(i, emptyAppModel())
                needsHomeRefresh = true
            }
        }

        for (gestureType in GestureType.entries) {
            val appModel = prefs.getGestureApp(gestureType)
            if (shouldClear(appModel, packageName)) {
                prefs.setGestureApp(gestureType, emptyAppModel())
                needsHomeRefresh = true
            }
        }

        val hiddenApps = prefs.hiddenApps
        if (hiddenApps.contains(packageName)) {
            prefs.hiddenApps = hiddenApps.filterNot { it == packageName }.toMutableSet()
            needsAppListRefresh = true
        }

        val pinnedShortcuts = prefs.pinnedShortcuts
        val filteredShortcuts =
            pinnedShortcuts
                .filterNot { entry ->
                    ShortcutEntry.parse(entry)?.packageName == packageName
                }.toSet()
        if (filteredShortcuts.size != pinnedShortcuts.size) {
            prefs.pinnedShortcuts = filteredShortcuts
            needsAppListRefresh = true
        }

        val hiddenShortcutIds = prefs.hiddenShortcutIds
        val filteredHiddenIds =
            hiddenShortcutIds
                .filterNot { it.startsWith("$packageName|") }
                .toSet()
        if (filteredHiddenIds.size != hiddenShortcutIds.size) {
            prefs.hiddenShortcutIds = filteredHiddenIds
        }

        if (needsHomeRefresh) {
            onHomeCleanupCallback?.invoke()
        }
        if (needsAppListRefresh) {
            onAppListCleanupCallback?.invoke()
        }
    }

    private fun shouldClear(
        appModel: AppModel,
        packageName: String,
    ): Boolean {
        if (appModel.appLabel.isEmpty()) return false
        if (appModel.appPackage == packageName) return true

        if (appModel.appPackage == Constants.PINNED_SHORTCUT_PACKAGE) {
            val activityName = appModel.appActivityName
            if (activityName.contains("|")) {
                val shortcutPackage = activityName.substringBefore("|")
                if (shortcutPackage == packageName) return true
            }
        }

        return false
    }

    private fun emptyAppModel(): AppModel =
        AppModel(
            appLabel = "",
            appPackage = "",
            appAlias = "",
            appActivityName = "",
            user = android.os.Process.myUserHandle(),
            key = null,
        )
}
