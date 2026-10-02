# Gains

Gains is a workout log for one person on one Android phone. Workouts, measurements, and supplement logs stay in a local database. There is no account and no cloud sync.

Version 1.1 (version code 2). Store text for this release is in [store/play-console.md](store/play-console.md).

## What it does

- Today shows the workout to do, the week, the next exercises, and supplements.
- A program can rotate (a workout, then rest, then the next) or assign one workout to each weekday.
- The workout screen stays dark. The keypad starts closed. A rest timer can ring while the screen is off. Leaving the screen keeps the session. Discard is the only way to delete it.
- Calendar and Progress share the same day marks. A trained day is green. A missed due day is red. A personal record is red.
- Progress is one chart, plus bodyweight, measurements, and supply.
- Settings cover theme, units, the catalog, programs, supplements, reminders, plates, and an optional Health Connect write.
- The home screen widget shows today’s suggestion, the next exercise, and the week.
- Backup is a zip you create.

Weights are stored as kilograms and lengths as centimeters, and shown in the unit you choose. The app does not track nutrition, and it has no social feed, ads, or Wear OS app.

## Build

Requirements: Android Studio, JDK 17, and Android SDK 37. The minimum phone version is Android 8.0 (API 26).

1. Open the project in Android Studio.
2. Sync Gradle.
3. Run the `app` configuration on a device or emulator.

A signed release uses `keystore.properties` in this directory. That file is not committed. Without it, the release task still builds, and the output is unsigned.

```bash
./gradlew :app:bundleRelease :app:assembleRelease
```

The signed files for 1.1 are copied to `dist/` after a local release build. `dist/` is gitignored.

## Layout

- `app/` is the Android application.
- `guide/CURRENT.md` is the product spec.
- Font licenses are in `app/src/main/assets/licenses/`.
