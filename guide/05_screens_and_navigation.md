# 05 — Screens & Navigation

## Navigation shell

Bottom navigation bar with 4 top-level destinations, plus modal/pushed
screens layered on top:

```
[ Today ]   [ Calendar ]   [ Progress ]   [ Settings ]
```

`Today` is the launch destination.

---

## 1. Today (Home) screen

Purpose: answer "what do I do today" in one glance and get into a workout
fast, or log supplements fast on a rest day.

Contents:
- Big card: today's slot — "Workout A", "Workout B", or "Rest" — with the
  cycle day number (e.g. "Day 3 of 5")
- If workout day: primary button **"Start Workout"** → pushes Session screen
- If rest day: quick supplement log card (creatine checkbox, optional whey
  checkbox) right on this screen — no navigation needed
- Small "Supplements today" summary (creatine ✓/✗, whey ✓/✗) always visible,
  even on workout days (creatine can be logged independent of the workout)
- Current streak indicator (e.g. "🔥 6-day streak")
- Supply warning banner if whey or creatine is running low (see
  `08_calendar_and_progress.md`)

## 2. Session screen (Active Workout)

Purpose: guide the user through the workout in real time. Full details in
`06_workout_session_and_timers.md`. Key UI elements:
- Session timer (counting up, with the 40-min soft limit indicated)
- Current block name (e.g. "Superset A — Round 2 of 3")
- Current exercise(s) with large tap targets to log reps/weight
- Rest timer (large countdown, auto-starts after logging a round)
- "Next" / "Finish Exercise" / "Skip Optional Core" controls
- "End Workout" button (with confirmation) — always accessible
- On finish: summary screen (sets logged, duration, any progression
  suggestions) before returning to Today

## 3. Calendar screen

Purpose: see the rotation and history at a glance.
- Month view; each day cell color/icon-coded: Workout A / Workout B / Rest /
  Rest+skipped-supplement-warning / Completed / Missed
- Tapping a past day shows that day's log (sets performed, supplements taken)
- Tapping a future day shows the planned slot
- Small control to "shift cycle" if the user needs to reschedule (e.g. sick
  day) — shifts `cycleStartDate` forward by the needed offset from that point

## 4. Progress screen

Purpose: charts and trends. Details in `08_calendar_and_progress.md`.
- Exercise picker → line chart of weight/reps over time for that exercise
- Bodyweight trend chart (if logging bodyweight)
- Supplement adherence chart (% days creatine taken over last 30/90 days)
- Streak history

## 5. Settings screen

- **Program editor**: view/edit Workout A and B — blocks, exercises,
  target reps, rest times (reuses the data model directly)
- **Supplement settings**: whey serving grams, creatine dose, tub/container
  sizes (resets `SupplyInventory`), protein goal multipliers
- **Reminders**: toggle + set times for workout-day reminder, creatine
  reminder, post-workout whey reminder (see `07_notifications_and_reminders.md`)
- **Cycle settings**: view/edit cycle start date
- **Units**: kg/lb toggle (store canonically in kg, convert for display)

---

## Screen → data flow summary

| Screen | Reads | Writes |
|---|---|---|
| Today | ScheduleRepository (today's slot), SupplementRepository, streak | SupplementLog (quick toggles) |
| Session | WorkoutRepository (template for today), last session history | WorkoutSession, SetLog |
| Calendar | WorkoutSession history, SupplementLog history, ScheduleRepository | CycleSettings (on reschedule) |
| Progress | SetLog history, BodyweightLog, SupplementLog | BodyweightLog (manual entry) |
| Settings | All template/settings tables | WorkoutTemplate/Block/Exercise edits, CycleSettings, reminder prefs, SupplyInventory reset |

## Visual notes

- Use Material 3 dynamic color if available, otherwise a calm, low-glare
  palette (dark surfaces work well for "used mid-workout" contexts) —
  default the app to **dark theme** since it's likely used in a gym.
- Rest timer and session timer should be legible from arm's length —
  large numerals, high contrast.
