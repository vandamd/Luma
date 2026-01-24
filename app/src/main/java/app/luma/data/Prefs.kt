package app.luma.data

import android.content.Context
import android.content.SharedPreferences
import app.luma.style.FontSizeOption

private const val PREFS_FILENAME = "app.luma"

private const val FIRST_SETTINGS_OPEN = "FIRST_SETTINGS_OPEN"
private const val HOME_PAGES = "HOME_PAGES"
private const val HOME_APPS_PER_PAGE = "HOME_APPS_PER_PAGE_"

private const val HIDDEN_APPS = "HIDDEN_APPS"
private const val INVERT_COLOURS = "INVERT_COLOURS"

private const val APP_NAME = "APP_NAME"
private const val APP_PACKAGE = "APP_PACKAGE"
private const val APP_USER = "APP_USER"
private const val APP_ALIAS = "APP_ALIAS"
private const val APP_ACTIVITY = "APP_ACTIVITY"

enum class GestureType(
    val actionKey: String,
    val appKey: String,
    val defaultAction: Constants.Action,
) {
    SWIPE_LEFT("SWIPE_LEFT_ACTION", "SWIPE_LEFT", Constants.Action.ShowAppList),
    SWIPE_RIGHT("SWIPE_RIGHT_ACTION", "SWIPE_RIGHT", Constants.Action.Disabled),
    SWIPE_DOWN("SWIPE_DOWN_ACTION", "SWIPE_DOWN", Constants.Action.ShowNotification),
    SWIPE_UP("SWIPE_UP_ACTION", "SWIPE_UP", Constants.Action.Disabled),
    DOUBLE_TAP("DOUBLE_TAP_ACTION", "DOUBLE_TAP", Constants.Action.Disabled),
}

private const val PAGE_INDICATOR_POSITION = "page_indicator_position"
private const val SHOW_NOTIFICATION_INDICATOR = "show_notification_indicator"
private const val FONT_SIZE_OPTION = "font_size_option"

class Prefs(
    val context: Context,
) {
    enum class PageIndicatorPosition { Left, Right, Hidden }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_FILENAME, 0)

    fun firstSettingsOpen(): Boolean = firstTrueFalseAfter(FIRST_SETTINGS_OPEN)

    var homePages: Int
        get() {
            return try {
                prefs.getInt(HOME_PAGES, 1)
            } catch (_: Exception) {
                1
            }
        }
        set(value) = prefs.edit().putInt(HOME_PAGES, value.coerceIn(1, 5)).apply()

    fun getAppsPerPage(page: Int): Int =
        try {
            prefs.getInt("${HOME_APPS_PER_PAGE}$page", 4)
        } catch (_: Exception) {
            4
        }

    fun setAppsPerPage(
        page: Int,
        count: Int,
    ) {
        prefs.edit().putInt("${HOME_APPS_PER_PAGE}$page", count).apply()
    }

    var swipeLeftAction: Constants.Action
        get() = getGestureAction(GestureType.SWIPE_LEFT)
        set(value) = setGestureAction(GestureType.SWIPE_LEFT, value)

    var swipeRightAction: Constants.Action
        get() = getGestureAction(GestureType.SWIPE_RIGHT)
        set(value) = setGestureAction(GestureType.SWIPE_RIGHT, value)

    var swipeDownAction: Constants.Action
        get() = getGestureAction(GestureType.SWIPE_DOWN)
        set(value) = setGestureAction(GestureType.SWIPE_DOWN, value)

    var swipeUpAction: Constants.Action
        get() = getGestureAction(GestureType.SWIPE_UP)
        set(value) = setGestureAction(GestureType.SWIPE_UP, value)

    var doubleTapAction: Constants.Action
        get() = getGestureAction(GestureType.DOUBLE_TAP)
        set(value) = setGestureAction(GestureType.DOUBLE_TAP, value)

    private fun loadAction(
        prefString: String,
        default: Constants.Action,
    ): Constants.Action {
        val string = prefs.getString(prefString, default.name) ?: default.name
        return Constants.Action.valueOf(string)
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
        get() = prefs.getStringSet(HIDDEN_APPS, mutableSetOf()) as MutableSet<String>
        set(value) = prefs.edit().putStringSet(HIDDEN_APPS, value).apply()

    fun getHomeAppModel(i: Int): AppModel = loadApp("$i")

    fun setHomeAppModel(
        i: Int,
        appModel: AppModel,
    ) {
        storeApp("$i", appModel)
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

    var appSwipeRight: AppModel
        get() = getGestureApp(GestureType.SWIPE_RIGHT)
        set(appModel) = setGestureApp(GestureType.SWIPE_RIGHT, appModel)

    var appSwipeLeft: AppModel
        get() = getGestureApp(GestureType.SWIPE_LEFT)
        set(appModel) = setGestureApp(GestureType.SWIPE_LEFT, appModel)

    var appSwipeDown: AppModel
        get() = getGestureApp(GestureType.SWIPE_DOWN)
        set(appModel) = setGestureApp(GestureType.SWIPE_DOWN, appModel)

    var appSwipeUp: AppModel
        get() = getGestureApp(GestureType.SWIPE_UP)
        set(appModel) = setGestureApp(GestureType.SWIPE_UP, appModel)

    var appDoubleTap: AppModel
        get() = getGestureApp(GestureType.DOUBLE_TAP)
        set(appModel) = setGestureApp(GestureType.DOUBLE_TAP, appModel)

    private fun loadApp(id: String): AppModel {
        val name = prefs.getString("${APP_NAME}_$id", "") ?: ""
        val pack = prefs.getString("${APP_PACKAGE}_$id", "") ?: ""
        val alias = prefs.getString("${APP_ALIAS}_$id", "") ?: ""
        val activity = prefs.getString("${APP_ACTIVITY}_$id", "") ?: ""

        return AppModel(
            appLabel = name,
            appPackage = pack,
            appAlias = alias,
            appActivityName = activity,
            user = android.os.Process.myUserHandle(),
            key = null,
        )
    }

    private fun storeApp(
        id: String,
        appModel: AppModel,
    ) {
        val edit = prefs.edit()
        edit.putString("${APP_NAME}_$id", appModel.appLabel)
        edit.putString("${APP_PACKAGE}_$id", appModel.appPackage)
        edit.putString("${APP_ACTIVITY}_$id", appModel.appActivityName)
        edit.putString("${APP_ALIAS}_$id", appModel.appAlias)
        edit.putString("${APP_USER}_$id", appModel.user.toString())
        edit.apply()
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

    fun clear() {
        prefs.edit().clear().apply()
    }
}
