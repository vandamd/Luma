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

class AppCountFragment : Fragment() {
    private lateinit var prefs: Prefs
    private var pageNumber: Int = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(requireContext())
        pageNumber = arguments?.getInt("pageNumber", 1) ?: 1
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = composeView { AppCountScreen() }

    @Composable
    fun AppCountScreen() {
        Column {
            SettingsHeader(
                title = "Page $pageNumber, Number of Apps",
                onBack = ::goBack,
            )

            ContentContainer {
                CustomScrollView {
                    for (i in 1..6) {
                        val isSelected = prefs.getAppsPerPage(pageNumber) == i
                        SimpleTextButton(
                            title = "$i App${if (i > 1) "s" else ""}",
                            underline = isSelected,
                            onClick = { updateAppsPerPage(pageNumber, i) },
                        )
                    }
                }
            }
        }
    }

    private fun updateAppsPerPage(
        page: Int,
        count: Int,
    ) {
        prefs.setAppsPerPage(page, count)
        goBack()
    }
}
