package app.luma.helper

import android.annotation.SuppressLint
import android.app.Activity
import android.content.*
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.content.res.Resources
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.UserHandle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowManager
import android.widget.Toast
import app.luma.BuildConfig
import app.luma.data.AppModel
import app.luma.data.Constants
import app.luma.data.Prefs
import app.luma.data.ShortcutEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.Collator

private const val TAG = "Utils"

fun performHapticFeedback(context: Context) {
    try {
        val vibrator =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager =
                    context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vibratorManager.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
        vibrator.vibrate(VibrationEffect.createOneShot(42, VibrationEffect.DEFAULT_AMPLITUDE))
    } catch (e: Exception) {
        // Continue if haptic feedback fails
    }
}

fun showToast(
    context: Context,
    message: String,
    duration: Int = Toast.LENGTH_SHORT,
) {
    Toast
        .makeText(context.applicationContext, message, duration)
        .apply {
            setGravity(Gravity.CENTER, 0, 0)
        }.show()
}

suspend fun getAppsList(context: Context): MutableList<AppModel> =
    withContext(Dispatchers.IO) {
        val appList: MutableList<AppModel> = mutableListOf()

        try {
            val prefs = Prefs.getInstance(context)
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
            val collator = Collator.getInstance()
            val userHandle = android.os.Process.myUserHandle()

            for (app in launcherApps.getActivityList(null, userHandle)) {
                if (app.applicationInfo.packageName == BuildConfig.APPLICATION_ID) {
                    continue
                }

                val appAlias =
                    prefs.getAppAlias(app.applicationInfo.packageName).ifEmpty {
                        prefs.getAppAlias(app.label.toString())
                    }

                val appModel =
                    AppModel(
                        app.label.toString(),
                        collator.getCollationKey(app.label.toString()),
                        app.applicationInfo.packageName,
                        app.componentName.className,
                        userHandle,
                        appAlias,
                        false,
                    )

                appList.add(appModel)
            }

            val hiddenShortcutIds = prefs.hiddenShortcutIds

            for (entry in prefs.pinnedShortcuts) {
                val shortcut = ShortcutEntry.parse(entry) ?: continue

                if (hiddenShortcutIds.contains(shortcut.payload)) continue

                val shortcutModel =
                    AppModel(
                        appLabel = shortcut.label,
                        key = collator.getCollationKey(shortcut.label),
                        appPackage = Constants.PINNED_SHORTCUT_PACKAGE,
                        appActivityName = shortcut.payload,
                        user = userHandle,
                        appAlias = "",
                        hasNotification = false,
                    )
                appList.add(shortcutModel)
            }

            appList.sortBy {
                if (it.appAlias.isEmpty()) {
                    it.appLabel.lowercase()
                } else {
                    it.appAlias.lowercase()
                }
            }

            val packagesWithNotifications = LumaNotificationListener.getActiveNotificationPackages()
            appList.forEach { appModel ->
                appModel.hasNotification = packagesWithNotifications.contains(appModel.appPackage)
            }
        } catch (e: java.lang.Exception) {
            if (BuildConfig.DEBUG) {
                Log.d("backup", "$e")
            }
        }
        appList
    }

suspend fun getHiddenAppsList(context: Context): MutableList<AppModel> =
    withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val prefs = Prefs.getInstance(context)
        val hiddenAppsSet = prefs.hiddenApps
        val hiddenShortcutIds = prefs.hiddenShortcutIds
        val appList: MutableList<AppModel> = mutableListOf()

        val collator = Collator.getInstance()
        val userHandle = android.os.Process.myUserHandle()

        for (appPackage in hiddenAppsSet) {
            try {
                val appInfo = pm.getApplicationInfo(appPackage, 0)
                val appName = pm.getApplicationLabel(appInfo).toString()
                val appKey = collator.getCollationKey(appName)
                appList.add(AppModel(appName, appKey, appPackage, "", userHandle, prefs.getAppAlias(appName), false))
            } catch (_: NameNotFoundException) {
                // App was uninstalled, skip silently
            }
        }

        for (entry in prefs.pinnedShortcuts) {
            val shortcut = ShortcutEntry.parse(entry) ?: continue

            if (!hiddenShortcutIds.contains(shortcut.payload)) continue

            val shortcutModel =
                AppModel(
                    appLabel = shortcut.label,
                    key = collator.getCollationKey(shortcut.label),
                    appPackage = Constants.PINNED_SHORTCUT_PACKAGE,
                    appActivityName = shortcut.payload,
                    user = userHandle,
                    appAlias = "",
                    hasNotification = false,
                )
            appList.add(shortcutModel)
        }

        appList.sort()
        appList
    }

