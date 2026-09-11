# 12 — Exercise Media, Icons & Navigation Polish

Your ask: "more icons and images specially the different workout" + "better
icons on the nav." This covers visual identity for exercises and navigation.

## 1. Exercise illustrations
Every `Exercise` should have a visual, not just a name in a list. Two
realistic options, pick one to start:

- **Option A — Illustrated icons (recommended to start):** simple
  line-art/duotone illustrations per exercise (a small person-doing-the-move
  glyph), styled consistently — flat, 2-color (outline + one accent from
  `app-color-system`'s workout-orange), roughly 64×64dp. Cheaper to produce
  consistently than photos, fits the calm iOS aesthetic, easy to generate
  or source as a matched set (e.g. via a stock fitness icon pack, or
  generated once and reused).
- **Option B — Short looping demo (GIF/short video/frame sequence):**
  higher production value, shows actual form, but needs real
  photography/video or licensed content per exercise and is meaningfully
  more work. Consider this a **Phase 2 upgrade** once Option A is in place
  and the app's core loop is solid — don't block on it now.

Either way: store as a drawable/asset reference on the `Exercise` entity
(`imageAssetName: String?`), with a graceful fallback (colored initial
badge) for any exercise missing art — never a broken image icon.

## 2. Muscle-group iconography
Tag each `Exercise` with a primary muscle group (`Back`, `Chest`,
`Shoulders`, `Arms`, `Legs`, `Core`) and show a small muscle-group icon
badge on exercise cards (Session screen, exercise library/editor). Use a
consistent simple body-outline icon set, colored per `app-color-system`'s
palette conventions extended with muscle-group hues if useful, or reuse the
Phosphor set's anatomy-adjacent icons where they fit.

## 3. Exercise library screen (new)
A dedicated browsable list of all exercises (separate from "editing the
program" in Settings) showing the illustration + name + muscle group for
each — useful once "swap exercise" (`11_workout_session_ui_polish.md` §7)
and custom exercises exist. Grid or list view, filterable by muscle group.

## 4. Bottom nav icon upgrade
Currently using default Material icons per the original plan. Upgrade to
the Phosphor set (per `app-color-system` §7) specifically for the 4 tab bar
destinations:

| Tab | Suggested icon |
|---|---|
| Today | House / home |
| Calendar | Calendar |
| Progress | Chart-line-up |
| Settings | Gear/sliders |

Use **outline weight** for inactive tabs, **fill/bold weight + accent
color** for the active tab — this state change alone reads as much more
polished than a color-only active state.

## 5. Category icons elsewhere
Apply the same treatment (outline → filled on active/selected) to:
- Supplement rows (whey droplet-ish icon, creatine capsule-ish icon)
- Streak flame icon
- Calendar day-type glyphs (small A/B letter chip or dumbbell glyph for
  workout days, a moon/coffee-cup icon for rest days)

## 6. Empty/placeholder states need art too
Empty Progress charts, an empty Exercise Library search, a freshly-started
app before any history exists — give each a simple friendly illustration
(reuse the exercise-illustration visual style) rather than plain text, per
`data-viz-style` §6.

## Data model additions

Add to `Exercise`: `muscleGroup: String`, `imageAssetName: String?`.

## Sourcing the actual art

Realistic options for Codex/you to produce the illustration set:
1. Generate a matched icon set once (consistent style/prompt) via an image
   tool and import as drawables — most consistent, one-time effort.
2. License a fitness icon pack (many exist with 100+ exercise glyphs in a
   single consistent style) — fastest, avoids consistency drift.
3. Hand-source individually — not recommended, style will drift across ~20+
   exercises.

Either 1 or 2 — don't mix styles from multiple sources in the same app.
