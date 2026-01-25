package app.luma.data

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.luma.R

object Constants {
    const val REQUEST_CONFIRM_PIN_SHORTCUT = "android.content.pm.action.CONFIRM_PIN_SHORTCUT"
    const val PINNED_SHORTCUT_PACKAGE = "__pinned_shortcut__"

    enum class AppDrawerFlag {
        LaunchApp,
        HiddenApps,
        SetHomeApp,
        SetSwipeLeft,
        SetSwipeRight,
        SetSwipeUp,
        SetSwipeDown,
        SetDoubleTap,
    }

    enum class Action(
        @StringRes val displayNameRes: Int,
    ) {
        Disabled(R.string.action_disabled),
        OpenApp(R.string.action_open_app),
        LockScreen(R.string.action_lock_screen),
        ShowAppList(R.string.action_show_app_list),
        OpenQuickSettings(R.string.action_quick_settings),
        ShowRecents(R.string.action_show_recents),
        ShowNotification(R.string.action_show_notifications),
        ;

        fun displayName(context: Context): String = context.getString(displayNameRes)

        @Composable
        fun displayName(): String = stringResource(displayNameRes)
    }
}
