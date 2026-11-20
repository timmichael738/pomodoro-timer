# Pomodoro Timer

A focus timer built on the Pomodoro technique, for **Android and iOS**.
Built with Kotlin Multiplatform and Compose Multiplatform: one shared codebase for UI, logic, and storage.

## Features

- Focus → short break → focus …, with a long break after every N focus sessions
- Start, pause/resume, reset, and skip; an animated progress ring and cycle dots show where you are
- **Accurate in the background:** the timer stores its end time, so a locked phone or a killed app never loses time
- **Phase-end notifications** delivered by the OS, even when the app is closed (no foreground service needed)
- Configurable focus, short-break and long-break lengths, sessions per cycle, and auto-start for breaks and focus
- Statistics: today, last 7 days, and all-time focus minutes, plus a 7-day bar chart; clear history with confirmation
- Light / Dark / System theme, plus Material You dynamic color on Android 12+
- Fully offline, with screen-reader labels and accessible touch targets

## Tech stack

| Concern | Library |
|---|---|
| UI | Compose Multiplatform, Material 3, Canvas (progress ring) |
| Architecture | MVVM, Unidirectional Data Flow, Repository pattern |
| DI | Koin |
| Timer | Kotlin Coroutines & Flow, `kotlin.time` |
| Notifications | AlarmManager + BroadcastReceiver (Android), UNUserNotificationCenter (iOS) behind `expect`/`actual` |
| Persistence | Room (multiplatform) for session history; Preferences DataStore for settings and timer state |
| Dates | kotlinx-datetime |
| Navigation | Navigation Compose (multiplatform), type-safe destinations |
| Testing | kotlin.test, kotlinx-coroutines-test (virtual time), Turbine (run on Android JVM and iOS simulator) |

## Highlights

| Concept | Where |
|---|---|
| **App-scoped timer engine**: wall-clock end time, persisted and restored, survives navigation and process death | `core/timer/PomodoroTimer.kt` |
| **Pure cycle rules** with unit tests | `core/domain/PomodoroCycle.kt` |
| **Coroutine timers tested with virtual time** (a test clock tied to the coroutine scheduler) | `commonTest/.../PomodoroTimerTest.kt`, `testing/Fakes.kt` |
| **`expect`/`actual` notifications** and permission prompts per platform | `core/notification/`, `androidMain/`, `iosMain/` |
| **Catching up after suspension** (`LifecycleResumeEffect` → `refresh()`) | `feature/timer/TimerScreen.kt` |
| **Atomic DataStore read-modify-write** for settings | `core/data/repository/DataStoreUserPreferencesRepository.kt` |
| **Per-day aggregation in the user's time zone** | `core/domain/FocusStats.kt` |

## How the timer stays accurate

```
start() ──▶ state = Running(endsAt = now + remaining) ──▶ saved to DataStore
   │
   ├─▶ OS notification scheduled for endsAt   (fires even if the app is killed)
   └─▶ coroutine delay(endsAt - now) ──▶ finishPhase() ──▶ record session (Room), next phase
                                                ▲
          app resumes / relaunches ─▶ refresh() / restore(): endsAt already passed? finish now
```

The UI never counts down on its own: it recomputes `endsAt - now` on every tick, so there is no drift.

## Architecture

```
Compose screens ──▶ ViewModels ──▶ PomodoroTimer (app singleton) ──▶ TimerStateStore (DataStore)
                        │                    │                  ├──▶ PhaseNotifier (expect/actual)
                        │                    │                  └──▶ FocusSessionRepository ──▶ Room
                        └─▶ UserPreferencesRepository ─────────────▶ DataStore<Preferences>
```

## Project structure

```
shared/                              # Kotlin Multiplatform module: everything shared
├── src/commonMain/kotlin/.../
│   ├── App.kt, AppViewModel.kt      # Root composable; applies the saved theme
│   ├── di/                          # Koin modules + initKoin()
│   ├── navigation/                  # NavHost
│   ├── core/model | domain | timer | time | notification | database | data | designsystem
│   └── feature/timer | stats | settings
├── src/commonMain/composeResources/ # Strings, plurals, vector icons
├── src/androidMain/                 # AlarmManager notifier, permission prompt, file paths, dynamic color
├── src/iosMain/                     # Local notifications, file paths, MainViewController
└── src/commonTest/                  # Engine, domain and ViewModel tests (run on Android + iOS)
androidApp/                          # Android launcher: Application, MainActivity, manifest (permissions, receiver)
iosApp/                              # iOS launcher: SwiftUI app hosting the shared UI
```

## Permissions

| Platform | Permission | Why |
|---|---|---|
| Android 13+ | `POST_NOTIFICATIONS` | Phase-end alerts; requested the first time you press Start |
| Android 13+ | `USE_EXACT_ALARM` | Alerts arrive on time (allowed for timer apps); falls back to inexact alarms if unavailable |
| iOS | Notification authorization | Phase-end alerts; requested the first time you press Start |

Declining notifications only disables the alerts: the timer itself keeps working.

## Build and run

**Requirements:** JDK 17, Android Studio (latest stable) with Android SDK Platform 37.1.
iOS additionally needs a Mac with Xcode 16+.

### Android

```bash
./gradlew :androidApp:installDebug            # install on a connected device/emulator
./gradlew :shared:testAndroidHostTest         # shared unit tests on the JVM
```

### iOS (macOS only)

```bash
./gradlew :shared:iosSimulatorArm64Test       # shared unit tests on the iOS simulator
open iosApp/iosApp.xcodeproj                  # then press Run in Xcode
```

To run on a physical iPhone, set `TEAM_ID` in `iosApp/Configuration/Config.xcconfig`.

CI builds and tests both platforms on every push (`.github/workflows/ci.yml`).

## Possible next steps

- Ongoing notification with a live countdown (Android) and a Live Activity (iOS)
- Custom alert sounds and vibration patterns
- Task labels per focus session
- Home-screen widgets (Glance on Android, WidgetKit on iOS)