fun getDefaultLauncherPackage(context: Context): String {
    val intent = Intent()
    intent.action = Intent.ACTION_MAIN
    intent.addCategory(Intent.CATEGORY_HOME)
    val packageManager = context.packageManager
    val result = packageManager.resolveActivity(intent, 0)
    return if (result?.activityInfo != null) {
        result.activityInfo.packageName
    } else {
        "android"
    }
}

fun openAppInfo(
    context: Context,
    userHandle: UserHandle,
    packageName: String,
) {
    val launcher = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
    val intent: Intent? = context.packageManager.getLaunchIntentForPackage(packageName)
    intent?.let {
        launcher.startAppDetailsActivity(intent.component, userHandle, null, null)
    } ?: showToast(context, "Unable to open app info")
}

fun initActionService(context: Context): ActionService? {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val actionService = ActionService.instance()
        if (actionService != null) {
            return actionService
        } else {
            openAccessibilitySettings(context)
        }
    } else {
        showToast(context, "This action requires Android P (9) or higher", Toast.LENGTH_LONG)
    }

    return null
}

fun openAccessibilitySettings(context: Context) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    val cs = ComponentName(context.packageName, ActionService::class.java.name).flattenToString()
    val bundle = Bundle()
    bundle.putString(":settings:fragment_args_key", cs)
    intent.apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra(":settings:fragment_args_key", cs)
        putExtra(":settings:show_fragment_args", bundle)
    }
    context.startActivity(intent)
}

fun hideStatusBar(activity: Activity) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        activity.window.insetsController?.hide(WindowInsets.Type.statusBars())
    } else {
        @Suppress("DEPRECATION")
        activity.window.decorView.apply {
            systemUiVisibility = View.SYSTEM_UI_FLAG_IMMERSIVE or View.SYSTEM_UI_FLAG_FULLSCREEN
        }
    }
}

fun uninstallApp(
    context: Context,
    appPackage: String,
) {
    val intent = Intent(Intent.ACTION_DELETE)
    intent.data = Uri.parse("package:$appPackage")
    context.startActivity(intent)
}

fun dp2px(
    resources: Resources,
    dp: Int,
): Int =
    TypedValue
        .applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics,
        ).toInt()

@Suppress("SpellCheckingInspection")
@SuppressLint("WrongConstant")
fun expandNotificationDrawer(context: Context) {
    // Source: https://stackoverflow.com/a/51132142
    try {
        val statusBarService = context.getSystemService("statusbar")
        val statusBarManager = Class.forName("android.app.StatusBarManager")
        val method = statusBarManager.getMethod("expandNotificationsPanel")
        method.invoke(statusBarService)
    } catch (e: Exception) {
        Log.e(TAG, "Error expanding notification drawer", e)
    }
}

@Suppress("SpellCheckingInspection")
@SuppressLint("WrongConstant")
fun expandQuickSettings(context: Context) {
    try {
        val statusBarService = context.getSystemService("statusbar")
        val statusBarManager = Class.forName("android.app.StatusBarManager")
        val method = statusBarManager.getMethod("expandSettingsPanel")
        method.invoke(statusBarService)
    } catch (e: Exception) {
        Log.e(TAG, "Error expanding quick settings", e)
    }
}
