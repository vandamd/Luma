package app.luma.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import app.luma.MainViewModel
import app.luma.R
import app.luma.data.Constants
import app.luma.data.Constants.Action
import app.luma.data.Constants.AppDrawerFlag
import app.luma.data.GestureType
import app.luma.data.Prefs
import app.luma.ui.compose.CustomScrollView
import app.luma.ui.compose.SettingsComposable.ContentContainer
import app.luma.ui.compose.SettingsComposable.SettingsHeader
import app.luma.ui.compose.SettingsComposable.SimpleTextButton

/**
 * Unified fragment for configuring gesture actions (swipe left/right/up/down, double tap).
 * Pass the gesture type via arguments using the GESTURE_TYPE key.
 */
class GestureActionFragment : Fragment() {
    companion object {
        const val GESTURE_TYPE = "gesture_type"

        private val gestureDisplayInfo =
            mapOf(
                GestureType.SWIPE_LEFT to GestureDisplayInfo("Swipe left", AppDrawerFlag.SetSwipeLeft),
                GestureType.SWIPE_RIGHT to GestureDisplayInfo("Swipe right", AppDrawerFlag.SetSwipeRight),
                GestureType.SWIPE_UP to GestureDisplayInfo("Swipe up", AppDrawerFlag.SetSwipeUp),
                GestureType.SWIPE_DOWN to GestureDisplayInfo("Swipe down", AppDrawerFlag.SetSwipeDown),
                GestureType.DOUBLE_TAP to GestureDisplayInfo("Double tap", AppDrawerFlag.SetDoubleTap),
            )
    }

    private data class GestureDisplayInfo(
        val title: String,
        val appDrawerFlag: AppDrawerFlag,
    )

    private lateinit var prefs: Prefs
    private lateinit var gestureType: GestureType

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs.getInstance(requireContext())
        val typeName = arguments?.getString(GESTURE_TYPE) ?: GestureType.SWIPE_LEFT.name
        gestureType = GestureType.valueOf(typeName)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = composeView { GestureScreen() }

    private fun getDisplayInfo(): GestureDisplayInfo = gestureDisplayInfo[gestureType] ?: error("Unknown gesture type: $gestureType")

    @Composable
    fun GestureScreen() {
        val displayInfo = getDisplayInfo()
        Column {
            SettingsHeader(
                title = displayInfo.title,
                onBack = ::goBack,
            )

            ContentContainer {
                CustomScrollView {
                    for (action in Constants.Action.values()) {
                        val isSelected = getCurrentAction() == action
                        val buttonText =
                            when (action) {
                                Constants.Action.OpenApp -> "Open ${getAppLabel()}"
                                else -> action.displayName()
                            }
                        SimpleTextButton(
                            title = buttonText,
                            underline = isSelected,
                            onClick = { handleActionSelection(action) },
                        )
                    }
                }
            }
        }
    }

    private fun getCurrentAction(): Action = prefs.getGestureAction(gestureType)

    private fun setCurrentAction(action: Action) {
        prefs.setGestureAction(gestureType, action)
    }

    private fun getAppLabel(): String = prefs.getGestureApp(gestureType).appLabel

    private fun handleActionSelection(action: Action) {
        setCurrentAction(action)
        if (action == Action.OpenApp) {
            val viewModel = ViewModelProvider(requireActivity())[MainViewModel::class.java]
            viewModel.getAppList()
            val displayInfo = getDisplayInfo()
            findNavController().navigate(
                R.id.appListFragment,
                bundleOf("flag" to displayInfo.appDrawerFlag.toString()),
            )
        } else {
            goBack()
        }
    }
}
