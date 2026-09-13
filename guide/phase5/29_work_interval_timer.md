# 29 — Work-Interval Timer for Time-Based Exercises

Exercises like Hollow Body Hold are logged as a duration ("2 × 20–40 sec"),
not a rep count, but there's currently no actual timer *during* the hold
itself — just a number entry after the fact. This adds a real countdown/
count-up timer for the working set itself, not just for rest between sets.

## Data model (formalizes the note left in `04_seed_data.md`)

Add to `TemplateBlockExercise`:
- `repType: String` — `"REPS"` or `"SECONDS"`
- `targetSecondsLow: Int?` / `targetSecondsHigh: Int?` (parallel to the
  existing `targetRepsLow/High`, used only when `repType = "SECONDS"`)

Add to `SetLog`:
- `actualSeconds: Int?` (populated instead of `reps` when `repType = "SECONDS"`)

Seed data update: set `repType = "SECONDS"` with `targetSecondsLow = 20,
targetSecondsHigh = 40` for Hollow Body Hold; everything else stays
`repType = "REPS"`.

## Session UI for time-based exercises

When the current exercise has `repType = "SECONDS"`:
- Instead of a rep stepper, show a **"Start Hold" button** that begins a
  count-up (or count-down toward `targetSecondsHigh`, user's choice —
  count-up is simpler and shows real effort, recommend that as default)
  timer with large numerals, matching the visual weight of the rest timer.
- While holding: show the target range as a subtle marker (e.g. a tick at
  20s and 40s on the display) so the user knows when they're in range.
- A **"Stop"** button ends the hold and logs `actualSeconds` as whatever
  the timer reached — this becomes the logged value for that round,
  exactly like reps would be for a rep-based exercise.
- Same haptic/visual "matched or beat last time" treatment as rep-based
  sets (per `11_workout_session_ui_polish.md` §9) — compare against last
  session's `actualSeconds` for this exercise.
- After stopping, flow continues exactly as a normal round would (advance
  to next exercise, or rest timer if that was the last exercise in the
  round) — this is a drop-in replacement for the rep-stepper step, not a
  separate flow.

## Progression logic update

The progression suggestion logic (`06_workout_session_and_timers.md`,
"Progression suggestion logic") should treat hitting
`targetSecondsHigh` across all rounds the same way it treats hitting
`targetRepsHigh` — suggest increasing difficulty (e.g. "try a harder
variation" for a bodyweight hold, since there's no weight to add).

## Where else `repType` matters

- Progress charts (`data-viz-style` / `08_calendar_and_progress.md`):
  time-based exercises chart `actualSeconds` over time instead of reps/weight.
- "Last time" prefill (`30_persistent_last_values_and_prefill.md`): prefill
  the target hold duration the same way reps/weight get prefilled.
- Exercise Library / program editor: when adding or editing an exercise
  within a block, expose a toggle for "Reps-based" vs. "Time-based" so any
  future custom exercise (not just Hollow Body Hold) can use this feature.
