# 27 — Build Roadmap (Batch 4 / Phases 20–25)

Continues numbering from guide/phase3's Phases 16–19. As always: each
phase compiles and is reviewable before the next starts.

## Phase 20 — Bug fixes
- Fix the transparent calendar popup
- Fix calendar scroll conflict
- Reference: `20_bug_fixes.md`
- Do this first and in isolation — it touches the calendar screen that
  Phase 23 will also modify, so get it solid before that lands.

## Phase 21 — Theme depth & motion
- Apply the `surface-depth-and-motion` skill project-wide: layered surface
  tones for dark and light mode, accent glow/wash on hero elements,
  tightened transition timings, shared-continuity transitions,
  predictive back, shimmer loading states
- Reference: `21_theme_depth_and_motion.md`

## Phase 22 — Exercise media
- Source and bundle free-exercise-db images for the current exercise list
- Wire `Exercise.imageAssetName` display into Exercise Library and the
  in-session exercise card (thumbnail + tap-to-expand)
- Add the Settings "Open Source Credits" entry
- Reference: `22_exercise_media_sourcing.md`

## Phase 23 — Flexible schedule engine
- Add `restDaysAfter`/`orderIndex` to `WorkoutTemplate`
- Rewrite `ScheduleRepository` around "most recent completed session"
  instead of date-offset math
- Rewrite Today screen's suggestion card + "Do something else" override
- Rewrite Calendar to show historical-fact vs. advisory-suggestion styling
- Remove the old "reschedule cycle" feature
- Redefine and rebuild the adherence streak logic with the grace-period rule
- Rewrite the rotation-math unit tests against the new algorithm
- Reference: `23_flexible_schedule_engine.md` — this is the largest single
  piece of work in this batch; budget the most review time here

## Phase 24 — Customizable workouts & categories
- Template add/reorder/delete in the program editor
- Category field + colored chip UI across Today/Calendar/Session
- Confirm/fix rest-time editability
- Exercise Library category filter (last, lowest priority in this phase)
- Reference: `24_customizable_workouts_and_categories.md`
- Depends on Phase 23's `WorkoutTemplate` schema changes being in place

## Phase 25 — Customizable supplements
- New `Supplement`/`SupplementIntakeLog` entities + migration with backfill
- "Manage Supplements" Settings screen
- Today screen's dynamic supplement quick-log list
- Update supply tracking, adherence charts, and reminders to be generic
  over any number of supplements
- Reference: `25_customizable_supplements.md`
- Independent of Phases 23–24; can be built in parallel if useful, but
  land its migration separately and test it in isolation regardless

## Sequencing notes for Codex

- Do Phase 20 before Phase 23 — fixing calendar bugs on the old rigid
  model first avoids re-diagnosing the same bugs against a moving target.
- Phase 21 can run in parallel with 22/23/24/25 since it's purely visual
  and doesn't touch the schema — but land it as its own reviewable pass,
  don't mix visual and logic changes in the same diff.
- Land the Phase 23 + Phase 26 (`26_data_model_additions_batch4.md`)
  migration together, tested thoroughly, before starting Phase 24 on top
  of it.
- Phase 25's supplement migration is independent — sequence it wherever
  convenient, but keep its migration isolated and tested on its own rather
  than bundled into the Phase 23 migration.
