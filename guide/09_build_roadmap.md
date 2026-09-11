# 09 — Build Roadmap

Build in this order. Get each phase compiling and runnable on-device/emulator
before starting the next. Don't jump ahead.

## Phase 1 — Project setup + data layer
- New Android Studio project, Kotlin + Compose, package structure per
  `02_architecture_and_stack.md`
- Add all dependencies (Room, DataStore, Navigation-Compose, WorkManager)
- Implement all Room entities + DAOs from `03_data_model.md`
- Implement the seed-data callback using `04_seed_data.md`
- Implement `ScheduleRepository` with the Day 1–5 rotation math
- Sanity check: unit test the rotation math (given a cycleStartDate and a
  target date, correct slot is returned) and the seed data actually inserts

## Phase 2 — Today screen + basic navigation
- Bottom nav shell with 4 empty destinations
- Today screen: show computed slot, streak placeholder, supplement quick-log
  card wired to `SupplementLog`
- Confirm data persists across app restarts

## Phase 3 — Workout session engine
- Session screen: full block/superset/round flow per
  `06_workout_session_and_timers.md`
- Rest timer + session timer (get these solid — this is the core UX)
- Set logging wired to `SetLog`
- Session summary screen + resumability after app kill

## Phase 4 — Progression suggestions
- Implement `domain/progression` logic
- Surface "Ready to increase weight" badges in the session screen

## Phase 5 — Reminders
- WorkManager workers per `07_notifications_and_reminders.md`
- Settings UI for reminder toggles/times
- Notification permission flow (Android 13+)

## Phase 6 — Calendar
- Month view, day detail, completion indicators per
  `08_calendar_and_progress.md`
- Reschedule action

## Phase 7 — Progress & supply tracking
- All charts (exercise progress, bodyweight, adherence, protein estimate)
- Supply inventory decrement logic + low-supply banner + restock flow

## Phase 8 — Settings / program editor
- Full CRUD UI for editing Workout A/B templates, blocks, exercises
- Supplement + cycle + units settings

## Phase 9 — Polish
- Dark theme pass, empty states, error states, accessibility (content
  descriptions, touch target sizes)
- App icon, launch screen
- Manual QA pass through a full 5-day cycle end to end

## Notes for Codex while building

- Prefer small, reviewable commits per phase/sub-feature rather than one
  giant diff.
- Write the rotation math and progression logic as pure, unit-testable
  Kotlin functions before wiring them into ViewModels.
- If a phase reveals the data model needs a small addition (e.g. the
  `repType` enum flagged in `04_seed_data.md`), make the change and note it
  — the schema in `03_data_model.md` is a strong starting point, not gospel.
