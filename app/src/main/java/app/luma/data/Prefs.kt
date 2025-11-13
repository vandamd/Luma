package app.luma.data

import android.content.Context
import android.content.SharedPreferences
import android.os.UserHandle
import android.util.Log
import app.luma.helper.getUserHandleFromString
import app.luma.style.FontSizeOption
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


private const val PREFS_FILENAME = "app.luma"

private const val FIRST_OPEN = "FIRST_OPEN"
private const val FIRST_SETTINGS_OPEN = "FIRST_SETTINGS_OPEN"
private const val LOCK_MODE = "LOCK_MODE"
private const val HOME_APPS_NUM = "HOME_APPS_NUM"
private const val HOME_PAGES = "HOME_PAGES"
private const val HOME_APPS_PER_PAGE = "HOME_APPS_PER_PAGE_"
private const val AUTO_OPEN_APP = "AUTO_OPEN_APP"
private const val HOME_ALIGNMENT = "HOME_ALIGNMENT"
private const val HOME_ALIGNMENT_BOTTOM = "HOME_ALIGNMENT_BOTTOM"
private const val HOME_CLICK_AREA = "HOME_CLICK_AREA"
private const val DRAWER_ALIGNMENT = "DRAWER_ALIGNMENT"
private const val TIME_ALIGNMENT = "TIME_ALIGNMENT"
private const val STATUS_BAR = "STATUS_BAR"
private const val SHOW_DATE = "SHOW_DATE"
private const val HOME_LOCKED = "HOME_LOCKED"
private const val SHOW_TIME = "SHOW_TIME"
private const val SWIPE_DOWN_ACTION = "SWIPE_DOWN_ACTION"
private const val SWIPE_UP_ACTION = "SWIPE_UP_ACTION"
private const val SWIPE_RIGHT_ACTION = "SWIPE_RIGHT_ACTION"
private const val SWIPE_LEFT_ACTION = "SWIPE_LEFT_ACTION"
private const val CLICK_CLOCK_ACTION = "CLICK_CLOCK_ACTION"
private const val CLICK_DATE_ACTION = "CLICK_DATE_ACTION"
private const val DOUBLE_TAP_ACTION = "DOUBLE_TAP_ACTION"
private const val SCREEN_TIMEOUT = "SCREEN_TIMEOUT"
private const val HIDDEN_APPS = "HIDDEN_APPS"
private const val HIDDEN_APPS_UPDATED = "HIDDEN_APPS_UPDATED"
private const val SHOW_HINT_COUNTER = "SHOW_HINT_COUNTER"
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
private const val CLICK_CLOCK = "CLICK_CLOCK"
private const val CLICK_DATE = "CLICK_DATE"
private const val DOUBLE_TAP = "DOUBLE_TAP"

private const val TEXT_SIZE = "text_size"
private const val PAGE_INDICATOR_POSITION = "page_indicator_position"
private const val SHOW_NOTIFICATION_INDICATOR = "show_notification_indicator"
private const val FONT_SIZE_OPTION = "font_size_option"

class Prefs(val context: Context) {

