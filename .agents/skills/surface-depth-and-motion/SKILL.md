---
name: surface-depth-and-motion
description: Use when styling a Compose surface or writing motion for Gains. Tiles are flat. Motion is a short ease-out. Keep shimmer and predictive back.
---

# Flat tiles and short motion

Read color from `app-color-system` and components from `ios-style-ui`. Do not add shadow, glow, or a gradient to make a screen feel deeper.

## Surfaces

- Canvas is the screen background. Paper and ink are tiles. A 1dp hairline separates them.
- No drop shadow, no elevation tint, no accent glow, no gradient wash.
- Dialogs and sheets are paper squircles with `shadowElevation` 0.
- The session route forces the dark theme. Other tabs follow the picker.
- Loading content uses the hairline shimmer in `Board.kt`, shaped like the tile that will arrive. Do not leave a blank screen while settings or a calendar day are loading.

## Motion

Use `androidx.compose.animation.core.EaseOutCubic`. No spring and no bounce.

| Interaction | Duration |
|---|---|
| Tab crossfade, tile state | 150–250ms, tab crossfade is 200ms |
| Pushed route | 220ms slide |
| Pressed pill | alpha 0.55 for 180ms. Do not scale the pill. |

Predictive back stays enabled for pushed screens. A sheet uses its own dismiss. System back must not discard an in-progress workout.
