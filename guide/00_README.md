# README — Context Docs for Codex

This folder contains the full spec for an Android app called **"Reps & Grams"**
(placeholder name — rename freely). It is a personal workout + supplement
tracker built around one specific rotation and one specific supplement
protocol (whey + creatine), with an active-workout mode, timers, reminders,
a calendar, and progress charts.

## Read these files in this order

1. `01_overview.md` — what the app is, who it's for, scope boundaries
2. `02_architecture_and_stack.md` — tech stack, libraries, project structure
3. `03_data_model.md` — Room entities, relationships, DAOs needed
4. `04_seed_data.md` — the actual default program (exercises, sets/reps,
   supplement protocol) to pre-populate the database with
5. `05_screens_and_navigation.md` — every screen, its purpose, and the nav graph
6. `06_workout_session_and_timers.md` — the active-workout engine: superset
   flow, rest timer, session timer, progression logic
7. `07_notifications_and_reminders.md` — WorkManager reminders
8. `08_calendar_and_progress.md` — calendar view, streaks, charts, supply countdown
9. `09_build_roadmap.md` — the phased implementation order to actually code this in

## Instructions for Codex

- Build this as a native Android app in **Kotlin + Jetpack Compose**, Material 3.
- Local-only storage (Room). No backend, no login, no cloud sync.
- Single user, single device. Do not build multi-user support.
- Follow `09_build_roadmap.md` phase by phase — get each phase compiling and
  runnable before moving to the next. Don't try to build everything in one pass.
- Where a design decision isn't specified, pick the simplest option that
  fits Material 3 / standard Android conventions, and keep it consistent
  with choices already made elsewhere in these docs.
- All exercise/set/rep/timer defaults in `04_seed_data.md` should be
  **editable by the user later**, not hardcoded permanently — seed data is
  a starting point, not a constraint on the schema.