    enum class PageIndicatorPosition { Left, Right, Hidden }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_FILENAME, 0)

    fun saveToString(): String {
        val all: HashMap<String, Any?> = HashMap(prefs.all)
        return Gson().toJson(all)
    }

    fun loadFromString(json: String) {
        val editor = prefs.edit()
        val all: HashMap<String, Any?> = Gson().fromJson(json, object : TypeToken<HashMap<String, Any?>>() {}.type)
        for ((key, value) in all) {
            when (value) {
                is String -> editor.putString(key, value)
                is Boolean -> editor.putBoolean(key, value)
                is Int -> editor.putInt(key, value)
                is Double -> editor.putInt(key, value.toInt())
                is Float -> editor.putInt(key, value.toInt())
                is MutableSet<*> -> {
                    val list = value.filterIsInstance<String>().toSet()
                    editor.putStringSet(key, list)
                }
                else ->  { Log.d("backup error", "$value") }
            }
        }
        editor.apply()
    }

    fun firstOpen(): Boolean {
        return firstTrueFalseAfter(FIRST_OPEN)
    }

    fun firstSettingsOpen(): Boolean {
        return firstTrueFalseAfter(FIRST_SETTINGS_OPEN)
    }

    var lockModeOn: Boolean
        get() = prefs.getBoolean(LOCK_MODE, false)
        set(value) = prefs.edit().putBoolean(LOCK_MODE, value).apply()

    var autoOpenApp: Boolean
        get() = prefs.getBoolean(AUTO_OPEN_APP, false)
        set(value) = prefs.edit().putBoolean(AUTO_OPEN_APP, value).apply()

    var homeAppsNum: Int
        get() {
            return try {
                prefs.getInt(HOME_APPS_NUM, 4)
            } catch (_: Exception) {
                4
            }
        }
        set(value) = prefs.edit().putInt(HOME_APPS_NUM, value).apply()

    var homePages: Int
        get() {
            return try {
                prefs.getInt(HOME_PAGES, 1)
            } catch (_: Exception) {
                1
            }
        }
        set(value) = prefs.edit().putInt(HOME_PAGES, value.coerceIn(1, 5)).apply()

    fun getAppsPerPage(page: Int): Int {
        return try {
            prefs.getInt("${HOME_APPS_PER_PAGE}$page", 4)
        } catch (_: Exception) {
            4
        }
    }

    fun setAppsPerPage(page: Int, count: Int) {
        prefs.edit().putInt("${HOME_APPS_PER_PAGE}$page", count).apply()
    }

    val homeAlignment: Constants.Gravity
        get() = Constants.Gravity.Center

    val homeAlignmentBottom: Boolean
        get() = false

    var extendHomeAppsArea: Boolean
        get() = prefs.getBoolean(HOME_CLICK_AREA, false)
        set(value) = prefs.edit().putBoolean(HOME_CLICK_AREA, value).apply()

    val drawerAlignment: Constants.Gravity
        get() = Constants.Gravity.Center

    var homeLocked: Boolean
        get() = prefs.getBoolean(HOME_LOCKED, false)
        set(value) = prefs.edit().putBoolean(HOME_LOCKED, value).apply()

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

    var clickClockAction: Constants.Action
        get() = loadAction(CLICK_CLOCK_ACTION, Constants.Action.OpenApp)
        set(value) = storeAction(CLICK_CLOCK_ACTION, value)

    var clickDateAction: Constants.Action
        get() = loadAction(CLICK_DATE_ACTION, Constants.Action.OpenApp)
        set(value) = storeAction(CLICK_DATE_ACTION, value)

    var doubleTapAction: Constants.Action
        get() = loadAction(DOUBLE_TAP_ACTION, Constants.Action.Disabled)
        set(value) = storeAction(DOUBLE_TAP_ACTION, value)

    private fun loadAction(prefString: String, default: Constants.Action): Constants.Action {
        val string = prefs.getString(
            prefString,
            default.toString()
        ).toString()
        return Constants.Action.valueOf(string)
    }

    private fun storeAction(prefString: String, value: Constants.Action) {
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

    fun getHomeAppModel(i:Int): AppModel {
        return loadApp("$i")
    }

    fun setHomeAppModel(i: Int, appModel: AppModel) {
        storeApp("$i", appModel)
    }

    fun setHomeAppName(i: Int, name: String) {
        val nameId = "${APP_NAME}_$i"
        prefs.edit().putString(nameId, name).apply()
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

    var appClickClock: AppModel
        get() = loadApp(CLICK_CLOCK)
        set(appModel) = storeApp(CLICK_CLOCK, appModel)

    var appClickDate: AppModel
        get() = loadApp(CLICK_DATE)
        set(appModel) = storeApp(CLICK_DATE, appModel)

    var appDoubleTap: AppModel
        get() = loadApp(DOUBLE_TAP)
        set(appModel) = storeApp(DOUBLE_TAP, appModel)

    private fun loadApp(id: String): AppModel {
        val name = prefs.getString("${APP_NAME}_$id", "").toString()
        val pack = prefs.getString("${APP_PACKAGE}_$id", "").toString()
        val alias = prefs.getString("${APP_ALIAS}_$id", "").toString()
        val activity = prefs.getString("${APP_ACTIVITY}_$id", "").toString()

        val userHandleString = try { prefs.getString("${APP_USER}_$id", "").toString() } catch (_: Exception) { "" }
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

    private fun storeApp(id: String, appModel: AppModel) {
        val edit = prefs.edit()
        edit.putString("${APP_NAME}_$id", appModel.appLabel)
        edit.putString("${APP_PACKAGE}_$id", appModel.appPackage)
        edit.putString("${APP_ACTIVITY}_$id", appModel.appActivityName)
        edit.putString("${APP_ALIAS}_$id", appModel.appAlias)
        edit.putString("${APP_USER}_$id", appModel.user.toString())
        edit.apply()
    }

    var textSize: Int
        get() {
            return try {
                prefs.getInt(TEXT_SIZE, 41)
            } catch (_: Exception) {
                18
            }
        }
        set(value) = prefs.edit().putInt(TEXT_SIZE, value).apply()

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

    fun getAppName(location: Int): String {
        return getHomeAppModel(location).appLabel
    }

    fun getAppAlias(appName: String): String {
        return prefs.getString(appName, "").toString()
    }
    fun setAppAlias(appPackage: String, appAlias: String) {
        prefs.edit().putString(appPackage, appAlias).apply()
    }

    private fun firstTrueFalseAfter(key: String): Boolean {
        val first = prefs.getBoolean(key, true)
        if (first) {
            prefs.edit().putBoolean(key, false).apply()
        }
        return  first
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
