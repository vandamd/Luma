package app.luma.ui

import SettingsTheme
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import kotlinx.coroutines.launch

import app.luma.R
import app.luma.data.AppModel
import app.luma.data.Prefs
import app.luma.ui.compose.SettingsComposable.SettingsHeader
import app.luma.ui.compose.SettingsComposable.ContentContainer

class RenameFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val compose = ComposeView(requireContext())
        compose.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        compose.setContent {
            val isDark = when (Prefs(requireContext()).appTheme) {
                app.luma.data.Constants.Theme.Light -> false
                app.luma.data.Constants.Theme.Dark -> true
                app.luma.data.Constants.Theme.System -> isSystemInDarkTheme()
            }
            SettingsTheme(isDark) {
                RenameContent()
            }
        }
        return compose
    }

    @Composable
    fun RenameContent() {
        // Get arguments passed from AppDrawerFragment
        val homePosition = arguments?.getInt("homePosition") ?: 0
        
        val textState = remember { mutableStateOf("") }
        val focusRequester = remember { FocusRequester() }
        val coroutineScope = rememberCoroutineScope()
        
        // Request focus when the composable is first displayed
        androidx.compose.runtime.LaunchedEffect(Unit) {
            coroutineScope.launch {
                focusRequester.requestFocus()
            }
        }
        
        fun saveAndReturn(newName: String, homePosition: Int) {
            val prefs = Prefs(requireContext())
            
            // Update the home app model - for consistency, update both appLabel and appAlias
            // This ensures the rename works regardless of which renaming method was used
            val homeAppModel = prefs.getHomeAppModel(homePosition)
            val updatedAppModel = AppModel(
                appLabel = newName,
                appPackage = homeAppModel.appPackage,
                appAlias = newName,  // Also update the alias to ensure consistency
                appActivityName = homeAppModel.appActivityName,
                user = homeAppModel.user,
                key = homeAppModel.key
            )
            prefs.setHomeAppModel(homePosition, updatedAppModel)
            
            // Navigate back to home screen
            findNavController().popBackStack(R.id.mainFragment, false)
        }
        
        Column {
            SettingsHeader(title = "Rename", onBack = { requireActivity().onBackPressedDispatcher.onBackPressed() }, onAction = {
                saveAndReturn(textState.value, homePosition)
            })
            
            ContentContainer {
                TextField(
                    value = textState.value,
                    onValueChange = { textState.value = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 24.sp,
                        color = SettingsTheme.typography.item.color
                    ),
                    singleLine = true,
                    trailingIcon = if (textState.value.isNotEmpty()) {
                        {
                            androidx.compose.material.IconButton(
                                onClick = { textState.value = "" }
                            ) {
                                androidx.compose.material.Icon(
                                    painter = androidx.compose.ui.res.painterResource(id = R.drawable.close_24px),
                                    contentDescription = "Clear",
                                    tint = SettingsTheme.typography.item.color
                                )
                            }
                        }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            saveAndReturn(textState.value, homePosition)
                        }
                    ),
                    colors = TextFieldDefaults.textFieldColors(
                        textColor = SettingsTheme.typography.item.color,
                        backgroundColor = SettingsTheme.color.settings,
                        cursorColor = androidx.compose.ui.graphics.Color.White,
                        focusedIndicatorColor = SettingsTheme.typography.item.color,
                        unfocusedIndicatorColor = SettingsTheme.typography.item.color
                    )
                )
            }
        }
    }
}
