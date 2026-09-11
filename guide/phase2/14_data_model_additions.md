# 14 — Data Model Additions (This Batch)

Consolidated schema changes needed to support files 10–13. Layer these onto
the existing schema in `03_data_model.md` — additive changes only, no
breaking changes to existing tables beyond the new nullable columns listed.

## Changes to existing entities

### `Exercise` — add:
- `muscleGroup: String` (e.g. "Back", "Chest", "Shoulders", "Arms", "Legs", "Core")
- `imageAssetName: String?` (illustration asset reference, nullable → fallback badge)
- `isCustom: Boolean` (default false; true for user-added exercises)

### `SetLog` — add:
- `rpeTag: String?` ("easy" / "right" / "hard", nullable)
- `substitutedFrom: Long?` (FK → Exercise, nullable — original exercise if swapped mid-session)

### `WorkoutSession` — add:
- `notes: String?`

## New entities

### `PersonalRecord`
| field | type | notes |
|---|---|---|
| id | Long (PK) | |
| exerciseId | Long (FK → Exercise) | |
| type | String | "maxWeight" / "maxReps" / "estimated1RM" |
| value | Float | |
| achievedDate | LocalDate | |
| sourceSetLogId | Long (FK → SetLog) | the set that set this record |

### `BodyMeasurementLog`
| field | type | notes |
|---|---|---|
| id | Long (PK) | |
| date | LocalDate | |
| type | String | "waist" / "chest" / "arms" / "thighs" / user-defined |
| valueCm | Float | store canonically in cm, convert for display |

### `Achievement`
| field | type | notes |
|---|---|---|
| id | Long (PK) | |
| key | String | stable identifier, e.g. "streak_10", "workouts_50" |
| unlockedDate | LocalDate | |

## Settings/DataStore additions

- Rest alert sound choice, vibration on/off, auto-advance on/off (from `10_rest_timer_alarms.md`)
- Warm-up calculator on/off, available plate/dumbbell increments (from `11_workout_session_ui_polish.md`)
- Voice cues on/off (from `13_broader_feature_roadmap.md`)
- Selected body measurement types to track (from `13_broader_feature_roadmap.md`)
- Health Connect: write-enabled on/off, last-sync timestamp (from `13_broader_feature_roadmap.md`)

## New DAOs needed

- `PersonalRecordDao` — insert, query latest per exercise, query all for Progress screen
- `BodyMeasurementLogDao` — insert, query range per type
- `AchievementDao` — insert, query all unlocked

## Migration note

Since seed data already exists (`04_seed_data.md`), this requires a Room
**migration**, not just a fresh schema — write an explicit `Migration`
object adding the new nullable columns/tables so existing local data (the
user's actual workout history by this point) isn't wiped. Test the
migration path specifically; do not rely on `fallbackToDestructiveMigration`.
