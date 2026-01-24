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

class PageCountFragment : Fragment() {
    private lateinit var prefs: Prefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs.getInstance(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = composeView { PageCountScreen() }

    @Composable
    fun PageCountScreen() {
        Column {
            SettingsHeader(
                title = "Number of Pages",
                onBack = ::goBack,
            )

            ContentContainer {
                CustomScrollView {
                    for (i in 1..5) {
                        val isSelected = prefs.homePages == i
                        SimpleTextButton(
                            title = "$i Page${if (i > 1) "s" else ""}",
                            underline = isSelected,
                            onClick = { updateHomePages(i) },
                        )
                    }
                }
            }
        }
    }

    private fun updateHomePages(homePages: Int) {
        prefs.homePages = homePages
        goBack()
    }
}
