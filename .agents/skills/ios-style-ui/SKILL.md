---
name: ios-style-ui
description: Use when writing or editing Jetpack Compose UI for Gains, including screens, components, navigation, type, and shape. Also use when a request says iOS-like, iPhone-like, Cupertino, or clean. Apply the black-and-white widget board, not an iOS Health clone.
---

# Board UI

Gains is a light grey board of black and white tiles, with the workout screen always dark. A request for an iOS, Cupertino, or Health-app look is answered with this board. Product boundaries are in `guide/CURRENT.md`. Color is `app-color-system`. Motion is `surface-depth-and-motion`. Charts are `data-viz-style`.

The theme composable stays `RepsGramsTheme`. Do not rename it. The session route is wrapped in `RepsGramsTheme(ThemeMode.DARK)`.

## Type

Do not bundle SF Pro, NDot, NType, Lettera, or Nothing trademarks.

| Role | Face | Use |
|---|---|---|
| Display numerals | Doto, weight about 500, `ROND` 100 | Timer, load, streak, calendar hero, PR value. 28sp or larger. Not sentences. |
| Words | Space Grotesk | Names, titles, buttons, REST, DONE. No italic. |
| Labels and figures | Space Mono | Labels in capitals at 11sp, tracking about 0.08em. Units sit beside the number in the label color, smaller. |

## Shape and components

Build from `ui/components/Board.kt`.

- `BoardTile`: ink or paper, squircle or circle, 1dp hairline, no shadow. It does not force full width. The caller sets the size.
- `InkPill` and `OutlinePill`: 48dp tall. Width comes from the caller. No internal `fillMaxWidth`.
- `TextAction`: minimum height 48dp. The destructive flag is only for a confirm label on a paper dialog.
- `BoardDialog`: paper squircle.
- `MonoLabel` and `MonoChip`: Space Mono. A selected chip is inverted. An unselected chip is a hairline.
- Icons stay `Icons.Outlined`. One back chevron, content description "Back".

Shapes are a circle, a fully rounded 48dp pill, and a 24dp squircle. Touch targets are at least 48dp.

## Navigation

Four tabs: Today, Calendar, Progress, Settings. Tab state is a `rememberSaveable` index with a crossfade. There is no horizontal pager, so a sideways drag does not change tabs. The four tab screens do not repeat the tab name in a large collapsing title.

The bottom bar is a custom row, 48dp tall, Space Mono labels, `Role.Tab`. It is the only consumer of navigation-bar insets. Pushed routes use the shared back chevron. Predictive back stays on. System back on an in-progress workout leaves the session in place. Discard is the only delete path.

## Do not bring back

Inter, Google Fonts, `iosSpring()`, press-scale, drop shadows, gradients, category-colored badges, or a rainbow palette. Signal red is not the primary button. The rule for where red is allowed is in `app-color-system`.
