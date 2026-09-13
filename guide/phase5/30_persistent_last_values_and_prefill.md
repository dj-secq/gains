# 30 — Remember Previous Values & Prefill Inputs

`11_workout_session_ui_polish.md` §1 specced showing last time's numbers
as a *reference* next to the input. That's not enough — you're still
having to manually re-enter or re-adjust reps/kg/seconds every single
session from some default. This fixes it so the input actually **starts**
at last time's logged value.

## The fix

For every set logged, when the exercise card for round N of a given
exercise loads:

1. Query the most recent prior `SetLog` for **this exact exercise +
   this exact round number** (round 1 of Pull-Ups compares to round 1 of
   Pull-Ups last time, not round 3) via `SetLogDao`.
2. Pre-populate the input with that value as the **starting point**:
   - Reps-based: stepper starts at last time's `reps`, weight stepper
     starts at last time's `weightKg`.
   - Time-based (`29_work_interval_timer.md`): the target marker shown on
     the hold timer reflects last time's `actualSeconds`.
3. The user can still freely adjust from there (that's the point of a
   stepper) — this isn't locking the value, it's eliminating the
   *default-to-zero-or-generic-placeholder* problem so they're adjusting
   from a relevant starting point instead of starting from scratch every set.
4. If there's a "Ready to increase weight" progression suggestion active
   for this exercise (from `06_workout_session_and_timers.md`), prefill
   with last time's value **plus** the smallest sensible increment (e.g.
   +1kg or the next dumbbell size up) rather than exactly last time's
   number — the whole point of that suggestion is to nudge the starting
   point up, so make the prefill reflect that instead of requiring a
   manual bump on top of an unhelpful prefill.
5. If there's no prior session for this exercise at all (first time doing
   it), fall back to the exercise's seeded/default target range midpoint
   as the starting stepper value — never start at 0 for something with a
   clearly nonzero target range.

## Where this must NOT apply

- If the user **swapped** to a substitute exercise this session
  (`11_workout_session_ui_polish.md` §7), prefill from that substitute
  exercise's own history, not the originally-planned exercise's history —
  they're different movements even if logged in the same slot.
- Don't carry a prefill across into a **different round number** of the
  same exercise — round 1 and round 3 typically have different fatigue
  levels and shouldn't share a prefill source.

## Why this matters enough to fix now

This was the single most-cited value-add in the original app research
(`13_broader_feature_roadmap.md`, PRs/1RM section references this same
principle) — apps like Strong/Hevy live or die on this exact behavior, and
a "last time" number you have to manually copy over isn't meaningfully
different from not having it at all.
