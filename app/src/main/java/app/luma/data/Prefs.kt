package app.luma.data

import android.content.Context
import android.content.SharedPreferences
import android.os.UserHandle
import app.luma.helper.getUserHandleFromString
import app.luma.style.FontSizeOption

private const val PREFS_FILENAME = "app.luma"

private const val FIRST_SETTINGS_OPEN = "FIRST_SETTINGS_OPEN"
private const val HOME_PAGES = "HOME_PAGES"
private const val HOME_APPS_PER_PAGE = "HOME_APPS_PER_PAGE_"

private const val SWIPE_DOWN_ACTION = "SWIPE_DOWN_ACTION"
private const val SWIPE_UP_ACTION = "SWIPE_UP_ACTION"
private const val SWIPE_RIGHT_ACTION = "SWIPE_RIGHT_ACTION"
private const val SWIPE_LEFT_ACTION = "SWIPE_LEFT_ACTION"

private const val DOUBLE_TAP_ACTION = "DOUBLE_TAP_ACTION"
private const val HIDDEN_APPS = "HIDDEN_APPS"
private const val HIDDEN_APPS_UPDATED = "HIDDEN_APPS_UPDATED"
private const val APP_THEME = "APP_THEME"

private const val APP_NAME = "APP_NAME"
private const val APP_PACKAGE = "APP_PACKAGE"
private const val APP_USER = "APP_USER"
private const val APP_ALIAS = "APP_ALIAS"
private const val APP_ACTIVITY = "APP_ACTIVITY"

private const val SWIPE_RIGHT = "SWIPE_RIGHT"
private const val SWIPE_LEFT = "SWIPE_LEFT"
private const val SWIPE_DOWN = "SWIPE_DOWN"
private const val SWIPE_UP = "SWIPE_UP"
private const val DOUBLE_TAP = "DOUBLE_TAP"

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
        get() = loadAction(SWIPE_LEFT_ACTION, Constants.Action.ShowAppList)
        set(value) = storeAction(SWIPE_LEFT_ACTION, value)

    var swipeRightAction: Constants.Action
        get() = loadAction(SWIPE_RIGHT_ACTION, Constants.Action.Disabled)
        set(value) = storeAction(SWIPE_RIGHT_ACTION, value)

    var swipeDownAction: Constants.Action
        get() = loadAction(SWIPE_DOWN_ACTION, Constants.Action.ShowNotification)
        set(value) = storeAction(SWIPE_DOWN_ACTION, value)

    var swipeUpAction: Constants.Action
        get() = loadAction(SWIPE_UP_ACTION, Constants.Action.Disabled)
        set(value) = storeAction(SWIPE_UP_ACTION, value)

    var doubleTapAction: Constants.Action
        get() = loadAction(DOUBLE_TAP_ACTION, Constants.Action.Disabled)
        set(value) = storeAction(DOUBLE_TAP_ACTION, value)

    private fun loadAction(
        prefString: String,
        default: Constants.Action,
    ): Constants.Action {
        val string =
            prefs
                .getString(
                    prefString,
                    default.toString(),
                ).toString()
        return Constants.Action.valueOf(string)
    }

    private fun storeAction(
        prefString: String,
        value: Constants.Action,
    ) {
        prefs.edit().putString(prefString, value.name).apply()
    }

    var appTheme: Constants.Theme
        get() {
            return try {
                Constants.Theme.valueOf(prefs.getString(APP_THEME, Constants.Theme.Dark.name).toString())
            } catch (_: Exception) {
                Constants.Theme.System
            }
        }
        set(value) = prefs.edit().putString(APP_THEME, value.name).apply()

    var hiddenApps: MutableSet<String>
        get() = prefs.getStringSet(HIDDEN_APPS, mutableSetOf()) as MutableSet<String>
        set(value) = prefs.edit().putStringSet(HIDDEN_APPS, value).apply()

    var hiddenAppsUpdated: Boolean
        get() = prefs.getBoolean(HIDDEN_APPS_UPDATED, false)
        set(value) = prefs.edit().putBoolean(HIDDEN_APPS_UPDATED, value).apply()

    fun getHomeAppModel(i: Int): AppModel = loadApp("$i")

    fun setHomeAppModel(
        i: Int,
        appModel: AppModel,
    ) {
        storeApp("$i", appModel)
    }

    var appSwipeRight: AppModel
        get() = loadApp(SWIPE_RIGHT)
        set(appModel) = storeApp(SWIPE_RIGHT, appModel)

    var appSwipeLeft: AppModel
        get() = loadApp(SWIPE_LEFT)
        set(appModel) = storeApp(SWIPE_LEFT, appModel)

    var appSwipeDown: AppModel
        get() = loadApp(SWIPE_DOWN)
        set(appModel) = storeApp(SWIPE_DOWN, appModel)

    var appSwipeUp: AppModel
        get() = loadApp(SWIPE_UP)
        set(appModel) = storeApp(SWIPE_UP, appModel)

    var appDoubleTap: AppModel
        get() = loadApp(DOUBLE_TAP)
        set(appModel) = storeApp(DOUBLE_TAP, appModel)

    private fun loadApp(id: String): AppModel {
        val name = prefs.getString("${APP_NAME}_$id", "").toString()
        val pack = prefs.getString("${APP_PACKAGE}_$id", "").toString()
        val alias = prefs.getString("${APP_ALIAS}_$id", "").toString()
        val activity = prefs.getString("${APP_ACTIVITY}_$id", "").toString()

        val userHandleString =
            try {
                prefs.getString("${APP_USER}_$id", "").toString()
            } catch (_: Exception) {
                ""
            }
        val userHandle: UserHandle = getUserHandleFromString(context, userHandleString)

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
            return try {
                PageIndicatorPosition.valueOf(prefs.getString(PAGE_INDICATOR_POSITION, PageIndicatorPosition.Left.name).toString())
            } catch (_: Exception) {
                PageIndicatorPosition.Left
            }
        }
        set(value) = prefs.edit().putString(PAGE_INDICATOR_POSITION, value.name).apply()

    var showNotificationIndicator: Boolean
        get() = prefs.getBoolean(SHOW_NOTIFICATION_INDICATOR, true)
        set(value) = prefs.edit().putBoolean(SHOW_NOTIFICATION_INDICATOR, value).apply()

    fun getAppAlias(appName: String): String = prefs.getString(appName, "").toString()

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
