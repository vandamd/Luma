package app.luma

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import app.luma.data.AppModel
import app.luma.data.Constants
import app.luma.data.Constants.AppDrawerFlag
import app.luma.data.Prefs
import app.luma.helper.*
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val appContext by lazy { application.applicationContext }
    private val prefs = Prefs(appContext)

    // setup variables with initial values
    val showMessageDialog = MutableLiveData<String>()

    val appList = MutableLiveData<List<AppModel>?>()
    val hiddenApps = MutableLiveData<List<AppModel>?>()
    val isLumaDefault = MutableLiveData<Boolean>()
    val launcherResetFailed = MutableLiveData<Boolean>()

    val homeAppsAlignment = MutableLiveData(Pair(prefs.homeAlignment, false))
    val homeAppsCount = MutableLiveData(prefs.homeAppsNum)
    
    // Track the current filtering state to prevent caching issues
    private var lastShowHiddenApps: Boolean? = null

    fun selectedApp(appModel: AppModel, flag: AppDrawerFlag, n: Int = 0) {
        when (flag) {
            AppDrawerFlag.LaunchApp, AppDrawerFlag.HiddenApps -> {
                launchApp(appModel)
            }
            AppDrawerFlag.SetHomeApp -> {
                prefs.setHomeAppModel(n, appModel)
            }
            AppDrawerFlag.SetSwipeLeft -> prefs.appSwipeLeft = appModel
            AppDrawerFlag.SetSwipeRight -> prefs.appSwipeRight = appModel
            AppDrawerFlag.SetSwipeUp -> prefs.appSwipeUp = appModel
            AppDrawerFlag.SetSwipeDown -> prefs.appSwipeDown = appModel
            AppDrawerFlag.SetClickClock -> prefs.appClickClock = appModel
            AppDrawerFlag.SetClickDate -> prefs.appClickDate = appModel
            AppDrawerFlag.SetDoubleTap -> prefs.appDoubleTap = appModel
        }
    }


    private fun launchApp(appModel: AppModel) {
        val packageName = appModel.appPackage
        val appActivityName = appModel.appActivityName
        val userHandle = appModel.user
        val launcher = appContext.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        val activityInfo = launcher.getActivityList(packageName, userHandle)

        // TODO: Handle multiple launch activities in an app. This is NOT the way.
        val component = when (activityInfo.size) {
            0 -> {
                showToastShort(appContext, "App not found")
                return
            }
            1 -> ComponentName(packageName, activityInfo[0].name)
            else -> if (appActivityName.isNotEmpty()) {
                ComponentName(packageName, appActivityName)
            } else {
                ComponentName(packageName, activityInfo[activityInfo.size - 1].name)
            }
        }

        try {
            launcher.startMainActivity(component, userHandle, null, null)
        } catch (e: SecurityException) {
            try {
                launcher.startMainActivity(component, android.os.Process.myUserHandle(), null, null)
            } catch (e: Exception) {
                showToastShort(appContext, "Unable to launch app")
            }
        } catch (e: Exception) {
            showToastShort(appContext, "Unable to launch app")
        }
    }

    fun getAppList(showHiddenApps: Boolean = false) {
        viewModelScope.launch {
            // Always load all apps - filtering will be done at UI level to prevent flickering
            appList.value = getAppsList(appContext, true)
        }
    }

    fun getHiddenApps() {
        viewModelScope.launch {
            hiddenApps.value = getHiddenAppsList(appContext)
        }
    }

    fun isLumaDefault() {
        isLumaDefault.value = isLumaDefault(appContext)
    }

    fun resetDefaultLauncherApp(context: Context) {
        resetDefaultLauncher(context)
        launcherResetFailed.value = getDefaultLauncherPackage(
            appContext
        ).contains(".")
    }


    fun updateHomeAppsAlignment(gravity: Constants.Gravity, onBottom: Boolean) {
        homeAppsAlignment.value = Pair(gravity, onBottom)
    }

    fun showMessageDialog(message: String) {
        showMessageDialog.postValue(message)
    }
}
