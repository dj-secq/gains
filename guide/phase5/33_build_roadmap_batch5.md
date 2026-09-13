# 33 — Build Roadmap (Batch 5 / Phases 28–32)

Continues numbering from guide/phase4's Phases 20–25. Same rule: each
phase compiles and is reviewable before the next starts. These are mostly
independent fixes — order below is by risk/dependency, not strict necessity.

## Phase 28 — Rest timer gaps + the +15s bug
- Add `restSecondsAfterBlock` to `TemplateBlock`, wire block-transition
  rests into the session flow
- Fix the +15s bug via the single-source-of-truth `endTimestampMillis` fix
- Run the regression checklist in `28_rest_timer_gaps_and_15s_bug.md`
  with an actual stopwatch, not just visual inspection
- Reference: `28_rest_timer_gaps_and_15s_bug.md`
- **Do this first** — it's the most safety-critical fix (a silently wrong
  timer is worse than a missing feature) and touches the same foreground
  service infrastructure that the next phase's hold-timer reuses.

## Phase 29 — Work-interval timer for time-based exercises
- Add `repType`/`targetSecondsLow/High` to `TemplateBlockExercise`,
  `actualSeconds` to `SetLog`
- Build the hold-timer UI, reusing the large-numeral timer component
  pattern from the (now-fixed) rest timer where sensible
- Update seed data for Hollow Body Hold
- Reference: `29_work_interval_timer.md`

## Phase 30 — Prefill from previous values
- Wire actual stepper prefill from `SetLogDao` history, per-round,
  per-exercise, with the progression-suggestion increment logic
- Handle the zero-history and exercise-swap edge cases explicitly
- Reference: `30_persistent_last_values_and_prefill.md`

## Phase 31 — Session back navigation + true cancel
- Add the "Previous" control and backward position-pointer logic
- Add "Cancel Workout" with confirmation + full deletion of the
  in-progress session and its sets
- Fix system back button interception on the Session screen to route
  through the same cancel-confirmation flow
- Reference: `31_session_navigation_and_cancel.md`
- Test explicitly: force-cancel a session, confirm zero trace in
  Calendar/Progress/streak afterward

## Phase 32 — Exercise categorization data
- Apply the `muscleGroup` values from `32_exercise_categorization_data.md`
  directly to the seed data / existing exercise rows
- Confirm muscle-group badges render correctly across Session, Exercise
  Library, and wherever else they're displayed
- Reference: `32_exercise_categorization_data.md`
- **Do this last** — it's the lowest-risk, purely-data change and doesn't
  need to block or be blocked by anything else in this batch

## Notes for Codex

- Phases 28 and 29 both touch timer infrastructure — land 28 completely,
  confirm the regression checklist passes, before starting 29, so any
  remaining timer bugs aren't attributed to the wrong phase.
- Phase 31's "cancel leaves zero trace" requirement should be covered by
  an actual test (create a session, log a couple of sets, cancel, then
  query Calendar/streak state and assert nothing changed) — this is
  exactly the kind of thing that's easy to get subtly wrong (e.g. deleting
  the `WorkoutSession` row but leaving orphaned `SetLog` rows behind).
- Phase 32 is a good candidate to do in the same sitting as a quick sanity
  pass through the whole exercise list — while you're in there, confirm
  every seeded exercise also still has a valid `imageAssetName` from
  guide/phase4's media sourcing work.
