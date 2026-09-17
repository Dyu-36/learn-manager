# LearnManager development

## Requirements

- JDK 17 or newer.
- Android SDK 36 for Android builds.
- Gradle 8.13 if the wrapper JAR has not been generated locally yet.

The project uses Kotlin 2.4.20, Compose Multiplatform 1.12.0 and Android Gradle Plugin 8.13.2.

## First local run

If `gradle/wrapper/gradle-wrapper.jar` is missing, run once with a locally installed Gradle:

```bash
gradle wrapper --gradle-version 8.13
```

Then:

```bash
./gradlew :composeApp:run
./gradlew :composeApp:assembleDebug
./gradlew :composeApp:allTests
```

On Windows use `gradlew.bat` after generating the wrapper.

The debug APK is produced under `composeApp/build/outputs/apk/debug/`.

## Android notification behavior

- Android 13+ asks for `POST_NOTIFICATIONS` on first launch.
- Android 12+ can grant exact-alarm access from Settings inside LearnManager.
- If exact-alarm access is not granted, the app falls back to an inexact alarm so reminders still work but can be late.
- Weekly reminders schedule the next alarm after the current reminder fires.
- Alarms are restored after reboot or app replacement.

## Optional Supabase sync

Local storage and notifications work without any backend. Sync is disabled by default.

Create this table in Supabase:

```sql
create table if not exists public.learn_manager_state (
  user_key text primary key,
  payload text not null
);

alter table public.learn_manager_state enable row level security;

create policy "learn manager anon read"
on public.learn_manager_state
for select
to anon
using (true);

create policy "learn manager anon insert"
on public.learn_manager_state
for insert
to anon
with check (true);

create policy "learn manager anon update"
on public.learn_manager_state
for update
to anon
using (true)
with check (true);
```

Then enter the Supabase project URL, anon key, and the same sync code on Windows and Android. This MVP treats the sync code as the row key and does not provide end-to-end encryption; do not store secrets in schedule text if the Supabase policy is left open as above.

For personal use, tighten Row Level Security before exposing the project publicly.

## Storage paths

- Android: app-private `filesDir/learn_manager_state.json`.
- Desktop: `~/.learn-manager/state.json`.

## TSV import

Expected columns:

```text
Day<TAB>Time<TAB>Content<TAB>Type
T2<TAB>08:30-10:00<TAB>Cyber Security Certificate<TAB>Tự học
```

Malformed rows are shown in the preview and are never silently imported.
