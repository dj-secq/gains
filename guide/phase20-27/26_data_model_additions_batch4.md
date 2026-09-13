# 26 — Data Model Additions (Batch 4)

Consolidates schema changes from files 22–25. Additive where possible;
flagged migrations where not.

## `WorkoutTemplate` — add
- `restDaysAfter: Int`
- `orderIndex: Int`
- `category: String`

## `CycleSettings` — deprecate
- `cycleStartDate` — no longer used for scheduling (see
  `23_flexible_schedule_engine.md`). Leave column in place but unused, or
  remove in the same migration pass — either is fine, just don't build new
  logic against it.
- Add `adherenceGraceDays: Int` (default 1)

## `Exercise` — add (if not already present from guide/phase2)
- `imageAssetName: String?` — now populated from the free-exercise-db
  mapping per `22_exercise_media_sourcing.md`

## Remove/replace: supplement tracking

- **Remove:** `SupplementLog`'s fixed `wheyTaken/wheyServings/creatineTaken/creatineGrams`
  columns (after backfill migration below).
- **Add:** `Supplement` entity (see `25_customizable_supplements.md` for
  full field list).
- **Add:** `SupplementIntakeLog` entity (supplementId, date, taken,
  actualAmount).
- **Change:** `SupplyInventory.type: String` → `SupplyInventory.supplementId: Long` (FK).

## Removed feature (no schema needed)

- The old "reschedule cycle from here" calendar action is removed per
  `23_flexible_schedule_engine.md` — no schema impact, just delete the
  associated UI/logic.

## New DAOs

- `SupplementDao` — CRUD active/inactive supplements
- `SupplementIntakeLogDao` — insert/query by supplement + date range
- Update `SupplyInventoryDao` to query/join by `supplementId`
- Update `WorkoutTemplateDao` to support reordering (`orderIndex` updates)
  and category queries

## Migration sequencing (single Room migration recommended)

Do this as **one** migration covering the whole batch, in this order
internally:
1. Add new columns to `WorkoutTemplate` and `CycleSettings`, backfill
   sensible defaults for existing rows (`restDaysAfter`: 1 for the
   existing "A" template, 2 for "B", matching the current program;
   `orderIndex`: 0 and 1 respectively; `category`: leave blank/"Custom"
   for the user to fill in later).
2. Create `Supplement`, `SupplementIntakeLog` tables; run the backfill
   described in `25_customizable_supplements.md`'s migration plan.
3. Migrate `SupplyInventory.type` string values to `supplementId` FK
   references pointing at the newly created Whey/Creatine `Supplement` rows.
4. Only after 1–3 are verified (test per `16_stability_and_testing.md` §1),
   drop the now-unused old `SupplementLog` fixed columns.

Test this migration against a realistic pre-migration database (a few
weeks of real logged sessions and supplement logs, not an empty DB) before
trusting it against your actual data.
