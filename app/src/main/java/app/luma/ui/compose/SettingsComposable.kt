package app.luma.ui.compose

import SettingsTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import app.luma.R
import app.luma.data.Constants
import app.luma.data.EnumOption
import app.luma.style.CORNER_RADIUS

object SettingsComposable {

    @Composable
    fun SettingsHeader(title: String, onBack: () -> Unit = {}, onAction: (() -> Unit)? = null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SettingsTheme.color.settings, SettingsTheme.shapes.settings)
                .padding(horizontal = 7.dp, vertical = 0.dp),
        ) {
            Image(
                painter = painterResource(id = R.drawable.arrow_back_ios_new_24px),
                contentDescription = "Back",
                modifier = Modifier
                    .size(38.dp)
                    .padding(top = 10.dp, bottom = 0.dp)
                    .clickable { onBack() },
                colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(SettingsTheme.typography.title.color)
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
                    modifier = Modifier
                        .size(38.dp)
                        .padding(top = 10.dp, bottom = 0.dp)
                        .clickable { onAction() },
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(SettingsTheme.typography.title.color)
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.arrow_back_ios_new_24px),
                    contentDescription = null,
                    modifier = Modifier
                        .size(38.dp)
                        .padding(top = 10.dp, bottom = 0.dp),
                    alpha = 0f,
                    colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(SettingsTheme.typography.title.color)
                )
            }
        }
    }

    @Composable
    fun SettingsTile(content: @Composable () -> Unit) {
        Column(
            modifier = Modifier
                .padding(12.dp, 12.dp, 12.dp, 0.dp)
                .background(SettingsTheme.color.settings, SettingsTheme.shapes.settings)
                .border(
                    0.5.dp,
                    colorResource(if (SettingsTheme.typography.title.color == app.luma.style.textLight) R.color.white else R.color.black),
                    RoundedCornerShape(CORNER_RADIUS),
                )
                .padding(20.dp)
                .fillMaxWidth()
        ) {
            content()
        }
    }

    @Composable
     fun SettingsArea (
         title: String,
         selected: MutableState<String>,
         items: Array<@Composable (MutableState<Boolean>, (Boolean) -> Unit ) -> Unit>
     ) {        SettingsTile {
             SettingsTitle(text = title)
             items.forEachIndexed { i, item -> item(mutableStateOf("$title-$i" == selected.value)) { b ->
                    val number = if (b) i else -1
                    selected.value = "$title-$number"
                }
            }

        }
    }

    @Composable
      fun SettingsTopView(title: String, onClick: () -> Unit = {}, content: @Composable () -> Unit) {
           SettingsTile {
               Box(modifier = Modifier.fillMaxWidth()) {
                   SettingsTitle(
                       text = title,
                       modifier = Modifier.align(CenterStart)
                   )
                   Image(
                       painter = painterResource(id = R.drawable.close_24px),
                       contentDescription = "Close",
                       modifier = Modifier.align(TopEnd).size(32.dp).clickable { onClick() },
                       colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(SettingsTheme.typography.title.color)
                   )
               }
               Column(modifier = Modifier.fillMaxWidth()) { content() }
           }
      }

    @Composable
    fun SettingsTitle(text: String, modifier: Modifier = Modifier) {
        Text(
            text = text,
            style = SettingsTheme.typography.title,
            modifier = modifier
                .padding(0.dp, 0.dp, 0.dp, 12.dp)
        )
    }

    @Composable
    fun SettingsToggle(
        title: String,
        state: MutableState<Boolean>,
        onChange: (Boolean) -> Unit,
        fontSize: TextUnit = TextUnit.Unspecified,
        onToggle: () -> Unit
    ) {
        val buttonText = if (state.value) stringResource(R.string.on) else stringResource(R.string.off)
        SettingsRow(
            title = title,
            onClick = {
                onChange(false)
                state.value = !state.value
                onToggle()
            },
            fontSize = fontSize,
            buttonText = buttonText
        )
    }

    @Composable
    fun <T: EnumOption> SettingsItem(
        title: String,
        currentSelection: MutableState<T>,
        currentSelectionName: String? = null,
        values: Array<T>,
        open: MutableState<Boolean>,
        active: Boolean = true,
        onChange: (Boolean) -> Unit,
        fontSize: TextUnit = TextUnit.Unspecified,
        onSelect: (T) -> Unit,
    ) {
        if (open.value) {
            Box(
                modifier = Modifier
                    .pointerInput(Unit) {
                        detectTapGestures {
                            onChange(false)
                        }
                    }
                    .onFocusEvent {
                        if (it.isFocused) {
                            onChange(false)
                        }
                    }
            ) {
                SettingsSelector(values, fontSize = fontSize) { i ->
                    onChange(false)
                    currentSelection.value = i
                    onSelect(i)
                }
            }
        } else {
            SettingsRow(
                title = title,
                onClick = { onChange(true) },
                fontSize = fontSize,
                active = active,
                buttonText = currentSelectionName ?: currentSelection.value.string()
            )
        }
    }

    @Composable
    fun SettingsGestureItem(
        title: String,
        open: MutableState<Boolean>,
        onChange: (Boolean) -> Unit,
        currentAction: Constants.Action,
        onSelect: (Constants.Action) -> Unit,
        appLabel: String,
        ) {
        SettingsItem(
            open = open,
            onChange = onChange,
            title = title,
            currentSelection = remember { mutableStateOf(currentAction) },
            currentSelectionName = if (currentAction == Constants.Action.OpenApp) "Open $appLabel" else currentAction.string(),
            values = Constants.Action.values(),
            active = currentAction != Constants.Action.Disabled,
            onSelect = onSelect,
        )
    }

    @Composable
    fun SettingsNumberItem(
        title: String,
        currentSelection: MutableState<Int>,
        min: Int = Int.MIN_VALUE,
        max: Int = Int.MAX_VALUE,
        open: MutableState<Boolean>,
        onChange: (Boolean) -> Unit,
        onValueChange: (Int) -> Unit = {},
        fontSize: TextUnit = TextUnit.Unspecified,
        onSelect: (Int) -> Unit
    ) {
        if (open.value) {
            SettingsNumberSelector(
                number = currentSelection,
                min = min,
                max = max,
                fontSize = fontSize,
                onValueChange = onValueChange,
            ) { i ->
                onChange(false)
                currentSelection.value = i
                onSelect(i)
            }
        } else {
            SettingsRow(
                title = title,
                onClick = { onChange(true) },
                fontSize = fontSize,
                buttonText = currentSelection.value.toString()
            )
        }
    }

    @Composable
    fun SettingsAppSelector(
        title: String,
        currentSelection: MutableState<String>,
        active: Boolean,
        fontSize: TextUnit = TextUnit.Unspecified,
        onClick: () -> Unit,
    ) {
        SettingsRow(
            title = title,
            onClick = onClick,
            buttonText = currentSelection.value,
            active = active,
            fontSize = fontSize,
            disabledText = stringResource(R.string.disabled)
        )
    }

    @Composable
    private fun SettingsRow(
        title: String,
        onClick: () -> Unit,
        buttonText: String,
        active: Boolean = true,
        disabledText: String = buttonText,
        fontSize: TextUnit = TextUnit.Unspecified,
    ) {
        ConstraintLayout(modifier = Modifier.fillMaxWidth()) {

            val (text, button) = createRefs()

            Box(
                modifier = Modifier
                    .constrainAs(text) {
                        start.linkTo(parent.start)
                        end.linkTo(button.start)
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        width = Dimension.fillToConstraints
                    },
            ) {
                Text(
                    title,
                    style = SettingsTheme.typography.item,
                    fontSize = fontSize,
                    modifier = Modifier.align(CenterStart)
                )
            }

            TextButton(
                onClick = onClick,
                modifier = Modifier.constrainAs(button) {
                    end.linkTo(parent.end)
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                },
            ) {
                Text(
                    text = if (active) buttonText else disabledText,
                    style = if (active) SettingsTheme.typography.button else SettingsTheme.typography.buttonDisabled,
                    fontSize = fontSize,
                    modifier = Modifier.testTag("test$buttonText")
                )
            }
        }
    }

    @Composable
    fun ToggleTextButton(title: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit, onClick: () -> Unit, fontSize: TextUnit = TextUnit.Unspecified) {
        Row(modifier = Modifier
            .padding(top = 8.dp, bottom = 0.dp)
            .fillMaxWidth(), 
            verticalAlignment = Alignment.CenterVertically) {
            CustomToggleSwitch(checked = checked, onCheckedChange = onCheckedChange)
            Spacer(modifier = Modifier.width(12.dp))
            SimpleTextButton(title = title, fontSize = fontSize, onClick = onClick)
        }
    }

    @Composable
    fun CustomToggleSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, enabled: Boolean = true) {
        val circleDiameter = 9.8.dp
        val circleBorder = 2.5.dp
        val lineWidth = 14.5.dp
        val lineHeight = 2.22.dp

        val switchColor = if (enabled) SettingsTheme.typography.title.color else Color.Gray

        Row(
            modifier = Modifier
                .clickable(enabled = enabled) { onCheckedChange(!checked) }
                .padding(8.5.dp, 10.dp, 20.dp, 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!checked) {
                Box(
                    modifier = Modifier
                        .size(circleDiameter)
                        .border(circleBorder, switchColor, CircleShape)
                )
                Box(
                    modifier = Modifier
                        .width(lineWidth)
                        .height(lineHeight)
                        .background(switchColor)
                )
            } else {
                Box(
                    modifier = Modifier
                        .width(lineWidth)
                        .height(lineHeight)
                        .background(switchColor)
                )
                Box(
                    modifier = Modifier
                        .size(circleDiameter)
                        .background(switchColor, CircleShape)
                )
            }
        }
    }

    @Composable
    private fun <T: EnumOption> SettingsSelector(options: Array<T>, fontSize: TextUnit = TextUnit.Unspecified, onSelect: (T) -> Unit) {
        Box(
            modifier = Modifier
                .background(SettingsTheme.color.selector, SettingsTheme.shapes.settings)
                .fillMaxWidth()
        ) {
            LazyRow(
                modifier = Modifier
                    .align(Alignment.CenterEnd),
                horizontalArrangement = Arrangement.SpaceEvenly
            )
            {
                for (opt in options) {
                    item {
                        TextButton(
                            onClick = { onSelect(opt) },
                        ) {
                            Text(
                                text = opt.string(),
                                fontSize = fontSize,
                                style = SettingsTheme.typography.button
                            )
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun SettingsNumberSelector(
        number: MutableState<Int>,
        min: Int,
        max: Int,
        fontSize: TextUnit = TextUnit.Unspecified,
        onValueChange: (Int) -> Unit = {},
        onCommit: (Int) -> Unit
    ) {
        ConstraintLayout(
            modifier = Modifier
                .background(SettingsTheme.color.selector, SettingsTheme.shapes.settings)
                .fillMaxWidth()
        ) {
            val (plus, minus, text, button) = createRefs()
            TextButton(
                onClick = {
                    if (number.value > min) {
                        number.value -= 1
                        onValueChange(number.value)
                    }
                },
                modifier = Modifier.constrainAs(minus) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(parent.start)
                    end.linkTo(text.start)
                },
            ) {
                Text("-", style = SettingsTheme.typography.button, fontSize = fontSize)
            }
            Text(
                text = number.value.toString(),
                fontSize = fontSize,
                modifier = Modifier
                    .fillMaxHeight()
                    .constrainAs(text) {
                        top.linkTo(parent.top)
                        bottom.linkTo(parent.bottom)
                        start.linkTo(minus.end)
                        end.linkTo(plus.start)
                    },
                style = SettingsTheme.typography.item,
            )
            TextButton(
                onClick = {
                    if (number.value < max) {
                        number.value += 1
                        onValueChange(number.value)
                    }
                },
                modifier = Modifier.constrainAs(plus) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(text.end)
                    end.linkTo(button.start)
                },
            ) {
                Text("+", style = SettingsTheme.typography.button, fontSize = fontSize)
            }
            TextButton(
                onClick = { onCommit(number.value) },
                modifier = Modifier.constrainAs(button) {
                    top.linkTo(parent.top)
                    bottom.linkTo(parent.bottom)
                    start.linkTo(plus.end)
                    end.linkTo(parent.end)
                },
            ) {
                Text(stringResource(R.string.save), style = SettingsTheme.typography.button, fontSize = fontSize)
            }
        }
    }

    @Composable
    fun ContentContainer(
        verticalArrangement: Arrangement.Vertical = Arrangement.Top,
        content: @Composable ColumnScope.() -> Unit
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 0.dp, bottom = 20.dp, start = 37.dp, end = 0.dp),
            verticalArrangement = verticalArrangement
        ) {
            content()
        }
    }

    @Composable
    fun SimpleTextButton(title: String, fontSize: TextUnit = TextUnit.Unspecified, underline: Boolean = false, onClick: () -> Unit) {
        val underlineColor = SettingsTheme.typography.pageButton.color
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 0.dp)) {
            Text(
                title,
                style = SettingsTheme.typography.pageButton,
                fontSize = fontSize,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .clickable { onClick() }
                    .then(
                        if (underline) Modifier.drawBehind {
                            val strokeWidth = 2.dp.toPx()
                            val y = size.height + 2.dp.toPx()
                            drawLine(
                                color = underlineColor,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = strokeWidth
                            )
                        } else Modifier
                    )
            )
        }
    }

    @Composable
    fun SelectorButton(
        label: String,
        value: String,
        isSelected: Boolean = false,
        onClick: () -> Unit
    ) {
        val selectedColor = SettingsTheme.typography.button.color
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 0.dp)
                .clickable { onClick() },
            horizontalAlignment = Alignment.Start
        ) {
            Text(
                label,
                style = SettingsTheme.typography.item,
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                value,
                style = SettingsTheme.typography.item,
                fontSize = 30.sp,
                modifier = Modifier
                    .padding(bottom = 0.dp)
                    .then(
                        if (isSelected) Modifier.drawBehind {
                            val strokeWidth = 2.dp.toPx()
                            val y = size.height - 5.dp.toPx()
                            drawLine(
                                color = selectedColor,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = strokeWidth
                            )
                        } else Modifier
                    )
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
        enabled: Boolean = true
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 0.dp)
                .clickable(enabled = enabled) { onClick() },
            verticalAlignment = Alignment.CenterVertically
        ) {
            CustomToggleSwitch(
                checked = checked, 
                onCheckedChange = if (enabled) onCheckedChange else { _ -> },
                enabled = enabled
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    label,
                    style = SettingsTheme.typography.item,
                    fontSize = 30.sp,
                    color = if (enabled) Color.Unspecified else Color.Gray,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    value,
                    style = SettingsTheme.typography.item,
                    fontSize = 16.sp,
                    color = if (enabled) Color.Unspecified else Color.Gray,
                    modifier = Modifier.padding(top = 0.dp)
                )
            }
        }
    }
}
