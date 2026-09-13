# 23 — Flexible Schedule: Suggest, Don't Force

This is the biggest change in this batch. Your request: the calendar
shouldn't *force* Workout A or B on a given day — it should let you decide
what to do, while suggesting/reminding what's "due" based on your actual
history and pattern. Streaks should reflect missed days, not deviation
from a rigid calendar.

## What changes conceptually

**Old model** (`01_overview.md` / `06_workout_session_and_timers.md`):
a fixed calendar formula — `cycleStartDate` + day-offset modulo 5 —
dictates that day's slot is always A, Rest, B, Rest, or Rest, regardless
of what actually happened.

**New model:** there is no fixed calendar formula. The "suggested next
workout" is **computed from your most recent actual session**, not from a
calendar date offset. The calendar becomes a record of what you did, plus
an advisory suggestion for today/future — never a constraint on what
you're allowed to do.

## The suggestion algorithm

Each active `WorkoutTemplate` has a `restDaysAfter: Int` (e.g. Workout A →
1 rest day after, Workout B → 2 rest days after — matches your current
program). Templates have an order (e.g. A → B → A → B...).

Given the most recently **completed** `WorkoutSession` (type = T, date = D):

1. Compute `dueDate = D + T.restDaysAfter + 1`.
2. If `today < dueDate`: suggestion = **Rest**, with "N days until
   [next template]" shown.
3. If `today == dueDate`: suggestion = **next template in order after T**,
   status = on schedule.
4. If `today > dueDate`: suggestion = same next template, status =
   **overdue by N days** — this is advisory, not blocking.
5. If there is no prior completed session at all (brand new install, or
   right after onboarding): suggestion = whichever template the user
   picked as "start with" during onboarding (see `17_onboarding_flow.md`,
   update that step to ask "which workout do you want to start with"
   instead of "when does Day 1 start").

## The key simplification this creates

Because the suggestion is derived entirely from actual logged history,
**user overrides need no special-case logic.** If the user does Workout B
when A was suggested, that's just... the new most recent session — the
algorithm naturally recomputes the next suggestion from it. There is no
"cycle" to reset or reschedule anymore. This replaces the old
`08_calendar_and_progress.md` "reschedule from here" feature entirely —
delete that feature, it's no longer needed.

## Today screen changes

- Show the suggestion prominently: "Suggested today: Workout B" (or "Rest
  day — Workout A due in 2 days"), with on-schedule/overdue styling.
- Primary action: "Start [suggested workout]".
- Secondary, clearly visible but not primary: "Do something else" → opens
  a picker of all active templates + "Rest today" — whatever the user
  picks and logs becomes the new basis for the next suggestion.
- If overdue, show it plainly but without guilt-tripping copy — e.g.
  "Workout A was due 2 days ago" in neutral styling, not red alarm styling
  unless it's meaningfully overdue (see streak grace period below).

## Calendar changes

- **Past days:** show what was *actually* logged (workout type completed,
  or explicitly logged rest, or nothing logged) — this is historical fact,
  rendered solid/filled per `app-color-system`.
- **Today and future days:** show the *computed suggestion* only, rendered
  in a visually distinct "advisory" style (e.g. dashed outline, lower
  opacity) so it's never confused with committed history — since future
  suggestions can shift if the user does something unexpected today.
- Remove the old "reschedule cycle" action entirely (superseded, see above).
- Day-detail popup (once `20_bug_fixes.md` is fixed) for a past day shows
  actual logged data; for today/future shows the suggestion + the "Start"/
  "Do something else" actions inline.

## Streak redefinition

This is where "streak matters if you miss a day," per your note.

- Define **adherence streak** as: the count of consecutive suggestion
  windows where the user acted within the expected window (on time or
  within grace), not tied to specific calendar dates.
- Add a configurable **grace period** (default: 1 day) — if the user logs
  the suggested workout up to `graceDays` late, the streak continues; if
  they exceed that, the streak breaks to 0 at that point.
- A **manually chosen deviation is not a break** — if the user does
  Workout B before A was "due," that's a valid logged session and simply
  becomes the new basis for the next suggestion; it doesn't break the
  streak, since the whole point is the user is allowed to choose.
- Streak only breaks on genuine **inactivity past the grace window** — no
  workout logged, no rest day explicitly acknowledged, for longer than
  `restDaysAfter + graceDays` since the last session.
- Rest-day supplement adherence (creatine) remains a **separate** streak/
  stat as already specced in `08_calendar_and_progress.md` — don't conflate
  workout-adherence streak with supplement-adherence streak; show both,
  labeled distinctly.

## Data model changes

- `WorkoutTemplate`: add `restDaysAfter: Int`, `orderIndex: Int` (defines
  rotation order for suggestions).
- `CycleSettings.cycleStartDate`: **deprecated** — no longer used for
  day-to-day slot computation. Either remove it in the migration or leave
  the column unused; don't build new logic on it.
- New settings: `adherenceGraceDays: Int` (default 1), configurable in
  Settings.
- `ScheduleRepository` is rewritten around "most recent completed
  session" queries instead of date-offset math — this replaces the unit
  tests from `16_stability_and_testing.md` §1 for rotation math; rewrite
  those tests against the new algorithm (test on-time, within-grace, and
  overdue suggestion states, plus the zero-history bootstrap case).

## What this does NOT change

- The actual workout content (blocks, supersets, rest timers, set logging)
  is untouched — this is purely about *which day suggests which workout*,
  not how a session itself runs.
- Supplement scheduling/reminders are untouched — creatine's "every day"
  and whey's "workout day only" logic don't depend on the rigid calendar
  and continue to work the same way (whey's "workout day" trigger now
  means "the day a workout was actually completed," which it already
  effectively meant).
