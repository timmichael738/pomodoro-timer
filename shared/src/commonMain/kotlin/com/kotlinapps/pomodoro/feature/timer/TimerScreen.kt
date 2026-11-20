package com.kotlinapps.pomodoro.feature.timer

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kotlinapps.pomodoro.core.designsystem.component.ProgressRing
import com.kotlinapps.pomodoro.core.designsystem.theme.PomodoroTheme
import com.kotlinapps.pomodoro.core.model.TimerPhase
import com.kotlinapps.pomodoro.core.notification.rememberNotificationPermissionRequester
import com.kotlinapps.pomodoro.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Duration.Companion.minutes

@Composable
internal fun TimerRoute(
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: TimerViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val requestNotificationPermission = rememberNotificationPermissionRequester()

    // Coroutine delays don't advance while the app is suspended; catch up when we come back.
    LifecycleResumeEffect(viewModel) {
        viewModel.onResume()
        onPauseOrDispose { }
    }

    TimerScreen(
        uiState = uiState,
        onStartPause = {
            requestNotificationPermission()
            viewModel.onStartPause()
        },
        onReset = viewModel::onReset,
        onSkip = viewModel::onSkip,
        onOpenStats = onOpenStats,
        onOpenSettings = onOpenSettings,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TimerScreen(
    uiState: TimerUiState,
    onStartPause: () -> Unit,
    onReset: () -> Unit,
    onSkip: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.app_name)) },
                actions = {
                    IconButton(onClick = onOpenStats) {
                        Icon(painterResource(Res.drawable.ic_bar_chart), stringResource(Res.string.statistics))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(painterResource(Res.drawable.ic_settings), stringResource(Res.string.settings))
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            when (uiState) {
                TimerUiState.Loading -> CircularProgressIndicator()
                is TimerUiState.Ready -> TimerContent(uiState, onStartPause, onReset, onSkip)
            }
        }
    }
}

@Composable
private fun TimerContent(
    state: TimerUiState.Ready,
    onStartPause: () -> Unit,
    onReset: () -> Unit,
    onSkip: () -> Unit,
) {
    val targetColor = phaseColor(state.phase)
    val color by animateColorAsState(targetColor, label = "phaseColor")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        PhaseIndicator(current = state.phase)
        Spacer(Modifier.height(32.dp))

        val timeText = formatClock(state.remainingSeconds)
        val phaseName = stringResource(state.phase.label)
        val description = stringResource(Res.string.time_remaining_description, timeText, phaseName)
        ProgressRing(
            progress = state.progress,
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier
                .widthIn(max = 320.dp)
                .fillMaxWidth()
                .aspectRatio(1f)
                // One readable label instead of separate digits; deliberately not a live region,
                // which would make screen readers announce every second.
                .clearAndSetSemantics { contentDescription = description },
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = timeText,
                    style = MaterialTheme.typography.displayLarge,
                    fontWeight = FontWeight.Medium,
                )
                val subtitle = when {
                    state.isStarted && !state.isRunning -> stringResource(Res.string.status_paused)
                    state.phase == TimerPhase.FOCUS -> stringResource(
                        Res.string.session_progress,
                        (state.completedInCycle + 1).coerceAtMost(state.sessionsUntilLongBreak),
                        state.sessionsUntilLongBreak,
                    )
                    else -> phaseName
                }
                Text(subtitle, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        Spacer(Modifier.height(24.dp))
        CycleDots(done = state.completedInCycle, total = state.sessionsUntilLongBreak, color = color)
        Spacer(Modifier.height(32.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FilledTonalIconButton(onClick = onReset, enabled = state.isStarted, modifier = Modifier.size(56.dp)) {
                Icon(painterResource(Res.drawable.ic_replay), stringResource(Res.string.reset))
            }
            FilledIconButton(
                onClick = onStartPause,
                modifier = Modifier.size(88.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = color),
            ) {
                Icon(
                    painter = painterResource(if (state.isRunning) Res.drawable.ic_pause else Res.drawable.ic_play),
                    contentDescription = stringResource(
                        when {
                            state.isRunning -> Res.string.pause
                            state.isStarted -> Res.string.resume
                            else -> Res.string.start
                        },
                    ),
                    modifier = Modifier.size(40.dp),
                )
            }
            FilledTonalIconButton(onClick = onSkip, modifier = Modifier.size(56.dp)) {
                Icon(painterResource(Res.drawable.ic_skip_next), stringResource(Res.string.skip))
            }
        }

        Spacer(Modifier.height(32.dp))
        Text(
            text = stringResource(
                Res.string.today_summary,
                pluralStringResource(Res.plurals.sessions_count, state.todaySessions, state.todaySessions),
                pluralStringResource(Res.plurals.minutes_count, state.todayMinutes, state.todayMinutes),
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Read-only indicator of the current phase (use "Skip" to move on). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhaseIndicator(current: TimerPhase) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        TimerPhase.entries.forEachIndexed { index, phase ->
            SegmentedButton(
                selected = phase == current,
                onClick = {},
                enabled = phase == current,
                shape = SegmentedButtonDefaults.itemShape(index, TimerPhase.entries.size),
                icon = {},
                colors = SegmentedButtonDefaults.colors(
                    disabledInactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            ) {
                Text(stringResource(phase.label), maxLines = 1)
            }
        }
    }
}

@Composable
private fun CycleDots(done: Int, total: Int, color: Color) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.semantics(mergeDescendants = true) {},
    ) {
        repeat(total) { index ->
            Box(
                Modifier
                    .size(12.dp)
                    .background(
                        color = if (index < done) color else MaterialTheme.colorScheme.surfaceContainerHighest,
                        shape = CircleShape,
                    ),
            )
        }
    }
}

@Composable
private fun phaseColor(phase: TimerPhase): Color = when (phase) {
    TimerPhase.FOCUS -> MaterialTheme.colorScheme.primary
    TimerPhase.SHORT_BREAK -> MaterialTheme.colorScheme.secondary
    TimerPhase.LONG_BREAK -> MaterialTheme.colorScheme.tertiary
}

internal val TimerPhase.label: StringResource
    get() = when (this) {
        TimerPhase.FOCUS -> Res.string.phase_focus
        TimerPhase.SHORT_BREAK -> Res.string.phase_short_break
        TimerPhase.LONG_BREAK -> Res.string.phase_long_break
    }

/** mm:ss, or h:mm:ss for phases of an hour or more. */
internal fun formatClock(totalSeconds: Long): String {
    val s = totalSeconds.coerceAtLeast(0)
    val hours = s / 3600
    val minutes = (s % 3600) / 60
    val seconds = s % 60
    fun Long.pad() = toString().padStart(2, '0')
    return if (hours > 0) "$hours:${minutes.pad()}:${seconds.pad()}" else "${minutes.pad()}:${seconds.pad()}"
}

@Preview(showBackground = true)
@Composable
private fun TimerScreenPreview() {
    PomodoroTheme(dynamicColor = false) {
        TimerScreen(
            uiState = TimerUiState.Ready(
                phase = TimerPhase.FOCUS,
                isRunning = true,
                isStarted = true,
                remaining = 17.minutes,
                total = 25.minutes,
                completedInCycle = 1,
                sessionsUntilLongBreak = 4,
                todaySessions = 3,
                todayMinutes = 75,
            ),
            onStartPause = {},
            onReset = {},
            onSkip = {},
            onOpenStats = {},
            onOpenSettings = {},
        )
    }
}
