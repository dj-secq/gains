# 11 — During-Workout UI Improvements

Your ask: "more improvement of the during workout UI." This is the screen
you'll actually stare at most, so it's worth the most polish. Builds on
`06_workout_session_and_timers.md` and pulls in `10_rest_timer_alarms.md`.

## 1. "Last time" inline comparison
Every exercise card should show what was logged **last time this exercise
appeared**, right next to the input (e.g. "Last: 10 reps @ 12.5kg") so the
user isn't guessing or context-switching to History. This is one of the
single highest-value features in apps like Strong/Hevy — implement it early.

## 2. Big, glanceable "up next" strip
A persistent small strip (not just the rest-timer callout) showing the
next 1–2 exercises coming up, so the user can mentally prep during the
current set without navigating anywhere.

## 3. Quick weight/rep steppers, not just number pads
Large +/- steppers with sensible increments (reps: ±1, weight: ±1kg / ±2.5lb,
long-press to accelerate) as the primary input, keyboard entry as a
fallback tap-to-type — minimizes fumbling mid-set.

## 4. Warm-up set calculator (optional per exercise)
Before working sets on a weighted exercise, offer an optional warm-up
ramp: given the planned working weight, suggest 2–3 warm-up sets at
~40%/60%/80% of that weight with lower reps. Toggle on/off per exercise or
globally in Settings.

## 5. Plate calculator
For barbell-style weighted exercises (if any get added later) or dumbbell
pairs, show a quick visual of which plates/dumbbells to load for the
target weight, given a configurable available-equipment list (e.g. plate
sizes on hand, dumbbell increments available) set once in Settings.

## 6. RPE / difficulty tag per set (optional, lightweight)
A single-tap 3-option chip after logging a set: "Easy / Right / Too Hard" —
maps loosely to RPE without requiring the user to know that term. Feeds
into the progression suggestion logic in `06_workout_session_and_timers.md`
as an extra signal alongside "hit top of rep range."

## 7. Swap exercise mid-session
If equipment's unavailable, let the user swap an exercise for an
alternative from a small curated substitution list (e.g. Pull-Ups ↔ Lat
Pulldown ↔ Assisted Pull-Up Machine), logged as a substitution note on that
`SetLog` entry so history stays accurate.

## 8. In-session notes
A small optional text field per session ("felt strong today," "shoulder
was tight") saved to `WorkoutSession.notes` — shows up later in Calendar
day detail and History.

## 9. Haptic feedback throughout
- Light haptic tick on stepper +/- taps
- Medium haptic on "log set" confirm
- Distinct haptic pattern when a set matches/beats last time's numbers (a
  small "nice" moment — pairs well with the PR celebration in
  `13_new_feature_roadmap.md`)

## 10. Visual session progress
A slim progress indicator (dots or a bar) showing how far through the
session the user is (block X of Y, or overall % of planned sets
completed) — gives a sense of "almost done" without checking the clock.

## 11. One-handed reachability
Keep primary actions (log set, skip rest, continue) in the **bottom
third** of the screen — this screen is used one-handed and often
mid-motion; don't put critical controls at the very top.

## Data model note

Add to `SetLog`: `rpeTag: String?` (nullable, "easy"/"right"/"hard"),
`substitutedFrom: Long?` (nullable exercise id, for exercise swaps).
Add to `WorkoutSession`: `notes: String?`.
