# 32 — Exercise Categorization (Actual Data)

The `muscleGroup` field was added to the schema back in guide/phase2
(`12_exercise_media_and_icons.md`), but it looks like it was never
actually populated with real values for the seeded exercises — that's why
everything still shows as uncategorized. This is the concrete data to
apply; no design decisions left, just fill it in.

## Categorization table — apply directly to seed data

| Exercise | `muscleGroup` |
|---|---|
| Jumping Jacks | Full Body |
| Arm Circles | Shoulders |
| Band Pull-Aparts | Back |
| Bodyweight Squats | Legs |
| Easy Push-Ups | Chest |
| Pull-Ups | Back |
| DB Floor Press | Chest |
| Single-Arm DB Row | Back |
| DB Overhead Press | Shoulders |
| DB Bicep Curl | Arms |
| Bulgarian Split Squat | Legs |
| Hanging Knee Raise | Core |
| Chin-Ups | Back |
| Push-Ups | Chest |
| DB Romanian Deadlift | Legs |
| Band Lateral Raise | Shoulders |
| DB Overhead Triceps Extension | Arms |
| Band Face Pull | Shoulders |
| Hollow Body Hold | Core |

These map to the muscle-group badge/icon system already specced in
`12_exercise_media_and_icons.md` §2 — no new design work needed, just
populate the `muscleGroup` field on each seeded `Exercise` row with the
value above (via the seed data callback / a data migration if exercises
already exist in a live database from earlier phases).

## Notes on borderline calls

A few of these hit more than one muscle group in reality — picking the
**primary** mover for a single-value field, consistent with the schema's
current single-`muscleGroup`-per-exercise design:
- **Chin-Ups**: primarily Back (lats), secondarily Arms (biceps) — filed
  under Back since that's the primary mover, same convention as Pull-Ups.
- **DB Romanian Deadlift**: primarily Legs (hamstrings/glutes) despite
  being a "posterior chain" movement often discussed alongside Back work.
- **Band Face Pull**: filed under Shoulders (rear delts) rather than Back,
  for consistency with Band Pull-Aparts... actually Band Pull-Aparts is
  filed under Back above — if you want perfect internal consistency
  between these two similar rear-delt movements, either both under
  Shoulders or both under Back is defensible; pick one and apply it to
  both. Recommend: both under **Back**, since that's how they function in
  this specific program (paired with pulling movements).
- **Jumping Jacks**: filed as Full Body since it's a warm-up cardio move,
  not targeting one muscle group — if the muscle-group filter UI in the
  Exercise Library doesn't have a natural place for "Full Body" as a
  filter option, add it as a valid `muscleGroup` value alongside Back/
  Chest/Shoulders/Arms/Legs/Core, per the original enum list in
  `12_exercise_media_and_icons.md`.

## If custom exercises get added later

Any new exercise added via the custom-exercise flow
(`13_broader_feature_roadmap.md` "Next" section /
`24_customizable_workouts_and_categories.md`) must require picking a
`muscleGroup` at creation time — don't let new exercises get created
without one, or this problem just recurs for every exercise added after
this fix.
