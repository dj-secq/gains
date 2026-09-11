# 07 — Notifications & Reminders

All reminders are local (WorkManager), no push/server needed.

## Reminder types

1. **Workout day reminder** — fires on Workout A/B days only, at a
   user-configurable time (default e.g. 6:00 PM), if that day's session
   hasn't been started yet. Tapping it opens Today → Start Workout.
2. **Creatine reminder** — fires **every day** (workout and rest), at a
   user-configurable time (default e.g. 8:00 PM), if today's
   `SupplementLog.creatineTaken` is still false. Skip firing if already
   logged. Tapping it opens Today with the creatine toggle focused.
3. **Post-workout whey reminder** — only relevant on workout days; rather
   than a fixed clock time, this should fire a short time (e.g. 20–30 min,
   configurable) after a `WorkoutSession` is marked `completed = true`, if
   whey hasn't been logged yet for that date. Implement as a one-shot
   `WorkManager` job scheduled at session-end time, not a daily recurring job.

## Scheduling approach

- Use a **daily recurring WorkManager job** (`PeriodicWorkRequest`, ~15 min
  minimum granularity is a WorkManager constraint — for exact clock times
  prefer `setInitialDelay` recalculated daily, or `AlarmManager` with
  `setExactAndAllowWhileIdle` if precise timing matters more than battery
  friendliness; either is acceptable, but be consistent) that:
  1. Checks today's schedule slot
  2. Checks today's `SupplementLog` and `WorkoutSession` state
  3. Fires the appropriate notification(s) if conditions aren't already met
- The post-workout whey reminder is scheduled separately, one-shot, at the
  moment a session completes (cancel it if the user logs whey manually
  before it fires).
- Re-schedule all recurring jobs on device boot (`BOOT_COMPLETED` receiver)
  and whenever the user changes reminder settings.

## Settings surface (in Settings screen)

- Master toggle: reminders on/off
- Per-reminder toggle + time picker for workout-day and creatine reminders
- Delay picker for post-workout whey reminder (e.g. 15/20/30/45 min)

## Notification content guidelines

- Keep copy short and specific, e.g.:
  - "Workout B is on for today — 40 min, whenever you're ready."
  - "Creatine check — 5g, anytime today."
  - "Nice work — grab your whey when you're ready."
- Respect Android 13+ `POST_NOTIFICATIONS` runtime permission — request it
  contextually (e.g. first time the user enables reminders in Settings),
  not on first app launch.
