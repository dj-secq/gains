# 02 — Architecture & Tech Stack

## Language & UI

- **Kotlin** (latest stable)
- **Jetpack Compose** for all UI — no XML layouts
- **Material 3** components and theming (support light/dark, derive a simple
  custom color scheme rather than default purple — see notes in
  `05_screens_and_navigation.md`)

## Architecture pattern

**MVVM + Repository pattern:**

```
UI (Compose screens)
   ↓ observes
ViewModel (one per screen/feature, exposes StateFlow<UiState>)
   ↓ calls
Repository (one per domain area: WorkoutRepository, SupplementRepository,
            ScheduleRepository, ProgressRepository)
   ↓ uses
Room DAOs + DataStore (for settings/preferences)
```

- Use `StateFlow` / `collectAsStateWithLifecycle()` for UI state.
- Use `Kotlin Coroutines` for all async work.
- Keep business logic (rotation math, progression suggestion, streak
  calculation) in plain Kotlin classes/use-cases, NOT in Composables or DAOs,
  so it's testable.

## Key libraries

- `androidx.room` — local database
- `androidx.datastore:datastore-preferences` — settings (reminder times,
  units, cycle start date, notification toggles)
- `androidx.navigation:navigation-compose` — screen navigation
- `androidx.work:work-runtime-ktx` (WorkManager) — daily reminders
- `androidx.compose.material3` — UI
- A simple Compose-native charting approach: either a lightweight chart
  library (e.g. Vico) or hand-rolled Canvas-based line/bar charts if a
  dependency isn't desired. Either is fine — pick one and be consistent.
- Dependency injection: keep it simple. Either **Hilt** (standard, more
  boilerplate) or manual constructor injection via a simple
  `AppContainer`/`ServiceLocator` class (less boilerplate, fine for an app
  this size). Recommend manual DI unless Codex judges Hilt meaningfully
  cleaner — either is acceptable.

## Project structure (suggested)

```
app/
 └─ src/main/java/.../repsandgrams/
     ├─ data/
     │   ├─ db/            (Room database, entities, DAOs)
     │   ├─ datastore/     (settings/preferences)
     │   └─ repository/    (repository implementations)
     ├─ domain/
     │   ├─ schedule/      (rotation calculation logic)
     │   ├─ progression/   (progression suggestion logic)
     │   └─ streak/        (streak + adherence calculation)
     ├─ ui/
     │   ├─ today/         (Home/Today screen)
     │   ├─ session/       (Active workout session screen)
     │   ├─ calendar/      (Calendar/history screen)
     │   ├─ progress/      (Charts screen)
     │   ├─ supplements/   (Whey/creatine log + supply tracker)
     │   ├─ settings/      (Program editor + reminder settings)
     │   ├─ components/    (shared composables)
     │   └─ theme/          (Material 3 theme)
     ├─ reminders/          (WorkManager workers)
     └─ MainActivity.kt / App.kt
```

## Minimum SDK

Target a recent stable Android version; set `minSdk` around API 26+
(covers the vast majority of active devices) and `targetSdk`/`compileSdk`
to the latest stable at build time.

## Permissions needed

- `POST_NOTIFICATIONS` (Android 13+) — for reminders
- `VIBRATE` — for rest-timer end alerts
- No internet, no location, no storage permissions needed for v1.
