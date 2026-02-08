package app.luma.data

import android.content.Context
import android.content.SharedPreferences
import android.os.UserManager
import app.luma.style.FontSizeOption

private const val PREFS_FILENAME = "app.luma"

private const val FIRST_SETTINGS_OPEN = "FIRST_SETTINGS_OPEN"
private const val HOME_PAGES = "HOME_PAGES"
private const val HOME_APPS_PER_PAGE = "HOME_APPS_PER_PAGE_"

private const val HIDDEN_APPS = "HIDDEN_APPS"
private const val HIDDEN_SHORTCUT_IDS = "HIDDEN_SHORTCUT_IDS"
private const val INVERT_COLOURS = "INVERT_COLOURS"
private const val PINNED_SHORTCUTS = "PINNED_SHORTCUTS"

data class ShortcutEntry(
    val packageName: String,
    val shortcutId: String,
    val label: String,
) {
    val payload: String get() = "$packageName|$shortcutId"

    fun serialize(): String = "$packageName|$shortcutId|$label"

    companion object {
        fun parse(entry: String): ShortcutEntry? {
            val parts = entry.split("|")
            return if (parts.size >= 3) {
                ShortcutEntry(parts[0], parts[1], parts.drop(2).joinToString("|"))
            } else {
                null
            }
        }
    }
}

private const val APP_NAME = "APP_NAME"
private const val APP_PACKAGE = "APP_PACKAGE"
private const val APP_ALIAS = "APP_ALIAS"
private const val APP_ACTIVITY = "APP_ACTIVITY"
private const val APP_USER_SERIAL = "APP_USER_SERIAL"

enum class GestureType(
    val actionKey: String,
    val appKey: String,
    val defaultAction: Constants.Action,
) {
    SWIPE_LEFT("SWIPE_LEFT_ACTION", "SWIPE_LEFT", Constants.Action.ShowAppList),
    SWIPE_RIGHT("SWIPE_RIGHT_ACTION", "SWIPE_RIGHT", Constants.Action.ShowNotificationList),
    SWIPE_DOWN("SWIPE_DOWN_ACTION", "SWIPE_DOWN", Constants.Action.Disabled),
    SWIPE_UP("SWIPE_UP_ACTION", "SWIPE_UP", Constants.Action.Disabled),
    DOUBLE_TAP("DOUBLE_TAP_ACTION", "DOUBLE_TAP", Constants.Action.Disabled),
}

private const val PAGE_INDICATOR_POSITION = "page_indicator_position"
private const val SHOW_NOTIFICATION_INDICATOR = "show_notification_indicator"
private const val STATUS_BAR_ENABLED = "status_bar_enabled"
private const val TIME_ENABLED = "time_enabled"
private const val TIME_FORMAT = "time_format"
private const val SHOW_SECONDS = "show_seconds"
private const val LEADING_ZERO = "leading_zero"
private const val FLASHING_SECONDS = "flashing_seconds"
private const val BATTERY_ENABLED = "battery_enabled"
private const val BATTERY_PERCENTAGE = "battery_percentage"
private const val BATTERY_ICON = "battery_icon"
private const val CELLULAR_ENABLED = "cellular_enabled"
private const val WIFI_ENABLED = "wifi_enabled"
private const val BLUETOOTH_ENABLED = "bluetooth_enabled"
private const val FONT_SIZE_OPTION = "font_size_option"

