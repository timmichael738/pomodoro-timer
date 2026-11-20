package com.kotlinapps.pomodoro.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kotlinapps.pomodoro.core.designsystem.theme.PomodoroTheme
import com.kotlinapps.pomodoro.core.designsystem.theme.supportsDynamicColor
import com.kotlinapps.pomodoro.core.model.ThemeMode
import com.kotlinapps.pomodoro.core.model.UserPreferences
import com.kotlinapps.pomodoro.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun SettingsRoute(
    onNavigateUp: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        uiState = uiState,
        showDynamicColorOption = supportsDynamicColor,
        actions = SettingsActions(
            onThemeModeChange = viewModel::onThemeModeChange,
            onDynamicColorChange = viewModel::onDynamicColorChange,
            onAdjust = viewModel::onAdjust,
            onAutoStartBreaksChange = viewModel::onAutoStartBreaksChange,
            onAutoStartFocusChange = viewModel::onAutoStartFocusChange,
        ),
        onNavigateUp = onNavigateUp,
    )
}

internal class SettingsActions(
    val onThemeModeChange: (ThemeMode) -> Unit = {},
    val onDynamicColorChange: (Boolean) -> Unit = {},
    val onAdjust: (TimerLength, Int) -> Unit = { _, _ -> },
    val onAutoStartBreaksChange: (Boolean) -> Unit = {},
    val onAutoStartFocusChange: (Boolean) -> Unit = {},
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsScreen(
    uiState: SettingsUiState,
    showDynamicColorOption: Boolean,
    actions: SettingsActions,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.settings)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(painterResource(Res.drawable.ic_arrow_back), stringResource(Res.string.navigate_up))
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (uiState) {
                SettingsUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is SettingsUiState.Success -> SettingsContent(uiState.preferences, showDynamicColorOption, actions)
            }
        }
    }
}

@Composable
private fun SettingsContent(
    preferences: UserPreferences,
    showDynamicColorOption: Boolean,
    actions: SettingsActions,
) {
    val timer = preferences.timerSettings
    Column(Modifier.verticalScroll(rememberScrollState())) {
        SectionHeader(stringResource(Res.string.timer_label))
        TimerLength.entries.forEach { length ->
            val label = stringResource(
                when (length) {
                    TimerLength.FOCUS -> Res.string.focus_length
                    TimerLength.SHORT_BREAK -> Res.string.short_break_length
                    TimerLength.LONG_BREAK -> Res.string.long_break_length
                    TimerLength.SESSIONS -> Res.string.sessions_until_long_break
                },
            )
            val value = length.valueIn(timer)
            StepperItem(
                label = label,
                valueText = if (length == TimerLength.SESSIONS) {
                    value.toString()
                } else {
                    stringResource(Res.string.minutes_value, value)
                },
                canDecrease = value > length.range.first,
                canIncrease = value < length.range.last,
                // Minutes move in steps of 5 above 10 so long durations are quick to reach.
                onDecrease = { actions.onAdjust(length, -stepFor(length, value, increasing = false)) },
                onIncrease = { actions.onAdjust(length, stepFor(length, value, increasing = true)) },
            )
        }
        SwitchItem(
            title = stringResource(Res.string.auto_start_breaks),
            summary = stringResource(Res.string.auto_start_breaks_summary),
            checked = timer.autoStartBreaks,
            onCheckedChange = actions.onAutoStartBreaksChange,
        )
        SwitchItem(
            title = stringResource(Res.string.auto_start_focus),
            summary = stringResource(Res.string.auto_start_focus_summary),
            checked = timer.autoStartFocus,
            onCheckedChange = actions.onAutoStartFocusChange,
        )
        Text(
            text = stringResource(Res.string.settings_apply_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )

        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        SectionHeader(stringResource(Res.string.theme_label))
        Column(Modifier.selectableGroup()) {
            ThemeMode.entries.forEach { mode ->
                val selected = mode == preferences.themeMode
                ListItem(
                    headlineContent = {
                        Text(
                            stringResource(
                                when (mode) {
                                    ThemeMode.SYSTEM -> Res.string.theme_system
                                    ThemeMode.LIGHT -> Res.string.theme_light
                                    ThemeMode.DARK -> Res.string.theme_dark
                                },
                            ),
                        )
                    },
                    leadingContent = { RadioButton(selected = selected, onClick = null) },
                    modifier = Modifier.selectable(
                        selected = selected,
                        role = Role.RadioButton,
                        onClick = { actions.onThemeModeChange(mode) },
                    ),
                )
            }
        }
        if (showDynamicColorOption) {
            SwitchItem(
                title = stringResource(Res.string.dynamic_color),
                summary = stringResource(Res.string.dynamic_color_summary),
                checked = preferences.useDynamicColor,
                onCheckedChange = actions.onDynamicColorChange,
            )
        }
    }
}

internal fun stepFor(length: TimerLength, value: Int, increasing: Boolean): Int {
    if (length == TimerLength.SESSIONS) return 1
    return if (if (increasing) value >= 10 else value > 10) 5 else 1
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}

@Composable
private fun StepperItem(
    label: String,
    valueText: String,
    canDecrease: Boolean,
    canIncrease: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(label) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDecrease, enabled = canDecrease) {
                    Icon(painterResource(Res.drawable.ic_remove), stringResource(Res.string.decrease, label))
                }
                Text(
                    valueText,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(min = 56.dp),
                )
                IconButton(onClick = onIncrease, enabled = canIncrease) {
                    Icon(painterResource(Res.drawable.ic_add), stringResource(Res.string.increase, label))
                }
            }
        },
    )
}

@Composable
private fun SwitchItem(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(summary) },
        trailingContent = { Switch(checked = checked, onCheckedChange = null) },
        modifier = Modifier.toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange),
    )
}

@Preview(showBackground = true)
@Composable
private fun SettingsScreenPreview() {
    PomodoroTheme(dynamicColor = false) {
        SettingsScreen(
            uiState = SettingsUiState.Success(UserPreferences(themeMode = ThemeMode.DARK)),
            showDynamicColorOption = true,
            actions = SettingsActions(),
            onNavigateUp = {},
        )
    }
}
