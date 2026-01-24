package app.luma.data

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

    enum class Action {
        Disabled,
        OpenApp,
        LockScreen,
        ShowAppList,
        OpenQuickSettings,
        ShowRecents,
        ShowNotification,
        ;

        fun displayName(): String =
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
}
