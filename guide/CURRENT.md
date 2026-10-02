# Gains — current spec

This file is the product spec. The old phase plans have been removed. Do not rebuild the 5-day cycle or the whey columns.

## Product

Gains (`com.example.repsgrams`) is a local workout log for one person on one device. The database is `reps-and-grams.db`. There are no accounts, no cloud sync, no social feed, no server or on-device program generator, no nutrition tracker, and no Wear OS.

Four tabs: Today, Calendar, Progress, Settings. Pushed routes are the session, the program editor, the exercise catalogs, and Manage Supplements. The session hides the tab bar and stays dark in either theme. The theme composable is `RepsGramsTheme`.

Weeks are Monday-first. Weights are stored as kilograms and lengths as centimeters.

## Schedule

The rotation engine picks the next template by `orderIndex`. The due date is the last completed workout’s date plus that template’s `restDaysAfter` plus one. Seeded A is `orderIndex` 0 and one rest day. Seeded B is `orderIndex` 1 and two rest days.

`workout_sessions.sessionKind` is `WORKOUT` or `REST`. A rest on or after the due date restarts the same gap and does not advance the rotation. A rest strictly before the due date does not push it. `FREESTYLE` is not written yet. Do not rebuild the old 5-day `cycleStartDate` scheduler. Do not add whey or creatine columns.

Personal records are written when a workout finishes. System back leaves an in-progress workout in place. Discard is the only delete path. The user exports a backup zip. Health Connect writes a finished workout and a bodyweight entry only when that toggle is on.

Schema 8 added `sessionKind`. Schema 9 adds `programs` (one backfilled row named Program, schedule mode `ROTATION`), a nullable weekday on each template, `setType` (existing rows are `WORKING`), numeric `rpe` copied from `rpeTag`, and `equipment` on exercises. The app schema is 9.

## Board

Light canvas `#E8E8EA`, paper `#FFFFFF`, ink `#111111`, hairline `#E0E0E0`, label `#666666`. Dark canvas `#000000`, paper `#1A1A1A`, hairline `#333333`, label `#999999`. Done green is `#128A42` on the light board and `#3DDC84` on the dark board. It marks a finished, taken, or trained state. Signal red `#D71921` marks not-yet (overdue, untaken, missed, low supply), and it stays on the live-session dot, a rest numeral at or after zero, a personal-record mark, and a destructive label on a paper dialog. It is not the primary button. Cards stay black and white. The on-screen Discard label is the label color.

A large card on the light board has a small drop shadow and no border. A dark card has a 1dp hairline and no glow. Icon buttons, pills, chips, keys, and the tab bar are flat. There is no inset well. A press uses alpha. No glow, no spring, no bounce. Screen inset and card padding are 16dp. Gaps between cards are 12dp.

Type is Doto for numerals at 28sp or larger, Space Grotesk for words, and Space Mono for labels and figures. Do not bundle NDot, SF Pro, or Nothing trademarks. Shapes are a circle, a 48dp pill, and a 24dp squircle. Motion is 150–250ms ease-out. New UI uses `ui/components/Board.kt`.

## Out of scope

Accounts, sharing, ads, a store, auto-deload, Health Connect reads, achievement UI, per-supplement reminder clocks, distance or calorie rep types, a widget configuration screen, and a live rest countdown on the widget.
