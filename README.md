# LearnManager

A simple cross-platform schedule manager for Windows and Android, built with Kotlin Multiplatform and Compose Multiplatform.

LearnManager focuses on one workflow:

```text
Paste → Review → Save → Synchronize → Notify
```

Windows is the primary platform for planning and importing schedules. Android is primarily used to view schedules and receive reminders.

## Core workflow

1. Copy a schedule from Notion, Excel, Google Sheets, or another table.
2. Paste it into LearnManager on Windows.
3. Select the week in which the schedule starts.
4. Review the parsed entries and fix invalid rows.
5. Save and synchronize the schedule.
6. Receive notifications on Windows and Android.

The application should require as few manual steps as possible.

## Input format

The expected input contains four columns:

| Day | Time | Content | Type |
|---|---|---|---|
| T2 | 08:30-10:00 | Cyber Security Certificate | Tự học |
| T2 | 10:15-11:45 | Nhập môn ATTT | Preview |
| T3 | 08:30-10:00 | Phân tích & Thiết kế hệ thống | Preview |

Example pasted text:

```text
T2	08:30-10:00	Cyber Security Certificate	Tự học
T2	10:15-11:45	Nhập môn ATTT	Preview
T3	08:30-10:00	Phân tích & Thiết kế hệ thống	Preview
```

The parser should:

- Accept tab-separated table data.
- Ignore an optional header row.
- Normalize whitespace.
- Validate the day and time range.
- Preserve Unicode and Vietnamese text.
- Show invalid rows before saving.
- Never silently import malformed rows.

## Date and time behavior

A pasted row contains a weekday and time range but not a specific calendar date. During import, the user selects the week in which the schedule starts.

For example, if the selected week starts on `2026-09-21`:

```text
T2 08:30-10:00
```

is converted to an event starting at:

```text
2026-09-21 08:30
```

A schedule entry stores at least:

- Day of week.
- Start time.
- End time.
- Schedule start date.
- Time zone.
- Whether it repeats weekly.

The default time zone is taken from the device during import and saved with the schedule. For Vietnam, this is normally `Asia/Ho_Chi_Minh`.

Both Windows and Android calculate notification times from the saved schedule time zone. The operating system clock on each device triggers the local notification.

## User interface

### Windows

Windows is the primary platform for schedule planning. The desktop application provides:

- Today's schedule.
- Weekly schedule.
- Paste schedule action.
- Import preview and validation.
- Basic schedule editing and deletion.
- Notification settings.
- Synchronization status.
- System tray support.

### Android

The Android application provides:

- Today's schedule.
- Weekly schedule.
- Basic schedule editing and deletion.
- Notification settings.
- Synchronization status.

The interfaces should remain small and straightforward. Importing a pasted schedule is optimized for Windows.

## Notifications

The default reminder is sent 30 minutes before an event starts.

Example:

```text
Upcoming schedule

Introduction to Information Security starts at 10:15.
Type: Preview
```

Notification behavior:

- Notifications work on both Windows and Android.
- Each device schedules its own local notifications.
- The current time is taken from the device's operating system clock.
- The event time is calculated using its saved date, time, and time zone.
- Editing or deleting an event cancels its previous notification.
- Synchronizing changed data reschedules affected notifications.
- Android restores scheduled reminders after a device reboot.
- Android 13 and later requests notification permission.
- Windows runs in the system tray to deliver notifications while the main window is closed.

The initial version only requires one configurable reminder time. Multiple reminders per event are outside the initial scope.

## Synchronization

Schedule data is synchronized between Windows and Android. The synchronization layer transfers schedule data; it does not continuously monitor the time or send every reminder.

After synchronization, each device schedules its own notifications:

```text
Windows updates schedule
        ↓
Shared synchronization layer
        ↓
Android receives updated schedule
        ↓
Each device schedules local notifications
```

The exact synchronization provider will be selected during implementation. It should remain replaceable and separate from the shared schedule domain logic.

## Technology

LearnManager uses:

- Kotlin Multiplatform.
- Compose Multiplatform.
- Jetpack Compose for Android.
- Compose Desktop for Windows.
- Kotlin Coroutines.
- Kotlinx Serialization.
- Shared schedule parsing, validation, and reminder calculations.

Platform-specific implementations are required for:

- Android notifications and permissions.
- Windows toast notifications.
- Android reboot handling.
- Windows system tray integration.
- Platform background execution and application startup behavior.

## Proposed project structure

```text
learn-manager/
├── composeApp/
│   ├── src/commonMain/
│   │   ├── model/
│   │   ├── parser/
│   │   ├── validation/
│   │   ├── data/
│   │   └── ui/
│   ├── src/androidMain/
│   │   └── notification/
│   └── src/desktopMain/
│       ├── notification/
│       └── tray/
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

The exact structure may change during project initialization. Business rules should remain in shared Kotlin code whenever possible.

## Initial scope

The first usable version includes:

- Paste a schedule on Windows.
- Parse tabular text into schedule entries.
- Select the starting week, time zone, and weekly repetition.
- Preview data before importing.
- Display clear validation errors.
- Save the schedule.
- View schedules by day and week.
- Synchronize schedules between Windows and Android.
- Edit and delete schedule entries.
- Notify 30 minutes before an event on both platforms.
- Reschedule notifications when data changes.
- Restore Android notifications after reboot.

## Non-goals

LearnManager is intentionally small. The initial application will not include:

- Drag-and-drop calendars.
- Study statistics.
- Pomodoro timers.
- AI classification.
- Team collaboration.
- Social features.
- Complex task management.
- Multiple calendar integrations.

Features should only be added when they directly improve the core workflow.

## Development status

The initial scope is complete for Windows and Android:

- KMP + Compose Multiplatform project builds with Gradle 9.3.1 (desktop app, Android APK, and a bundled Windows distribution).
- Shared schedule parser accepts `T2  08:30-10:00  Cyber Security Certificate  Tự học` (two spaces between fields) and pasted TSV, with automatic formatting, preview, and validation.
- Day and week views with week navigation, in-progress/next-up badges, and automatic refresh.
- Manual entry editing with a scrollable editor dialog and Vietnamese weekday chips; deletion asks for confirmation.
- Light/dark theme that follows the system; Vietnamese UI text throughout.
- Windows: system tray with branded tray and window icons; closing the window keeps the app running in the tray (first hide shows an explanatory notification); minimum window size enforced.
- Android: adaptive launcher icon (with Android 13+ monochrome variant), POST_NOTIFICATIONS runtime request, exact-alarm opt-in with inexact fallback, boot/package-replace rescheduling, and orphan-alarm cleanup on reschedule.
- State files are written atomically on both platforms.
- Supabase synchronization (push/pull, off by default) reschedules affected notifications after pull.

## Implementation order

1. Initialize the Kotlin Multiplatform and Compose Multiplatform project. ✅
2. Define the shared schedule model. ✅
3. Implement and test the pasted-text parser. ✅
4. Build the import preview interface. ✅
5. Add local schedule storage. ✅
6. Build the Windows schedule interface. ✅
7. Build the Android schedule interface. ✅
8. Add synchronization. ✅
9. Add Android notifications. ✅
10. Add Windows notifications and system tray support. ✅
11. Test editing, deletion, reboot, time zones, and notification rescheduling. ✅
