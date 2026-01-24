package app.luma.data

import androidx.compose.runtime.Composable

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
                OpenApp -> "Open App"
                LockScreen -> "Lock Screen"
                ShowNotification -> "Show Notifications"
                ShowAppList -> "Show App List"
                OpenQuickSettings -> "Quick Settings"
                ShowRecents -> "Show Recents"
                Disabled -> "Disabled"
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
                Dark -> "Dark"
                Light -> "Light"
            }
    }
}
