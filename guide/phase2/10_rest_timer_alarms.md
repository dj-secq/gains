# 10 — Rest Timer Alarms & Reliability

Your ask: "alarms after the rest timer" — right now (per `06_workout_session_and_timers.md`)
the rest timer just counts down with vibrate+sound at zero. This upgrades it
to something that reliably gets your attention even if the phone screen is
off, locked, or you've switched apps mid-rest.

## Problems this solves

- Rest timers in Compose that live only in a ViewModel/coroutine die or pause
  when the screen locks or Doze kicks in — the countdown silently drifts.
- A single vibrate+beep at zero is easy to miss if the phone's in a bag or
  across the room.

## What to build

### 1. Foreground service for the active rest timer
- When a rest timer starts, start a lightweight foreground service holding
  the countdown, with an ongoing notification showing live remaining time
  (e.g. "Resting — 0:42"). This keeps the timer accurate regardless of
  Doze/screen state and is the standard reliable pattern for this on Android.
- Stop the service when the timer ends or the user skips/cancels it.

### 2. Alarm-style completion, not just a notification tick
When the timer hits zero:
- **Full alarm-style sound** (not just a notification chime) — use a short,
  distinct tone, respecting the user's chosen volume/ringer settings. Let
  the user pick from 2–3 built-in tones in Settings.
- **Vibration pattern**, not a single buzz — a short repeating pulse (e.g.
  3× 200ms pulses) so it's noticeable even face-down.
- If the **screen is off or app backgrounded**, post a **high-priority
  notification** with the sound/vibration attached, so it surfaces even
  over lock screen, with actions: "Continue" and "+15s".
- If the **app is foregrounded** on the Session screen, skip the system
  notification and instead do an in-app full-screen flash/pulse + the same
  sound/vibration, so it doesn't feel like two separate alerts.

### 3. "Up next" context on the alert
The completion alert (both notification and in-app) should say what's
coming next, not just "rest over" — e.g. "Rest over — Round 3: DB Overhead
Press." Pull this from the session state already tracked per
`06_workout_session_and_timers.md`.

### 4. Auto-advance option
Add a Settings toggle: "Auto-start next round after rest ends" (default
on) vs. requiring a manual tap to continue — some users want to be forced
to confirm they're ready, others want zero friction.

### 5. Respect Do Not Disturb / silent mode sensibly
Rest-timer alerts should behave like a **timer/alarm**, not a generic
notification — on Android this generally means using an appropriately
categorized notification channel (e.g. alarm-like importance) so it's not
silently suppressed the way a low-priority chat notification would be.
Don't fight the user's explicit Do Not Disturb settings, but do use the
correct channel importance so it isn't accidentally treated as spam.

## New Settings additions

- Rest alert sound (picker, 2–3 built-in options)
- Rest alert vibration on/off
- Auto-advance after rest on/off
- "+15s" quick-add default increment (already in session UI — just confirm
  it's user-configurable now)

## Data/permissions impact

- Requires `FOREGROUND_SERVICE` and `FOREGROUND_SERVICE_SPECIAL_USE` (or
  the closest appropriate type) permissions, plus the existing
  `POST_NOTIFICATIONS` permission from `07_notifications_and_reminders.md`.
- No new Room tables needed — this is a runtime/service concern layered on
  the existing session state.
