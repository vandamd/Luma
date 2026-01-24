package app.luma.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.fragment.app.Fragment
import app.luma.data.Prefs
import app.luma.ui.compose.CustomScrollView
import app.luma.ui.compose.SettingsComposable.ContentContainer
import app.luma.ui.compose.SettingsComposable.SettingsHeader
import app.luma.ui.compose.SettingsComposable.SimpleTextButton

class PageIndicatorPositionFragment : Fragment() {
    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs.getInstance(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = composeView { Screen() }

    @Composable
    fun Screen() {
        Column {
            SettingsHeader(
                title = "Page Indicator Position",
                onBack = ::goBack,
            )
            ContentContainer {
                CustomScrollView {
                    SimpleTextButton(
                        title = "Left",
                        underline = prefs.pageIndicatorPosition == Prefs.PageIndicatorPosition.Left,
                        onClick = { selectPosition(Prefs.PageIndicatorPosition.Left) },
                    )
                    SimpleTextButton(
                        title = "Right",
                        underline = prefs.pageIndicatorPosition == Prefs.PageIndicatorPosition.Right,
                        onClick = { selectPosition(Prefs.PageIndicatorPosition.Right) },
                    )
                    SimpleTextButton(
                        title = "Hidden",
                        underline = prefs.pageIndicatorPosition == Prefs.PageIndicatorPosition.Hidden,
                        onClick = { selectPosition(Prefs.PageIndicatorPosition.Hidden) },
                    )
                }
            }
        }
    }

    private fun selectPosition(position: Prefs.PageIndicatorPosition) {
        prefs.pageIndicatorPosition = position
        goBack()
    }
}
