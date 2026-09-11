# 03 — Data Model (Room)

All entities below are a starting schema. Codex should add indices/foreign
keys as appropriate and feel free to add fields that make implementation
cleaner, as long as the core relationships hold.

## Entities

### `Exercise`
Represents a single movement (e.g. "Pull-Ups").
| field | type | notes |
|---|---|---|
| id | Long (PK, autogen) | |
| name | String | |
| notes | String? | optional cue/notes |

### `WorkoutTemplate`
Represents "Workout A" or "Workout B" (the repeatable blueprint).
| field | type | notes |
|---|---|---|
| id | Long (PK) | |
| name | String | e.g. "Workout A — Upper Body + Light Legs" |
| dayLabel | String | "A" or "B" |
| maxDurationMinutes | Int | e.g. 40 |

### `TemplateBlock`
A superset group within a template (e.g. "Superset A", "Warm-Up", "Optional Core").
| field | type | notes |
|---|---|---|
| id | Long (PK) | |
| templateId | Long (FK → WorkoutTemplate) | |
| label | String | "Warm-Up", "Superset A", "Optional Core", etc. |
| orderIndex | Int | display/execution order |
| targetRounds | String | e.g. "3", "2–3" |
| restSecondsBetweenRounds | Int? | e.g. 90–120 → store a default int, editable |
| isOptional | Boolean | true for "Optional Core" blocks |

### `TemplateBlockExercise`
Join table: which exercises belong to a block, in what order, with what target.
| field | type | notes |
|---|---|---|
| id | Long (PK) | |
| blockId | Long (FK → TemplateBlock) | |
| exerciseId | Long (FK → Exercise) | |
| orderIndex | Int | order within the block (for supersets, execution order) |
| targetRepsLow | Int | |
| targetRepsHigh | Int | |
| perSide | Boolean | true if reps are "per arm/leg" |

### `WorkoutSession`
One occurrence of doing Workout A or B (or a logged rest day, optionally).
| field | type | notes |
|---|---|---|
| id | Long (PK) | |
| templateId | Long? (FK) | null if this is a rest-day entry |
| date | LocalDate | |
| startTime | Instant? | |
| endTime | Instant? | |
| completed | Boolean | |
| durationSeconds | Int? | actual time taken |

### `SetLog`
Individual set performance within a session.
| field | type | notes |
|---|---|---|
| id | Long (PK) | |
| sessionId | Long (FK → WorkoutSession) | |
| exerciseId | Long (FK → Exercise) | |
| roundNumber | Int | which round of the superset (1, 2, 3...) |
| reps | Int | |
| weightKg | Float? | null for bodyweight moves |
| loggedAt | Instant | |

### `SupplementLog`
One day's whey/creatine intake.
| field | type | notes |
|---|---|---|
| id | Long (PK) | |
| date | LocalDate | unique per date |
| wheyTaken | Boolean | |
| wheyServings | Float | default 1.0, editable (e.g. 0.5, 1, 2) |
| creatineTaken | Boolean | |
| creatineGrams | Float | default 5.0 |

### `BodyweightLog`
Optional bodyweight tracking for the protein-goal calculation.
| field | type | notes |
|---|---|---|
| id | Long (PK) | |
| date | LocalDate | |
| weightKg | Float | |

### `SupplyInventory`
Tracks servings remaining for whey tub and creatine container.
| field | type | notes |
|---|---|---|
| id | Long (PK) | |
| type | String | "whey" or "creatine" |
| totalServings | Int | e.g. 65 for whey, 30 for creatine |
| servingsRemaining | Float | decremented on each log |
| startDate | LocalDate | when this container was opened |

### `CycleSettings` (DataStore, not Room)
| field | type | notes |
|---|---|---|
| cycleStartDate | LocalDate | date Day 1 (Workout A) began, used to compute today's slot |
| wheyServingGrams | Float | default 25.0 (protein per serving) |
| proteinGoalMultiplierLow | Float | default 1.6 |
| proteinGoalMultiplierHigh | Float | default 2.0 |
| reminderTimesEnabled/times | see `07_notifications_and_reminders.md` |

## DAOs needed

- `ExerciseDao`, `WorkoutTemplateDao`, `TemplateBlockDao`,
  `TemplateBlockExerciseDao` — mostly read + admin edit
- `WorkoutSessionDao` — CRUD sessions, query by date range, query last N
  sessions for a template (for progression logic)
- `SetLogDao` — insert, query by session, query history per exercise
  (for progress charts + progression suggestion)
- `SupplementLogDao` — upsert by date, query by date range, streak queries
- `BodyweightLogDao` — insert, query latest, query range for chart
- `SupplyInventoryDao` — read/update remaining servings

## Relationships summary

```
WorkoutTemplate 1—* TemplateBlock 1—* TemplateBlockExercise *—1 Exercise
WorkoutSession *—1 WorkoutTemplate (nullable)
WorkoutSession 1—* SetLog *—1 Exercise
SupplementLog: one row per calendar date
BodyweightLog: many rows, one used per date for protein-goal calc
```
