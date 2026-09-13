# 28 — Rest Timer Gaps & the +15s Bug

Two related but distinct bugs in the rest timer system from
`06_workout_session_and_timers.md` / `10_rest_timer_alarms.md`.

## Bug 1: No rest timer between superset blocks, or after warm-up

**What's happening:** the original spec only auto-started a rest timer
**within** a block, between rounds (e.g. between round 1 and round 2 of
Superset A). It never specified a rest gap when **transitioning between
blocks** — Warm-Up → Superset A, Superset A → Superset B, Superset B →
Superset C, Superset C → Optional Core. Right now the app is (correctly,
per the original spec) not resting there — but that spec was incomplete;
you need a breather between blocks too, not just between rounds of the
same block.

**Fix:**
- Add `restSecondsAfterBlock: Int?` to `TemplateBlock` — the rest applied
  once, after the *last* round of that block finishes, before the next
  block starts. Nullable/0 = no rest (appropriate for some transitions,
  e.g. maybe none needed after Optional Core since the session just ends).
- Set sensible defaults in the seed data: e.g. 60s after Warm-Up (settle
  in before the first working superset), and reuse each block's own
  `restSecondsBetweenRounds` value as a reasonable default for
  `restSecondsAfterBlock` between working supersets (editable independently
  per block after that).
- This is user-editable in the program editor (`05_screens_and_navigation.md`,
  `24_customizable_workouts_and_categories.md`) same as the between-round
  rest value — expose both as separate fields per block, clearly labeled
  ("Rest between rounds" vs. "Rest before next exercise group").

## Bug 2: Timer rings at the original time even after adding 15s

**Root cause to look for:** this is almost always a "two sources of
truth" bug — the countdown displayed in the UI is one value (updated when
+15s is tapped), but the actual alarm/notification/foreground-service
trigger that fires the ring was scheduled separately against a fixed
end-time computed once at timer start, and **never gets rescheduled** when
+15s is tapped. The UI looks correct (shows more time) right up until the
original, stale alarm fires anyway.

**Fix — single source of truth:**
- The rest timer should have exactly **one** authoritative value:
  `endTimestampMillis` (wall-clock target end time), held by the
  foreground service from `10_rest_timer_alarms.md`.
- The UI countdown is *derived* from `endTimestampMillis - now()`,
  recomputed on each tick — never an independently decremented counter.
- Tapping **+15s** must do exactly one thing: `endTimestampMillis += 15_000`.
  Do not separately update a UI-only counter and a backend alarm value —
  there should only be the one value to update.
- Whatever mechanism actually triggers the ring (an `AlarmManager` exact
  alarm, a `Handler`/coroutine delay inside the foreground service, or a
  scheduled notification) must read from that same `endTimestampMillis` at
  fire-check time, or be explicitly **cancelled and rescheduled** against
  the new value the moment +15s is tapped. Don't schedule a one-shot delay
  at timer start and forget about it.
- Add a regression test for this specifically: start a timer, wait, add
  15s partway through, and assert the ring fires at the new time, not the
  original one. This bug is easy to silently reintroduce later if the
  ring-trigger and the displayed countdown ever get decoupled again.

## Regression checklist

- [ ] Start a session, confirm a rest period now occurs after Warm-Up
      before Superset A begins
- [ ] Confirm a rest period occurs between Superset A → B → C transitions
      (using each block's `restSecondsAfterBlock`)
- [ ] Start a rest timer, tap +15s once, confirm ring fires 15s later than
      it would have originally — verify with a stopwatch, not just visually
- [ ] Tap +15s multiple times in a row, confirm each addition is honored
- [ ] Confirm this holds true whether the app is foregrounded or
      backgrounded/screen-off when +15s is tapped
