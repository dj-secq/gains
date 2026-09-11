# 04 — Default Seed Data

This is the actual program to pre-populate on first launch (via a Room
`RoomDatabase.Callback` or a one-time migration/seed step). The user can
edit all of it later in Settings — this is just the starting state.

## Rotation

```
Day 1: Workout A
Day 2: Rest
Day 3: Workout B
Day 4: Rest
Day 5: Rest
→ repeat from Day 1
```

Max session duration: **40 minutes** for both workouts.

---

## Workout A — Upper Body + Light Legs

**Warm-Up (block, not counted as a "round" superset)** — 4–5 min
- Jumping Jacks — 40–60 reps
- Arm Circles — 10 forward + 10 backward
- Band Pull-Aparts — 15 reps
- Bodyweight Squats — 10 reps
- Easy Push-Ups — 5–8 reps

**Superset A** — 3 rounds, rest 90–120s between rounds
- Pull-Ups — 4–6 reps
- DB Floor Press — 8–12 reps

**Superset B** — 3 rounds, rest 90–120s
- Single-Arm DB Row — 8–15 reps per arm (perSide = true)
- DB Overhead Press — 8–12 reps

**Superset C** — 2–3 rounds, rest 60–90s
- DB Bicep Curl — 8–15 reps
- Bulgarian Split Squat — 8–12 reps per leg (perSide = true)

**Optional Core** (isOptional = true, skip first if near 40 min)
- Hanging Knee Raise — 2 × 10–15 reps

---

## Workout B — Upper Body + Posterior Chain

**Warm-Up** — same as Workout A's warm-up block.

**Superset A** — 3 rounds, rest 90–120s
- Chin-Ups — 4–7 reps
- Push-Ups — 8–15 reps

**Superset B** — 2–3 rounds, rest 60–90s
- DB Romanian Deadlift — 8–15 reps
- Band Lateral Raise — 12–20 reps

**Superset C** — 2–3 rounds, rest 60–90s
- DB Overhead Triceps Extension — 10–15 reps
- Band Face Pull — 15–20 reps

**Optional Core** (isOptional = true, skip first if near 40 min)
- Hollow Body Hold — 2 × 20–40 sec (time-based "reps" — see note below)

> **Note on time-based sets:** Hollow Body Hold is a duration, not a rep
> count. Either add an optional `targetSeconds` field to
> `TemplateBlockExercise` for time-based moves, or store it as reps with a
> unit flag (`repType: REPS | SECONDS`). Recommend adding a `repType` enum
> to keep the schema honest — several core/hold moves may be added later.

---

## Progression Rule (for `domain/progression`)

Stay around 1–2 reps before failure on most working sets. When the user
hits the **top of the target rep range on all rounds of a movement with
clean form**, the app should suggest increasing the load next session.

Example: target is 8–12 reps. Once logged as 12/12/12 across all rounds,
flag that exercise with a "Ready to increase weight" suggestion the next
time it's scheduled. See `06_workout_session_and_timers.md` for exact logic.

---

## Whey Protein

- Product: "Promatrix Whey" — **~25 g protein per serving**
- Workout day: 1 serving after workout (mixed with water or milk)
- Rest day: 1 serving *only if meals are low in protein* (optional, user-logged)
- Daily protein goal: **1.6–2.0 g protein × bodyweight in kg**
  - e.g. 60 kg → ~96–120 g/day (whey is one contributor to this, not the whole target)
- Tub size: **~65 servings**. At ~1/day → ~65 days. At mostly-workout-days
  (roughly 2 servings per 5-day cycle) → ~3.5–4 months.

## Creatine Monohydrate

- Dose: **5 g every day**, workout days and rest days alike
- Workout day: 1 serving whey + 5 g creatine + water/milk, after workout
- Rest day: 5 g anytime, with a meal or water
- No loading phase, no cycling, timing not important
- If a day is missed: just take the normal 5 g the next day — never double up
- Container size: **30 servings**, lasts **30 days** at 5 g/day

---

## Seed values for `SupplyInventory`

| type | totalServings | servingsRemaining (initial) |
|---|---|---|
| whey | 65 | 65 |
| creatine | 30 | 30 |

## Seed values for `CycleSettings`

| field | value |
|---|---|
| cycleStartDate | date of first app use (Day 1 = Workout A), user-confirmable on onboarding |
| wheyServingGrams | 25.0 |
| proteinGoalMultiplierLow | 1.6 |
| proteinGoalMultiplierHigh | 2.0 |
