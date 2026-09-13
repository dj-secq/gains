# 21 — Theme Depth & Motion Polish

Your feedback: dark theme feels like flat plain-black, light theme feels
too bright/plain white, and transitions/animations should feel faster and
smoother. This doc specs the fix; the actual rules live in a new skill
(`surface-depth-and-motion`) so Antigravity applies them consistently
everywhere, not just wherever this doc is read once.

## The core problem

A single flat background color (`#000000` or `#FFFFFF`) with cards that
are just a slightly different flat color reads as cheap/generic. Real iOS
apps (and good Android apps) build depth from **layered surface tones**,
**soft shadows/glows**, and **occasional subtle gradients** — not from
color alone.

## Dark theme fix

- Base background: not pure black — use a near-black with a very slight
  warm or cool tint (e.g. `#0B0B0D`) depending on the accent palette's
  temperature.
- Layer at least 3 distinct surface tones for depth (background → card →
  elevated card/sheet), each a small step lighter, not just one shared
  "card gray":
  - Level 0 (screen background): near-black
  - Level 1 (standard cards): +3–4% lightness
  - Level 2 (elevated: sheets, active/selected cards): +6–8% lightness
- Add a soft, low-opacity colored glow behind key hero elements (e.g. the
  Today screen's workout card) using that section's semantic accent color
  at very low opacity, rather than a plain flat card.
- Use soft drop shadows on elevated surfaces even in dark mode (a subtle
  darker-than-background shadow still reads even against black — don't
  skip shadows just because it's dark mode).

## Light theme fix

- Base background: not pure white — use a soft off-white/warm or cool
  gray (e.g. `#F7F7F9` or `#FAF9F6`) to reduce glare.
- Cards: pure or near-white, with a soft, real shadow (not just a border)
  to separate from the tinted background — this is what creates the
  "floating card" depth iOS is known for.
- Same accent-glow treatment as dark mode, adapted for light backgrounds
  (a soft colored wash rather than a glow, since glows read better on dark).

## Motion fix — faster and smoother

- Reduce standard transition duration from the original 250–350ms range to
  **180–260ms** for screen transitions, **100–150ms** for micro-interactions
  (button press, toggle, chip selection) — the earlier spec was fine
  structurally but erred slow; tighten it.
- Increase spring `stiffness` slightly and reduce bounce
  (`Spring.DampingRatioLowBouncy` → `Spring.DampingRatioMediumBouncy`) for
  primary navigation transitions specifically, reserving the bouncier
  spring for small delightful moments (PR celebration, streak increment)
  rather than routine navigation, which should feel snappy, not springy.
- Add **shared-continuity transitions**: when navigating from a list item
  to its detail (e.g. a calendar day cell to its detail popup, an exercise
  card to its library detail), animate a shared visual anchor (position/
  size/color) between the two rather than a plain cross-fade — this is a
  major perceived-smoothness upgrade and Compose's shared element APIs
  support it directly.
- Support Android's predictive back gesture animation for pushed screens
  rather than a static pop — this is now a platform-level expectation for
  smooth navigation.
- Replace blank loading spinners with lightweight **shimmer/skeleton
  placeholders** (e.g. on Progress charts while data loads) — this doesn't
  make anything technically faster, but it reads as considerably faster
  and smoother to the user, which is the actual goal here.

## Where this applies

Everywhere — this isn't a one-screen fix. Prioritize Today, Session, and
Calendar first since they're used most, then Progress and Settings.
