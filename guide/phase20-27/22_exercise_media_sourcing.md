# 22 — Exercise Media Sourcing (Open Source)

You asked to search for open source exercise images/icons rather than
generating a custom set. Here's what exists and what I'd actually use.

## Options researched

| Source | Content | License | Verdict |
|---|---|---|---|
| **free-exercise-db** (`yuhonas/free-exercise-db` on GitHub) | 800+ exercises, JSON metadata + real photo images per exercise | **Unlicense (public domain)** | **Recommended primary source.** No attribution required, safe for any use, large coverage, actively used as a base dataset by other projects. |
| **exercemus/exercises** | Curated exercise list combining wger + exercises.json | MIT (code); per-exercise license varies | Good as a metadata/reference cross-check, but mixed per-exercise licensing on some entries adds complexity — not needed if free-exercise-db already covers your program's exercises. |
| **wger project** | Full open-source workout manager with exercise images | AGPL 3+ (app code); images individually CC-licensed, varies per exercise | Only relevant if self-hosting the whole wger platform — not worth the licensing complexity just to lift a handful of images. Skip. |
| **RepDB free tier** | 250 exercises, stylized flat 512px WebP illustrations, EN/DE/ES instructions | Free for personal + commercial in-app use, **requires attribution** | Good alternative if you want a consistent flat-illustration look instead of photos — matches the "icon" aesthetic more than free-exercise-db's photos. Requires crediting RepDB somewhere (e.g. a Settings "Credits" screen — trivial for a personal app). |
| Gym Visual-derived datasets (e.g. `hasaneyldrm/exercises-dataset`) | GIF animations | Media requires a **separate paid license from Gym Visual** despite being on GitHub | **Avoid.** The repo's code is open but the media itself isn't actually free to use — don't get caught by this. |

## Recommendation

Use **free-exercise-db** as the primary source:
- Public domain (Unlicense) — no attribution needed, no risk.
- Covers essentially every exercise in your current program (Pull-Ups, DB
  Floor Press, Chin-Ups, Romanian Deadlift, etc.) plus plenty of headroom
  for the Exercise Library / custom exercises feature.
- Photos, not stylized icons — good for "what does this actually look
  like" reference during a workout, which is arguably more useful mid-set
  than a flat icon.

Optionally layer in **RepDB's free tier** later if you specifically want a
consistent illustrated icon look for list/browse views (Exercise Library
grid, category badges) while keeping free-exercise-db photos for the
in-session detail view — that's a nice-to-have split, not required for v1.

## Implementation plan

1. Since this app is fully offline/local-only, **bundle the images at
   build time** rather than fetching over network at runtime — download
   the subset of free-exercise-db images matching your actual exercise
   list once, add them to `res/drawable` or `assets/`, and reference them
   via `Exercise.imageAssetName` (already in the schema from
   `14_data_model_additions.md`).
2. Build a one-time mapping table (exercise name → free-exercise-db slug)
   for the ~15–20 exercises already in your program, plus any added later
   via the Exercise Library / custom exercise feature.
3. Add a small "Open Source Credits" entry in Settings noting the
   free-exercise-db (Unlicense) source — not legally required, but good
   practice and costs nothing.
4. Show the image:
   - **In the Exercise Library** (from `12_exercise_media_and_icons.md`):
     full card image.
   - **During a workout session:** a smaller thumbnail on the active
     exercise card, tap-to-expand to full size — this directly answers
     your "shown during workout" request.
   - **Fallback:** if an exercise has no matched image (e.g. a brand-new
     custom exercise the user just added), fall back to the colored
     initial/muscle-group badge already specced in
     `12_exercise_media_and_icons.md`, never a broken image icon.

## What NOT to do

- Don't fetch exercise images over the network at runtime for a supposedly
  offline-first app — bundle them.
- Don't mix media from the Gym Visual-derived datasets in — that media
  isn't actually free despite appearing in an open GitHub repo.
- Don't hand-pick images from random web searches per exercise — you'll
  end up with inconsistent style/quality and unclear licensing per image;
  stick to one sourced dataset.
