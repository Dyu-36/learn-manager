# LearnManager

Native Android app for managing class schedules and reminding users 30 minutes before each study or work period.

## Development environment

- Kotlin + Jetpack Compose
- Android SDK, Emulator and ADB
- Gradle Wrapper
- Android Lint, JUnit, Compose UI Test and UI Automator
- Maestro for repeatable end-to-end UI tests

## Verification

```powershell
.\gradlew.bat :app:assembleDebug :app:lintDebug :app:testDebugUnitTest
.\gradlew.bat :app:connectedDebugAndroidTest
maestro test .maestro
```

The reminder implementation must test a schedule at `T+30 minutes`, notification permission on Android 13+, exact-alarm access, and rescheduling after a reboot.