class Prefs(
    val context: Context,
) {
    companion object {
        @Volatile private var instance: Prefs? = null

        fun getInstance(context: Context): Prefs =
            instance ?: synchronized(this) {
                instance ?: Prefs(context.applicationContext).also { instance = it }
            }
    }

    enum class TimeFormat { Standard, TwentyFourHour }

    enum class PageIndicatorPosition { Left, Right, Hidden }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_FILENAME, 0)
    private val userManager = context.getSystemService(Context.USER_SERVICE) as UserManager
    val mySerial: Long = userManager.getSerialNumberForUser(android.os.Process.myUserHandle())

    init {
        migrateHiddenApps()
    }

    private fun migrateHiddenApps() {
        val stored = prefs.getStringSet(HIDDEN_APPS, null) ?: return
        var changed = false
        val migrated =
            stored.mapTo(mutableSetOf()) { entry ->
                val parts = entry.split("|")
                if (parts.size == 2) {
                    val serial = parts[1].toLongOrNull()
                    if (serial == null || serial == mySerial) {
                        changed = true
                        parts[0]
                    } else {
                        entry
                    }
                } else {
                    entry
                }
            }
        if (changed) {
            prefs.edit().putStringSet(HIDDEN_APPS, migrated).apply()
        }
    }

    fun firstSettingsOpen(): Boolean = firstTrueFalseAfter(FIRST_SETTINGS_OPEN)

    var homePages: Int
        get() = prefs.getInt(HOME_PAGES, 1)
        set(value) = prefs.edit().putInt(HOME_PAGES, value.coerceIn(HomeLayout.MIN_PAGES, HomeLayout.MAX_PAGES)).apply()

    fun getAppsPerPage(page: Int): Int = prefs.getInt("${HOME_APPS_PER_PAGE}$page", 4)

    fun setAppsPerPage(
        page: Int,
        count: Int,
    ) {
        prefs.edit().putInt("${HOME_APPS_PER_PAGE}$page", count).apply()
    }

    private fun loadAction(
        prefString: String,
        default: Constants.Action,
    ): Constants.Action {
        val string = prefs.getString(prefString, default.name) ?: default.name
        return try {
            Constants.Action.valueOf(string)
        } catch (_: Exception) {
            default
        }
    }

    private fun storeAction(
        prefString: String,
        value: Constants.Action,
    ) {
        prefs.edit().putString(prefString, value.name).apply()
    }

    var invertColours: Boolean
        get() = prefs.getBoolean(INVERT_COLOURS, false)
        set(value) = prefs.edit().putBoolean(INVERT_COLOURS, value).apply()

    var hiddenApps: MutableSet<String>
        get() = prefs.getStringSet(HIDDEN_APPS, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        set(value) = prefs.edit().putStringSet(HIDDEN_APPS, value).apply()

    fun getHomeAppModel(i: Int): AppModel = loadApp("$i")

    fun setHomeAppModel(
        i: Int,
        appModel: AppModel,
    ) {
        storeApp("$i", appModel)
    }

    var pinnedShortcuts: Set<String>
        get() = prefs.getStringSet(PINNED_SHORTCUTS, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(PINNED_SHORTCUTS, value).apply()

    fun addPinnedShortcut(
        packageName: String,
        shortcutId: String,
        label: String,
    ) {
        val entry = ShortcutEntry(packageName, shortcutId, label)
        pinnedShortcuts = pinnedShortcuts + entry.serialize()
    }

    fun removePinnedShortcut(payload: String) {
        pinnedShortcuts =
            pinnedShortcuts
                .filterNot { entry ->
                    ShortcutEntry.parse(entry)?.payload == payload
                }.toSet()
    }

    var hiddenShortcutIds: Set<String>
        get() = prefs.getStringSet(HIDDEN_SHORTCUT_IDS, emptySet()) ?: emptySet()
        set(value) = prefs.edit().putStringSet(HIDDEN_SHORTCUT_IDS, value).apply()

    fun hideShortcut(id: String) {
        hiddenShortcutIds = hiddenShortcutIds + id
    }

    fun unhideShortcut(id: String) {
        hiddenShortcutIds = hiddenShortcutIds - id
    }

    fun getGestureApp(type: GestureType): AppModel = loadApp(type.appKey)

    fun setGestureApp(
        type: GestureType,
        appModel: AppModel,
    ) {
        storeApp(type.appKey, appModel)
    }

    fun getGestureAction(type: GestureType): Constants.Action = loadAction(type.actionKey, type.defaultAction)

    fun setGestureAction(
        type: GestureType,
        action: Constants.Action,
    ) {
        storeAction(type.actionKey, action)
    }

    private fun loadApp(id: String): AppModel {
        val name = prefs.getString("${APP_NAME}_$id", "") ?: ""
        val pack = prefs.getString("${APP_PACKAGE}_$id", "") ?: ""
        val alias = prefs.getString("${APP_ALIAS}_$id", "") ?: ""
        val activity = prefs.getString("${APP_ACTIVITY}_$id", "") ?: ""
        val serial = prefs.getLong("${APP_USER_SERIAL}_$id", -1L)
        val myHandle = android.os.Process.myUserHandle()
        val userHandle =
            if (serial >= 0) {
                userManager.getUserForSerialNumber(serial) ?: myHandle
            } else {
                myHandle
            }

        return AppModel(
            appLabel = name,
            appPackage = pack,
            appAlias = alias,
            appActivityName = activity,
            user = userHandle,
            key = null,
        )
    }

    private fun storeApp(
        id: String,
        appModel: AppModel,
    ) {
        val serial = userManager.getSerialNumberForUser(appModel.user)
        prefs
            .edit()
            .putString("${APP_NAME}_$id", appModel.appLabel)
            .putString("${APP_PACKAGE}_$id", appModel.appPackage)
            .putString("${APP_ACTIVITY}_$id", appModel.appActivityName)
            .putString("${APP_ALIAS}_$id", appModel.appAlias)
            .putLong("${APP_USER_SERIAL}_$id", serial)
            .apply()
    }

    var fontSizeOption: FontSizeOption
        get() {
            val key = prefs.getString(FONT_SIZE_OPTION, FontSizeOption.Medium.name)
            return FontSizeOption.fromKey(key)
        }
        set(value) = prefs.edit().putString(FONT_SIZE_OPTION, value.name).apply()

    var pageIndicatorPosition: PageIndicatorPosition
        get() {
            val stored =
                prefs.getString(PAGE_INDICATOR_POSITION, null)
                    ?: return PageIndicatorPosition.Left
            return try {
                PageIndicatorPosition.valueOf(stored)
            } catch (_: Exception) {
                PageIndicatorPosition.Left
            }
        }
        set(value) = prefs.edit().putString(PAGE_INDICATOR_POSITION, value.name).apply()

    var showNotificationIndicator: Boolean
        get() = prefs.getBoolean(SHOW_NOTIFICATION_INDICATOR, true)
        set(value) = prefs.edit().putBoolean(SHOW_NOTIFICATION_INDICATOR, value).apply()

    var statusBarEnabled: Boolean
        get() = prefs.getBoolean(STATUS_BAR_ENABLED, true)
        set(value) = prefs.edit().putBoolean(STATUS_BAR_ENABLED, value).apply()

    var timeEnabled: Boolean
        get() = prefs.getBoolean(TIME_ENABLED, true)
        set(value) = prefs.edit().putBoolean(TIME_ENABLED, value).apply()

    var timeFormat: TimeFormat
        get() {
            val stored = prefs.getString(TIME_FORMAT, null) ?: return TimeFormat.TwentyFourHour
            return try {
                TimeFormat.valueOf(stored)
            } catch (_: Exception) {
                TimeFormat.TwentyFourHour
            }
        }
        set(value) = prefs.edit().putString(TIME_FORMAT, value.name).apply()

    var showSeconds: Boolean
        get() = prefs.getBoolean(SHOW_SECONDS, false)
        set(value) = prefs.edit().putBoolean(SHOW_SECONDS, value).apply()

    var leadingZero: Boolean
        get() = prefs.getBoolean(LEADING_ZERO, false)
        set(value) = prefs.edit().putBoolean(LEADING_ZERO, value).apply()

    var flashingSeconds: Boolean
        get() = prefs.getBoolean(FLASHING_SECONDS, false)
        set(value) = prefs.edit().putBoolean(FLASHING_SECONDS, value).apply()

    var batteryEnabled: Boolean
        get() = prefs.getBoolean(BATTERY_ENABLED, true)
        set(value) = prefs.edit().putBoolean(BATTERY_ENABLED, value).apply()

    var batteryPercentage: Boolean
        get() = prefs.getBoolean(BATTERY_PERCENTAGE, true)
        set(value) = prefs.edit().putBoolean(BATTERY_PERCENTAGE, value).apply()

    var batteryIcon: Boolean
        get() = prefs.getBoolean(BATTERY_ICON, true)
        set(value) = prefs.edit().putBoolean(BATTERY_ICON, value).apply()

    var cellularEnabled: Boolean
        get() = prefs.getBoolean(CELLULAR_ENABLED, true)
        set(value) = prefs.edit().putBoolean(CELLULAR_ENABLED, value).apply()

    var wifiEnabled: Boolean
        get() = prefs.getBoolean(WIFI_ENABLED, true)
        set(value) = prefs.edit().putBoolean(WIFI_ENABLED, value).apply()

    var bluetoothEnabled: Boolean
        get() = prefs.getBoolean(BLUETOOTH_ENABLED, true)
        set(value) = prefs.edit().putBoolean(BLUETOOTH_ENABLED, value).apply()

    fun getHiddenAppKey(
        packageName: String,
        userSerial: Long,
    ): String = if (userSerial == mySerial) packageName else "$packageName|$userSerial"

    fun isAppHidden(
        packageName: String,
        userSerial: Long,
    ): Boolean {
        val hidden = hiddenApps
        return if (userSerial == mySerial) {
            hidden.contains(packageName)
        } else {
            hidden.contains("$packageName|$userSerial")
        }
    }

    fun getAppAlias(appName: String): String = prefs.getString(appName, "") ?: ""

    fun setAppAlias(
        appPackage: String,
        appAlias: String,
    ) {
        prefs.edit().putString(appPackage, appAlias).apply()
    }

    private fun firstTrueFalseAfter(key: String): Boolean {
        val first = prefs.getBoolean(key, true)
        if (first) {
            prefs.edit().putBoolean(key, false).apply()
        }
        return first
    }
}
