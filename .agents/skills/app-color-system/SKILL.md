---
name: app-color-system
description: Use when adding color, picking a theme palette, or tinting an icon in Gains. The board is black, white, and one signal red. A request for more color, category hues, or iOS Health badges stays inside that set.
---

# Board color

One palette. Do not add a second accent, a category hue, or a gradient because a screen feels flat. Flat is the design. Tokens live in `ui/theme/Color.kt`. `colorScheme.error` is signal red.

| Token | Light | Dark |
|---|---|---|
| Canvas | `#E8E8EA` | `#000000` |
| Paper | `#FFFFFF` | `#1A1A1A` |
| Ink | `#111111` | `#111111` tile, white type on the dark canvas |
| Hairline | `#E0E0E0` | `#333333` |
| Label | `#666666` | `#999999` |
| Signal | `#D71921` | `#D71921` |

Ink on paper is the primary text. Depth is ink versus paper versus canvas, plus a 1dp hairline. No surface tint, no colored badge behind an icon, no glow.

## Where signal red is allowed

- The 8dp live-session dot.
- The rest numeral at or after zero.
- A personal-record mark. An estimated one-rep max is not that mark.
- The destructive confirm label on a paper dialog.

The primary pill is black or white, never red and never blue. Missed days, overdue copy, and low supply are hairline or label color, not red. The on-screen Discard label, including "End workout early", is paper or `#999999`, not red. Do not use `#007AFF`, `#FF3B30`, `#FF2D20`, `#FF9500`, or `#FFCC00`.

Icons stay `Icons.Outlined` in ink or the label color. Do not invent a colored rounded-square badge.
