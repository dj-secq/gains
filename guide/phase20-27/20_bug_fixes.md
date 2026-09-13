# 20 — Bug Fixes: Calendar Popup & Scroll

Fix these first, before any new feature work — they're isolated,
regression-prone if left, and likely to interact with the schedule engine
changes in `23_flexible_schedule_engine.md` if fixed after rather than before.

## Bug 1: Transparent popup when tapping a calendar day

**Likely root cause:** the day-detail popup (Dialog or ModalBottomSheet)
is missing an explicit container background/color, so it's rendering with
a transparent surface — this commonly happens when a custom-styled dialog
was hand-rolled during the `ios-style-ui` restyle and the default Material
`containerColor` got dropped in the process.

**Diagnosis steps for Codex:**
1. Locate the composable backing the day-detail popup. Confirm whether
   it's a `Dialog`, `AlertDialog`, or `ModalBottomSheet`.
2. Check whether a `containerColor` / background is explicitly set to a
   real value from the theme (e.g. `MaterialTheme.colorScheme.surface` or
   the app's custom surface token from `app-color-system`) — not omitted,
   not set to `Color.Transparent`, not inheriting from a parent that
   itself has no background.
3. If it's a custom `Dialog { ... }` composable (not `AlertDialog`), confirm
   the content is wrapped in a `Surface` or `Box` with `.background(...)`
   and appropriate shape/elevation — raw `Dialog` composables render
   whatever you put inside with no default background at all, unlike
   `AlertDialog`/`ModalBottomSheet` which supply one by default.
4. Check the scrim: confirm `DialogProperties` isn't set in a way that
   disables the dim background (e.g. don't need `dismissOnClickOutside`
   changes, but if `usePlatformDefaultWidth = false` was set for custom
   sizing, confirm the scrim wasn't inadvertently affected).

**Fix:** ensure the popup content sits inside a themed `Surface`/`Card`
using a real surface color + the shape scale from `ios-style-ui`, with a
proper scrim behind it. Test in both light and dark mode — a bug like this
sometimes only shows in one theme if a hardcoded color was involved.

## Bug 2: Calendar can't scroll properly

**Likely root cause:** nested scrollable containers fighting each other —
very common in Compose calendars, e.g. a horizontally-swipeable month
view (`HorizontalPager` or custom swipe gesture) nested inside a
vertically-scrollable `Column`/`LazyColumn`, or a day grid built with its
own `LazyVerticalGrid` placed inside another scrollable parent without
proper height constraints, causing gesture conflicts or infinite/zero
height issues.

**Diagnosis steps for Codex:**
1. Identify every scrollable modifier/composable in the calendar screen's
   composition tree (`verticalScroll`, `LazyColumn`, `LazyVerticalGrid`,
   `HorizontalPager`, `Pager`, custom `draggable`/`scrollable` modifiers).
2. Confirm there is exactly **one** primary vertical scroll container for
   the whole screen. A month's day-grid should be a fixed-height, non-
   scrolling grid (e.g. `LazyVerticalGrid` with
   `userScrollEnabled = false` and a bounded height, or a manually laid
   out grid) — it should not itself scroll independently of the screen.
3. If month-to-month navigation is swipe-based, that should be the
   **only** horizontal gesture handler on that section, and it shouldn't
   be nested inside something that also wants to consume vertical drag
   gestures without a `nestedScroll` connection set up correctly.
4. If a fixed-height grid is placed inside `Modifier.height(IntrinsicSize.Max)`
   within an unbounded-height parent (like inside another `LazyColumn`
   item without explicit constraints), that's a common source of both
   layout and scroll bugs — give the grid an explicit height or use
   `Modifier.heightIn()`/measured height instead of intrinsic sizing here.

**Fix approach:** restructure so there's one outer scroll owner (ideally a
single `LazyColumn` with the month header, the day grid, and the
legend/detail section as items), and the day grid itself has
`userScrollEnabled = false` with a fixed/bounded height so it lays out
predictably and doesn't compete for scroll gestures.

## Regression check after both fixes

- Tap every day type (past workout day, past rest day, today, future day)
  and confirm the popup renders opaque, correctly themed, in both light
  and dark mode.
- Scroll the full calendar screen — month navigation, and the outer page
  scroll if there's more content below the grid (legend, streak info) —
  and confirm smooth, single-direction-at-a-time behavior with no dead
  zones or fighting gestures.
