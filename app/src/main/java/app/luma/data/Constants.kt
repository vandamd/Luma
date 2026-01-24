package app.luma.data

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.luma.R

interface EnumOption {
    @Composable
    fun string(): String
}

object Constants {
    const val REQUEST_CODE_ENABLE_ADMIN = 666
    const val LONG_PRESS_DELAY_MS = 500

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

    enum class Action : EnumOption {
        Disabled,
        OpenApp,
        LockScreen,
        ShowAppList,
        OpenQuickSettings,
        ShowRecents,
        ShowNotification,
        ;

        @Composable
        override fun string(): String =
            when (this) {
                OpenApp -> stringResource(R.string.open_app)
                LockScreen -> stringResource(R.string.lock_screen)
                ShowNotification -> stringResource(R.string.show_notifications)
                ShowAppList -> stringResource(R.string.show_app_list)
                OpenQuickSettings -> stringResource(R.string.open_quick_settings)
                ShowRecents -> stringResource(R.string.show_recents)
                Disabled -> stringResource(R.string.disabled)
            }
    }

    enum class Theme : EnumOption {
        System,
        Dark,
        Light,
        ;

        @Composable
        override fun string(): String =
            when (this) {
                System -> "System"
                Dark -> stringResource(R.string.dark)
                Light -> stringResource(R.string.light)
            }
    }
}
