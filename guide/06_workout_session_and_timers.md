# 06 — Workout Session Engine & Timers

This is the most interaction-heavy part of the app. Get this right first
after core data (see `09_build_roadmap.md`).

## Starting a session

1. `ScheduleRepository` computes today's slot from `cycleStartDate` +
   `LocalDate.now()` modulo 5:
   - offset 0 → Workout A
   - offset 1 → Rest
   - offset 2 → Workout B
   - offset 3 → Rest
   - offset 4 → Rest
2. If today is a workout day, "Start Workout" creates a new `WorkoutSession`
   row (templateId set, startTime = now, completed = false) and navigates
   to the Session screen with that session's blocks loaded in order:
   Warm-Up → Superset A → Superset B → Superset C → Optional Core (if not skipped).

## Session timer

- Starts counting up from `startTime`.
- Persistent small display throughout the session.
- At 35 minutes (5 min before the 40-min soft cap), show a subtle warning
  ("5 min left — consider skipping Optional Core").
- This is a soft limit, not enforced — never block the user from continuing.

## Superset / block flow

For a block with N exercises and R target rounds:

1. Show exercise 1 of the block → user performs it → logs reps (+weight if
   applicable) via a simple stepper/number input (large touch targets,
   default pre-filled to last session's reps for that exercise as a
   starting guess).
2. Immediately show exercise 2 of the block (no rest between exercises
   *within* a round of a superset — that's the point of a superset).
3. After the last exercise in the round is logged, **auto-start the rest
   timer** using the block's `restSecondsBetweenRounds`.
4. When the rest timer ends (or user taps "Skip Rest"), advance to round+1,
   back to exercise 1 of the block.
5. After the final round of the block, advance to the next block.
6. Warm-Up block: no rest timer needed between warm-up items — just a
   simple checklist advance.
7. Optional Core block: shown after Superset C, with a visible "Skip" button
   (matches the source program's "skip core first if near 40 min" rule).

## Rest timer

- Large countdown numeral, auto-starts as described above.
- Vibrate + optional sound when it hits 0.
- Controls: **+15s**, **Skip**, and it should keep running if the screen
  locks (use a foreground service or reliable coroutine + notification with
  a live countdown, so the timer doesn't die if the phone sleeps).
- Show which exercise/round is coming up next while resting, so the user
  doesn't have to re-orient after the rest.

## Set logging inputs

- Reps: number stepper (tap +/-, or type)
- Weight: number stepper in kg or lb per user's unit setting, in reasonable
  increments (e.g. 1 kg / 2.5 lb steps), only shown for weighted exercises
- Bodyweight-only exercises (Pull-Ups, Push-Ups, Chin-Ups, Bulgarian Split
  Squat, etc.) skip the weight field
- `perSide` exercises should make it clear reps are per arm/leg in the UI copy

## Ending a session

- "End Workout" (manual, always available) or auto-prompt after the last
  block finishes.
- Sets `endTime`, `durationSeconds`, `completed = true`.
- Show a session summary: total sets logged, duration vs. 40-min target,
  and any progression suggestions (below).
- From the summary, prompt to log the post-workout whey/creatine (pre-filled
  per the program default — 1 whey serving + 5g creatine — single tap to
  confirm, matching `04_seed_data.md`).

## Progression suggestion logic (`domain/progression`)

For each exercise in today's session, look at **the most recent prior
session where this exercise was performed** (same exercise, most recent
`WorkoutSession` with matching template before today):

- If **every round's logged reps** in that prior session were at or above
  `targetRepsHigh` for that exercise, mark it as **"Ready to increase
  weight"** and show that badge on the exercise card *before* the user logs
  today's set, so they know to bump the dumbbell.
- This is advisory only — never auto-change the target rep range or weight;
  just surface the suggestion. The user manually updates their working
  weight when they choose to (the app can optionally remember "last weight
  used" per exercise as a convenience field, prefilled next time).
- Bodyweight exercises (Pull-Ups, Chin-Ups, Push-Ups) follow the same rule
  but the "increase" suggestion for these can instead suggest reducing
  assistance / adding reps / adding a pause, since there's no weight to add
  — keep the suggestion copy generic ("Ready for more challenge") for
  exercises with no `weightKg` logged.

## Edge cases to handle

- User skips a scheduled workout day entirely (never opens the app) — the
  next time they open it, Today screen should just show whatever slot
  today actually is; don't force makeup logic unless the user chooses to
  reschedule via Calendar.
- User does a workout out of order (e.g. does B when A was scheduled) —
  allow it; let them pick "Start Workout A" or "Start Workout B" manually
  from Today via a secondary "switch workout" option, don't hard-lock to
  the computed slot.
- App killed mid-session — session should be resumable (persist current
  block/round/rest-timer state, e.g. in DataStore or a lightweight
  in-progress table) so reopening the app returns to the same spot.
