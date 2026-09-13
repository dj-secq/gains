# 24 — Customizable Workouts & Categories

Builds directly on `23_flexible_schedule_engine.md` — since suggestions
now come from an ordered list of active templates rather than a hardcoded
A/B pair, it's natural to also let the user manage that list freely and
tag each entry with a category.

## Relationship to the "multiple programs" idea from an earlier batch

Note: this is **not** the same as the full "multiple named Programs you
switch between" feature specced earlier (guide/phase3 `18_bigger_features.md`
§1). This is simpler and addresses what you actually asked for now: one
active set of workout templates that you can freely add to, reorder, and
edit — not multiple separate switchable programs. Build this now; leave
the heavier multi-program-switching feature for if/when you actually want
two genuinely separate training plans to switch between later.

## Editable workout templates (expanding beyond fixed A/B)

In Settings' program editor (`05_screens_and_navigation.md`):

- **Add a new workout template** (not limited to 2) — name, category,
  `restDaysAfter`, and its blocks/exercises built the same way A and B are
  today.
- **Reorder templates** — drag-to-reorder the list, which sets
  `orderIndex` and therefore the rotation order the suggestion engine
  cycles through.
- **Delete a template** — only if it has no session history, or soft-delete
  (keep historical sessions intact, just remove it from future rotation).
- **Edit rest-after value per template** — expose `restDaysAfter` as a
  simple stepper in the template editor.
- **Edit rest-between-rounds per block** — already possible per
  `05_screens_and_navigation.md`'s program editor; confirm this is a real
  editable field, not just seeded and fixed.
- **Edit global default rest time** — a Settings-level default (e.g. 90s)
  applied when creating a new block, which the user can still override
  per-block.

## Categorization

Add a `category` field to `WorkoutTemplate` — freeform text, but seed a
suggested list the user can pick from or extend: **Push, Pull, Legs,
Upper, Lower, Full Body, Core, Custom**.

- Assign each category a color from the existing semantic palette
  (`app-color-system`) — reuse the workout-orange family with distinct
  shades per category, or introduce a small secondary palette specifically
  for categories if more than ~4 are in use simultaneously.
- Show the category as a small colored chip anywhere the workout name
  appears: Today's suggestion card, Calendar day cells/detail, Session
  screen header, History/Progress views.
- Category is descriptive/organizational only — it does not affect the
  suggestion engine's rotation logic (`orderIndex` and `restDaysAfter`
  still drive that). Don't conflate "category" with "scheduling rule."

## Exercise Library integration

The Exercise Library screen (`12_exercise_media_and_icons.md` §3) should
let filtering/browsing by the same category taxonomy where relevant (e.g.
"show me exercises used in Pull-tagged workouts"), though an exercise's
primary tag remains its `muscleGroup`, not the workout category — these
are related but distinct taxonomies (muscle group = body-part-based,
category = workout-structure-based like Push/Pull/Legs).

## Data model additions

- `WorkoutTemplate`: add `category: String`, confirm `restDaysAfter` and
  `orderIndex` exist (from `23_flexible_schedule_engine.md`).
- New `CategoryColor` mapping (can be a simple enum-to-color function in
  code rather than a DB table, since categories are a small fixed-ish set
  with user-extensible custom labels falling back to a default neutral
  color).

## Suggested build order within this file

1. Template CRUD (add/reorder/delete) — foundational, everything else
   hangs off having more than 2 templates work at all.
2. Category field + chip UI.
3. Rest-time editability confirmation/fixes.
4. Exercise Library category filter (lower priority, do last).
