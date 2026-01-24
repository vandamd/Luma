package app.luma.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import app.luma.R
import app.luma.data.AppModel
import app.luma.data.Prefs
import app.luma.style.SettingsTheme
import app.luma.ui.compose.SettingsComposable.ContentContainer
import app.luma.ui.compose.SettingsComposable.SettingsHeader
import kotlinx.coroutines.launch

class RenameFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: android.os.Bundle?,
    ): View = composeView { RenameContent() }

    @Composable
    fun RenameContent() {
        val homePosition = arguments?.getInt("homePosition") ?: 0

        val textState = remember { mutableStateOf("") }
        val focusRequester = remember { FocusRequester() }
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(Unit) {
            coroutineScope.launch {
                focusRequester.requestFocus()
            }
        }

        fun saveAndReturn(
            newName: String,
            homePosition: Int,
        ) {
            val prefs = Prefs.getInstance(requireContext())

            val homeAppModel = prefs.getHomeAppModel(homePosition)
            val updatedAppModel =
                AppModel(
                    appLabel = newName,
                    appPackage = homeAppModel.appPackage,
                    appAlias = newName,
                    appActivityName = homeAppModel.appActivityName,
                    user = homeAppModel.user,
                    key = homeAppModel.key,
                )
            prefs.setHomeAppModel(homePosition, updatedAppModel)

            findNavController().popBackStack(R.id.mainFragment, false)
        }

        Column {
            SettingsHeader(
                title = "Rename",
                onBack = ::goBack,
                onAction = { saveAndReturn(textState.value, homePosition) },
            )

            ContentContainer {
                TextField(
                    value = textState.value,
                    onValueChange = { textState.value = it },
                    modifier =
                        Modifier
                            .focusRequester(focusRequester)
                            .fillMaxWidth()
                            .padding(end = 37.dp),
                    textStyle =
                        TextStyle(
                            fontSize = 27.sp,
                            color = SettingsTheme.typography.item.color,
                        ),
                    singleLine = true,
                    trailingIcon =
                        if (textState.value.isNotEmpty()) {
                            {
                                IconButton(
                                    onClick = { textState.value = "" },
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.close_24px),
                                        contentDescription = "Clear",
                                        tint = SettingsTheme.typography.item.color,
                                    )
                                }
                            }
                        } else {
                            null
                        },
                    keyboardOptions =
                        KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done,
                        ),
                    keyboardActions =
                        KeyboardActions(
                            onDone = {
                                saveAndReturn(textState.value, homePosition)
                            },
                        ),
                    colors =
                        TextFieldDefaults.textFieldColors(
                            textColor = SettingsTheme.typography.item.color,
                            backgroundColor = SettingsTheme.backgroundColor,
                            cursorColor = Color.White,
                            focusedIndicatorColor = SettingsTheme.typography.item.color,
                            unfocusedIndicatorColor = SettingsTheme.typography.item.color,
                        ),
                )
            }
        }
    }
}
