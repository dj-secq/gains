# 25 — Customizable Supplements

Right now whey and creatine are hardcoded columns on `SupplementLog`, and
"managing supply" is just a bare restock action. This generalizes it to
any number of user-defined supplements with real editable properties.

## New model: generic `Supplement` entity

Replace the hardcoded whey/creatine fields with a proper entity:

### `Supplement`
| field | type | notes |
|---|---|---|
| id | Long (PK) | |
| name | String | e.g. "Promatrix Whey", "Creatine Monohydrate", or anything user-added |
| doseAmount | Float | e.g. 25.0, 5.0 |
| unit | String | "g", "mg", "ml", "capsule", "scoop" |
| scheduleType | String | "daily", "workoutDayOnly", "customDays" |
| customDays | String? | e.g. "Mon,Wed,Fri" — only used if scheduleType = customDays |
| containerSize | Int | total servings/doses per container |
| lowSupplyThreshold | Int | when to show the low-supply banner |
| colorToken | String | reuses `app-color-system` semantic tokens or a user-picked accent |
| iconName | String | icon reference (Phosphor set) |
| isActive | Boolean | soft-disable without deleting history |

### `SupplementIntakeLog`
| field | type | notes |
|---|---|---|
| id | Long (PK) | |
| supplementId | Long (FK → Supplement) | |
| date | LocalDate | |
| taken | Boolean | |
| actualAmount | Float | usually equals `doseAmount`, but editable per-log (e.g. half serving) |

### `SupplyInventory` (existing entity, now generic)
Change `type: String` (was "whey"/"creatine" literal) to
`supplementId: Long` (FK → Supplement) — same structure otherwise
(`totalServings`, `servingsRemaining`, `startDate`), now works for any
number of supplements.

## Settings: "Manage Supplements" screen

- List of all supplements (active + inactive), each showing name, dose,
  schedule, and current supply level.
- **Add supplement**: name, dose amount + unit, schedule type, container
  size, low-supply threshold, color/icon pick.
- **Edit supplement**: all the same fields, editable anytime — not locked
  after creation.
- **Restock**: reset `servingsRemaining` to `totalServings` (can also edit
  `containerSize` here if they bought a different size tub/bottle this time).
- **Deactivate/delete**: soft-deactivate (keeps history, stops showing on
  Today) vs. hard-delete (only if no `SupplementIntakeLog` history exists).

## Today screen changes

Replace the two fixed whey/creatine checkboxes with a **dynamic list**:
for each active `Supplement` whose `scheduleType` says it's due today
(daily = always due; workoutDayOnly = due if a workout was completed
today; customDays = due if today matches the configured days), show a
quick-log row with the supplement's icon/color, name, and dose — tap to
mark taken (defaulting `actualAmount` to `doseAmount`, editable inline if
they took a different amount).

## Migration plan (preserve existing history)

This changes existing data, not just adds columns — handle carefully:

1. Create two `Supplement` rows on migration: "Promatrix Whey" (25g,
   workoutDayOnly default, matching `04_seed_data.md`) and "Creatine
   Monohydrate" (5g, daily).
2. For every existing `SupplementLog` row, create corresponding
   `SupplementIntakeLog` rows: one for whey if `wheyTaken` was true (using
   `wheyServings` as `actualAmount`), one for creatine if `creatineTaken`
   was true (using `creatineGrams`).
3. Migrate `SupplyInventory`'s `type = "whey"/"creatine"` rows to reference
   the new `Supplement` IDs via `supplementId`.
4. Drop the old fixed columns from `SupplementLog` (or drop the table
   entirely if `SupplementIntakeLog` fully replaces it) only after
   confirming the backfill is correct — test this migration explicitly per
   `16_stability_and_testing.md` §1's migration-testing guidance.

## Downstream updates needed

- Supply tracking / low-supply banner logic (`08_calendar_and_progress.md`)
  now iterates over all active supplements generically instead of two
  hardcoded checks.
- Supplement adherence chart (`08_calendar_and_progress.md`) becomes
  per-supplement, with a picker if there are more than 2.
- Reminders (`07_notifications_and_reminders.md`) generalize similarly —
  one reminder job type parameterized per supplement's schedule, rather
  than two hardcoded reminder kinds.
