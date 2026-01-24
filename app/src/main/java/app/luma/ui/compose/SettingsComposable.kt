package app.luma.ui.compose

import SettingsTheme
import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterStart
import androidx.compose.ui.Alignment.Companion.TopEnd
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.Dimension
import app.luma.R
import app.luma.data.Constants
import app.luma.helper.performHapticFeedback
import app.luma.style.CORNER_RADIUS

object SettingsComposable {
    @Composable
    fun SettingsHeader(
        title: String,
        onBack: () -> Unit = {},
        onAction: (() -> Unit)? = null,
    ) {
        val context = LocalContext.current
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(SettingsTheme.backgroundColor, SettingsTheme.shape)
                    .padding(horizontal = 7.dp, vertical = 0.dp),
        ) {
            Image(
                painter = painterResource(id = R.drawable.arrow_back_ios_new_24px),
                contentDescription = "Back",
                modifier =
                    Modifier
                        .size(38.dp)
                        .padding(top = 10.dp, bottom = 0.dp)
                        .clickable {
                            performHapticFeedback(context)
                            onBack()
                        },
                colorFilter =
                    androidx.compose.ui.graphics.ColorFilter
                        .tint(SettingsTheme.typography.title.color),
            )
            Spacer(modifier = Modifier.width(16.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                Text(text = title, style = SettingsTheme.typography.title, modifier = Modifier.padding(top = 10.dp, bottom = 25.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            if (onAction != null) {
                Image(
                    painter = painterResource(id = R.drawable.check_24px),
                    contentDescription = "Save",
                    modifier =
                        Modifier
                            .size(38.dp)
                            .padding(top = 10.dp, bottom = 0.dp)
                            .clickable {
                                performHapticFeedback(context)
                                onAction()
                            },
                    colorFilter =
                        androidx.compose.ui.graphics.ColorFilter
                            .tint(SettingsTheme.typography.title.color),
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.arrow_back_ios_new_24px),
                    contentDescription = null,
                    modifier =
                        Modifier
                            .size(38.dp)
                            .padding(top = 10.dp, bottom = 0.dp),
                    alpha = 0f,
                    colorFilter =
                        androidx.compose.ui.graphics.ColorFilter
                            .tint(SettingsTheme.typography.title.color),
                )
            }
        }
    }

    @Composable
    fun ToggleTextButton(
        title: String,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        onClick: () -> Unit,
        fontSize: TextUnit = TextUnit.Unspecified,
    ) {
        Row(
            modifier =
                Modifier
                    .padding(top = 8.dp, bottom = 0.dp)
                    .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CustomToggleSwitch(checked = checked, onCheckedChange = onCheckedChange)
            Spacer(modifier = Modifier.width(12.dp))
            SimpleTextButton(title = title, fontSize = fontSize, onClick = onClick)
        }
    }

    @Composable
    fun CustomToggleSwitch(
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        enabled: Boolean = true,
    ) {
        val context = LocalContext.current
        val circleDiameter = 9.8.dp
        val circleBorder = 2.5.dp
        val lineWidth = 14.5.dp
        val lineHeight = 2.22.dp

        val switchColor = if (enabled) SettingsTheme.typography.title.color else Color.Gray

        Row(
            modifier =
                Modifier
                    .clickable(enabled = enabled) {
                        performHapticFeedback(context)
                        onCheckedChange(!checked)
                    }.padding(8.5.dp, 10.dp, 20.dp, 0.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (!checked) {
                Box(
                    modifier =
                        Modifier
                            .size(circleDiameter)
                            .border(circleBorder, switchColor, CircleShape),
                )
                Box(
                    modifier =
                        Modifier
                            .width(lineWidth)
                            .height(lineHeight)
                            .background(switchColor),
                )
            } else {
                Box(
                    modifier =
                        Modifier
                            .width(lineWidth)
                            .height(lineHeight)
                            .background(switchColor),
                )
                Box(
                    modifier =
                        Modifier
                            .size(circleDiameter)
                            .background(switchColor, CircleShape),
                )
            }
        }
    }

    @Composable
    fun ContentContainer(
        verticalArrangement: Arrangement.Vertical = Arrangement.Top,
        content: @Composable ColumnScope.() -> Unit,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(top = 0.dp, bottom = 20.dp, start = 37.dp, end = 0.dp),
            verticalArrangement = verticalArrangement,
        ) {
            content()
        }
    }

    @Composable
    fun SimpleTextButton(
        title: String,
        fontSize: TextUnit = TextUnit.Unspecified,
        underline: Boolean = false,
        onClick: () -> Unit,
    ) {
        val context = LocalContext.current
        val underlineColor = SettingsTheme.typography.pageButton.color
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 0.dp)) {
            Text(
                title,
                style = SettingsTheme.typography.pageButton,
                fontSize = fontSize,
                modifier =
                    Modifier
                        .align(Alignment.CenterStart)
                        .clickable {
                            performHapticFeedback(context)
                            onClick()
                        }.then(
                            if (underline) {
                                Modifier.drawBehind {
                                    val strokeWidth = 2.dp.toPx()
                                    val y = size.height + 2.dp.toPx()
                                    drawLine(
                                        color = underlineColor,
                                        start = Offset(0f, y),
                                        end = Offset(size.width, y),
                                        strokeWidth = strokeWidth,
                                    )
                                }
                            } else {
                                Modifier
                            },
                        ),
            )
        }
    }

    @Composable
    fun SelectorButton(
        label: String,
        value: String,
        isSelected: Boolean = false,
        onClick: () -> Unit,
    ) {
        val context = LocalContext.current
        val selectedColor = SettingsTheme.typography.button.color
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 0.dp)
                    .clickable {
                        performHapticFeedback(context)
                        onClick()
                    },
            horizontalAlignment = Alignment.Start,
        ) {
            Text(
                label,
                style = SettingsTheme.typography.item,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 4.dp),
            )
            Text(
                value,
                style = SettingsTheme.typography.item,
                fontSize = 30.sp,
                modifier =
                    Modifier
                        .padding(bottom = 0.dp)
                        .then(
                            if (isSelected) {
                                Modifier.drawBehind {
                                    val strokeWidth = 2.dp.toPx()
                                    val y = size.height - 5.dp.toPx()
                                    drawLine(
                                        color = selectedColor,
                                        start = Offset(0f, y),
                                        end = Offset(size.width, y),
                                        strokeWidth = strokeWidth,
                                    )
                                }
                            } else {
                                Modifier
                            },
                        ),
            )
        }
    }

    @Composable
    fun ToggleSelectorButton(
        label: String,
        value: String,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit,
        onClick: () -> Unit,
        enabled: Boolean = true,
    ) {
        val context = LocalContext.current
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 0.dp)
                    .clickable(enabled = enabled) {
                        performHapticFeedback(context)
                        onClick()
                    },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CustomToggleSwitch(
                checked = checked,
                onCheckedChange = if (enabled) onCheckedChange else { _ -> },
                enabled = enabled,
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start,
            ) {
                Text(
                    label,
                    style = SettingsTheme.typography.item,
                    fontSize = 30.sp,
                    color = if (enabled) Color.Unspecified else Color.Gray,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                Text(
                    value,
                    style = SettingsTheme.typography.item,
                    fontSize = 16.sp,
                    color = if (enabled) Color.Unspecified else Color.Gray,
                    modifier = Modifier.padding(top = 0.dp),
                )
            }
        }
    }
}
