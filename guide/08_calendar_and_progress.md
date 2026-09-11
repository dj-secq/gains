# 08 — Calendar, Progress & Supply Tracking

## Calendar view

- Standard month grid. Each day cell shows:
  - A letter/icon for the planned slot: **A**, **B**, or a rest icon
  - A completion indicator overlay: filled/checked if a session was
    completed that day (for workout days) or supplements were logged (for
    rest days), outlined/empty otherwise
  - Past days with a workout day that was **not** completed should be
    visually distinct (e.g. muted red outline) — informational, not
    guilt-tripping copy
- Tap a day → bottom sheet or detail screen:
  - Workout day: which exercises/sets/weights were logged (or "not done")
  - Rest day: whey/creatine logged or not
- "Reschedule from here" action: lets the user shift `cycleStartDate` so
  future days recompute around a missed/moved day, without touching past
  logged history

## Streaks

Define streak as: **consecutive calendar days where the day's requirement
was met** —
- Workout day requirement met = session completed
- Rest day requirement met = creatine logged (whey on rest days is
  optional per the program, so it should NOT break a streak)
- A missed requirement breaks the streak back to 0; today doesn't break the
  streak until the day is over (compute "current streak" as of yesterday,
  plus today if already met)

Show current streak and best streak (all-time) somewhere visible on Today
and in Progress.

## Progress charts

1. **Per-exercise progress** — user picks an exercise → line chart of
   either max weight per session or total reps per session over time
   (x-axis = date, one point per session that exercise appeared in). Toggle
   between "weight" and "reps" view for weighted exercises; reps-only view
   for bodyweight exercises.
2. **Bodyweight trend** — simple line chart from `BodyweightLog`, with a
   lightweight "log today's weight" entry point right on this chart.
3. **Supplement adherence** — bar or heatmap-style view of creatine-taken
   over the last 30/90 days (% adherence stat + visual).
4. **Protein estimate vs. goal** — using latest `BodyweightLog` entry ×
   `proteinGoalMultiplierLow/High` from `CycleSettings`, show the daily
   protein gram target range, and how many whey servings (× `wheyServingGrams`)
   have been logged this week as a rough contribution toward it. Make clear
   in the UI this only counts whey, not total dietary protein.

## Supply tracking

- Each `SupplementLog` insert/update that marks `wheyTaken = true` or
  `creatineTaken = true` decrements the corresponding `SupplyInventory.servingsRemaining`
  by the logged amount (`wheyServings` or `creatineGrams / 5.0` doses — keep
  it simple: 1 log = 1 serving/dose consumed).
- Show remaining servings and an estimated "runs out around [date]" on the
  Today screen (small) and Settings/Progress (detailed), based on average
  consumption rate so far.
- Low-supply banner on Today when `servingsRemaining` drops below a
  threshold (e.g. 7 servings for whey, 5 for creatine) — actionable copy
  like "Whey running low — about a week left."
- Settings screen lets the user "Restock" (reset `servingsRemaining` to
  `totalServings`, update `startDate`) when they open a new tub/container.
