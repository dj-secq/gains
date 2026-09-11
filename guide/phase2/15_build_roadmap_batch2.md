# 15 — Build Roadmap (Batch 2 / Phases 10–15)

Continues numbering from `09_build_roadmap.md` (Phases 1–9 = initial build).
Same rule as before: each phase should compile and be reviewable before
starting the next. This batch assumes Phases 1–9 and the visual restyle
(ios-style-ui / app-color-system / data-viz-style) are already done.

## Phase 10 — Rest timer reliability + alarm behavior
- Foreground service for active rest timer
- Alarm-style sound + vibration pattern on completion
- Lock-screen/background notification with "Continue" / "+15s" actions
- "Up next" context on the completion alert
- Settings: sound picker, vibration toggle, auto-advance toggle
- Reference: `10_rest_timer_alarms.md`

## Phase 11 — During-workout UI polish
- "Last time" inline comparison on exercise cards
- "Up next" strip
- Stepper-based rep/weight input
- Warm-up calculator (optional, toggle-able)
- RPE tag chip per set
- Exercise swap flow
- In-session notes field
- Haptic feedback pass
- Reference: `11_workout_session_ui_polish.md`, data changes from `14_data_model_additions.md`

## Phase 12 — Exercise media + icon/nav polish
- Illustration asset pipeline (pick sourcing option from `12_exercise_media_and_icons.md` §6)
- Muscle-group tagging + badges
- Exercise Library screen
- Bottom nav icon upgrade (Phosphor, outline/fill active state)
- Category icon pass (supplements, streak, calendar glyphs)
- Empty-state illustrations
- Reference: `12_exercise_media_and_icons.md`

## Phase 13 — Personal Records, 1RM, body measurements, backup
- PR detection logic + celebration moment
- Estimated 1RM calculation + display
- Body measurement logging + chart
- Export/import (JSON/zip) flow in Settings
- Room migration for all Phase 10–13 schema changes (do this migration
  once, covering everything from `14_data_model_additions.md`, rather than
  piecemeal per phase)
- Reference: `13_broader_feature_roadmap.md` (Now section), `14_data_model_additions.md`

## Phase 14 — Health Connect + achievements + widget
- Health Connect permission flow + write-only integration (sessions, bodyweight)
- Achievement definitions + unlock logic
- Home screen widget (today's plan + quick action)
- Custom exercise creation flow
- Voice cue toggle (TextToSpeech) in session
- Reference: `13_broader_feature_roadmap.md` (Next section)

## Phase 15 — Polish pass
- Full regression pass through a complete 5-day cycle with every Phase
  10–14 feature exercised at least once
- Accessibility check (contrast, touch targets, TalkBack labels on new
  icons/illustrations)
- Performance check on the exercise illustration asset pipeline (image
  sizes, load times) and the new charts/animations

## Notes for Codex

- Don't start Phase 14's Health Connect work until Phase 13's local
  data model is stable — Health Connect writes should read from the same
  finalized `WorkoutSession`/`BodyMeasurementLog` shape, not a moving target.
- The single Room migration in Phase 13 is deliberate — batching schema
  changes into one migration per phase-group is safer than one migration
  per individual feature for a local-only app with real user data at stake.
- If any phase surfaces the need for a design decision not covered by the
  `ios-style-ui` / `app-color-system` / `data-viz-style` skills, extend
  those skill files rather than improvising inconsistent one-off styling.
