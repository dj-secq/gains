# 01 — Overview

## What this app is

A single-user Android app that runs one person's specific training rotation
and supplement schedule end-to-end:

- Tells the user what today is (Workout A / Workout B / Rest)
- Runs a guided, timed workout session (supersets, rest timers, set logging)
- Tracks whey protein and creatine intake daily
- Reminds the user at the right times
- Shows a calendar of what was done vs. planned
- Shows progress over time (reps/weight per exercise, bodyweight, adherence)
- Tracks how many servings are left in the whey tub and creatine container

## Who it's for

One person (the app owner), tracking their own program. No multi-user
accounts, no social features, no sharing.

## Core rotation being modeled

```
Workout A → 1 Rest Day → Workout B → 2 Rest Days → repeat
```

This is a fixed 5-day cycle: Day1=A, Day2=Rest, Day3=B, Day4=Rest, Day5=Rest,
then repeats from Day1=A. The app must compute "what is today" from a cycle
start date, not require manual day-by-day input (though the user can
override/reschedule a single day if life happens — see `08_calendar_and_progress.md`).

## In scope (v1)

- Guided workout sessions with supersets and rest timers
- Set/rep/weight logging per exercise
- Progression suggestion (see `06_workout_session_and_timers.md`)
- Daily whey + creatine logging
- Reminders for workout days, creatine, and post-workout whey
- Calendar view of the rotation + completion history
- Progress charts (per-exercise, bodyweight, supplement adherence)
- Supply tracking (servings remaining in tub/container, low-supply alerts)
- Editing the program (exercises, sets/reps/rest, supplement amounts)

## Out of scope (v1)

- Cloud sync / backup / multi-device
- User accounts or login
- Social features, sharing, leaderboards
- Nutrition/macro tracking beyond protein from whey
- Wearable integration (Wear OS, etc.) — may be a future phase
- Apple/iOS — Android only

## Design tone

Simple, fast to use *during* a workout (large touch targets, minimal typing
mid-set), and informative *outside* a workout (calendar, charts). Prioritize
in-workout usability above all — this is used one-handed, sweaty, mid-rest.
