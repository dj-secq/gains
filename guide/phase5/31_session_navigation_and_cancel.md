# 31 — Session Back Navigation & True Cancel

Two related gaps: no way to go back to a previous exercise/round to review
or fix something, and no way to bail out of an accidentally-started
workout without it being saved.

## 1. Back navigation within an active session

The session flow (`06_workout_session_and_timers.md`) only ever advances
forward through blocks/rounds/exercises. Add the ability to step backward.

- Add a **"Previous"** control (e.g. a back chevron in the session
  screen's own header, separate from the system back button — see §3 on
  why) that steps the session's internal position pointer back one
  exercise/round at a time, mirroring however "advance" is currently
  implemented.
- Going back should show the **previously logged set for that exercise/
  round with its actual logged values** already filled in (not last
  session's values — this session's own values, since they were already
  logged), editable — if the user changes a number, update the existing
  `SetLog` row rather than inserting a duplicate.
- Going back should **not** restart any rest timer that already
  completed, and should not re-trigger the "beat last time" celebration
  a second time if it already fired.
- Put a reasonable limit on how far back this goes — allow backing up
  within the current session's already-completed steps, but not past the
  start of the session (there's nothing to go back to before it began).

## 2. True cancel — leave without logging anything

Right now starting a session appears to create a committed
`WorkoutSession` row immediately with no way out except finishing it. Fix:

- Add a clearly-separate **"Cancel Workout"** action (distinct from "End
  Workout," which finalizes and saves whatever was logged so far — keep
  both, they serve different purposes).
- Tapping "Cancel Workout" shows a confirmation: *"Discard this workout?
  Nothing from this session will be saved."* with Cancel/Discard actions.
- On confirm: **delete** the in-progress `WorkoutSession` row and any
  `SetLog` rows created during it. This should leave no trace in Calendar,
  Progress, or the streak/suggestion calculation from
  `23_flexible_schedule_engine.md` — as if the session never started.
- This must also correctly stop the session timer and cancel any active
  rest timer / foreground service (`10_rest_timer_alarms.md`) — don't
  leave an orphaned timer running after a cancelled session.

## 3. System back button behavior during a session

This is the actual root cause of "I can't leave" — right now there's
likely no handling for the system/gesture back action while on the
Session screen at all, or it's been disabled outright. Fix properly rather
than just adding an escape hatch elsewhere:

- Intercept the system back gesture/button while on the Session screen.
- If tapped/gestured, show the **same cancel-confirmation** from §2 (don't
  silently discard, and don't silently do nothing — both are bad: one
  loses data unexpectedly, the other traps the user, which is the exact
  bug being fixed here).
- If the user confirms, cancel exactly as in §2. If they back out of the
  confirmation, they remain in the session uninterrupted.
- This means system back and the explicit "Cancel Workout" button
  (§2) can share the same underlying confirmation + discard logic —
  implement it once, trigger it from both places.

## Why this combination matters

Without back-navigation (§1), a fat-fingered log or a Hollow Body Hold
timer stopped early has no fix short of finishing the whole session with
wrong data in it. Without true cancel (§2/§3), an accidental tap on
"Start Workout" locks the user into either finishing a workout they didn't
mean to start or force-quitting the app entirely (which likely leaves a
half-finished session sitting in the database anyway, corrupting streak/
history data). Fix both together since they're both "the session screen
needs an escape hatch that doesn't just mean forward."
