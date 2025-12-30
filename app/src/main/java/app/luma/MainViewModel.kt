package app.luma

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
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

    val showMessageDialog = MutableLiveData<String>()

    val appList = MutableLiveData<List<AppModel>?>()
    val hiddenApps = MutableLiveData<List<AppModel>?>()
    val launcherResetFailed = MutableLiveData<Boolean>()

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

    fun getAppList() {
        viewModelScope.launch {
            appList.value = getAppsList(appContext, true)
        }
    }

    fun getHiddenApps() {
        viewModelScope.launch {
            hiddenApps.value = getHiddenAppsList(appContext)
        }
    }

    fun resetDefaultLauncherApp(context: Context) {
        resetDefaultLauncher(context)
        launcherResetFailed.value = getDefaultLauncherPackage(
            appContext
        ).contains(".")
    }


    fun showMessageDialog(message: String) {
        showMessageDialog.postValue(message)
    }
}
