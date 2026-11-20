package com.kotlinapps.pomodoro.feature.stats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kotlinapps.pomodoro.core.designsystem.theme.PomodoroTheme
import com.kotlinapps.pomodoro.core.model.DailyFocus
import com.kotlinapps.pomodoro.core.model.FocusTotals
import com.kotlinapps.pomodoro.resources.*
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun StatsRoute(
    onNavigateUp: () -> Unit,
    viewModel: StatsViewModel = koinViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    StatsScreen(uiState = uiState, onClearHistory = viewModel::onClearHistory, onNavigateUp = onNavigateUp)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StatsScreen(
    uiState: StatsUiState,
    onClearHistory: () -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirmClear by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.statistics)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(painterResource(Res.drawable.ic_arrow_back), stringResource(Res.string.navigate_up))
                    }
                },
                actions = {
                    if (uiState is StatsUiState.Success) {
                        IconButton(onClick = { confirmClear = true }) {
                            Icon(painterResource(Res.drawable.ic_delete), stringResource(Res.string.clear_history))
                        }
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
                StatsUiState.Loading -> CircularProgressIndicator()
                StatsUiState.Empty -> Message(
                    title = stringResource(Res.string.stats_empty_title),
                    body = stringResource(Res.string.stats_empty_body),
                )
                StatsUiState.Error -> Message(title = stringResource(Res.string.stats_error), body = null)
                is StatsUiState.Success -> StatsContent(uiState)
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(Res.string.clear_history_title)) },
            text = { Text(stringResource(Res.string.clear_history_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    onClearHistory()
                }) { Text(stringResource(Res.string.clear)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text(stringResource(Res.string.cancel)) }
            },
        )
    }
}

@Composable
private fun Message(title: String, body: String?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(32.dp),
    ) {
        Icon(
            painter = painterResource(Res.drawable.ic_timer),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp),
        )
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        if (body != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun StatsContent(state: StatsUiState.Success) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val today = FocusTotals(state.today.sessions, state.today.minutes)
            StatTile(stringResource(Res.string.stats_today), today, Modifier.weight(1f))
            StatTile(stringResource(Res.string.stats_this_week), state.week, Modifier.weight(1f))
            StatTile(stringResource(Res.string.stats_all_time), state.allTime, Modifier.weight(1f))
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(Res.string.stats_chart_title), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(16.dp))
                WeekChart(state.days)
            }
        }
    }
}

@Composable
private fun StatTile(label: String, totals: FocusTotals, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(
                pluralStringResource(Res.plurals.minutes_count, totals.minutes, totals.minutes),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                pluralStringResource(Res.plurals.sessions_count, totals.sessions, totals.sessions),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Simple bar chart: one bar per day, height proportional to minutes, labeled with the value. */
@Composable
private fun WeekChart(days: List<DailyFocus>) {
    val max = days.maxOf { it.minutes }.coerceAtLeast(1)
    val description = stringResource(Res.string.stats_chart_description) + ": " +
        days.joinToString { "${it.date.dayOfWeek.shortName()} ${it.minutes}" }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .clearAndSetSemantics { contentDescription = description },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        days.forEachIndexed { index, day ->
            val isToday = index == days.lastIndex
            Column(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom,
            ) {
                Text(
                    day.minutes.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        // A tiny stub keeps empty days visible as a baseline.
                        .height(BarMaxHeight * (day.minutes.toFloat() / max).coerceAtLeast(0.02f))
                        .background(
                            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp),
                        ),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    day.date.dayOfWeek.shortName(),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private val BarMaxHeight = 130.dp

private fun DayOfWeek.shortName(): String = name.take(3).lowercase().replaceFirstChar { it.uppercase() }

@Preview(showBackground = true)
@Composable
private fun StatsScreenPreview() {
    val start = LocalDate(2026, 11, 14)
    val minutes = listOf(50, 0, 75, 100, 25, 125, 50)
    PomodoroTheme(dynamicColor = false) {
        StatsScreen(
            uiState = StatsUiState.Success(
                days = minutes.mapIndexed { i, m -> DailyFocus(start.plus(DatePeriod(days = i)), m / 25, m) },
                allTime = FocusTotals(sessions = 48, minutes = 1200),
            ),
            onClearHistory = {},
            onNavigateUp = {},
        )
    }
}
