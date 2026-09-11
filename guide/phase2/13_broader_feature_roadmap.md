# 13 — Broader Feature Roadmap

Features common in established workout-tracker apps (Strong, Hevy, Caliber,
and similar), evaluated for fit with this app and prioritized. "Now" =
build next after the items in files 10–12. "Next" = solid second batch.
"Later" = genuinely nice but either high-effort or lower-value for a
single-user personal app — don't start these until Now+Next are done.

## Now

### Personal Records (PRs) + celebration moment
Track best weight, best reps-at-a-weight, and best estimated 1RM per
exercise. When a logged set beats a stored PR, show a small celebratory
in-app moment (haptic + brief animation, per `11_workout_session_ui_polish.md`
§9) and store the new PR. Show a "PRs" list somewhere in Progress.
**Why now:** cheap to build off data you're already logging, and is one of
the most motivating features in every popular tracker for exactly this
reason.

### Estimated 1-rep-max (1RM)
Standard formula (e.g. Epley: `weight × (1 + reps/30)`) computed from
logged sets, shown per exercise in Progress alongside the rep/weight chart.
Advisory only, clearly labeled as an estimate.

### Body measurements (beyond bodyweight)
Extend `BodyweightLog`-style tracking to optional measurements: waist,
chest, arms, thighs, etc. — user picks which ones they care about in
Settings, logged on the same cadence as bodyweight. Adds a small multi-line
or tabbed chart to Progress.

### Local backup / export
Since this app is intentionally local-only (no cloud sync per
`01_overview.md`), add a manual **export to a single JSON/zip file**
(all tables) and **import** to restore — this is the safety net for a
local-only app and matters a lot once months of history exist. Simple
Settings action: "Export data" (share sheet) / "Import data" (file picker).
**Why now:** local-only + no backup is a real data-loss risk; this is
cheap insurance.

## Next

### Health Connect integration
Android's unified on-device health/fitness data platform (the successor to
the now-deprecating Google Fit APIs). Write completed `WorkoutSession`s as
`ExerciseSessionRecord`s and bodyweight as `WeightRecord`s, so this app's
data is visible to/compatible with other fitness apps and Wear OS the user
might use. Read-only pull-in isn't necessary — start with **write-only**
so this app remains the source of truth, and only add reading later if
there's a real need (e.g. pulling in steps). Requires Health Connect
permissions UI on first use.

### Achievements / milestones
Lightweight badges beyond PRs — e.g. "10-workout streak," "First month
complete," "50 total workouts logged." Purely local, no social component.
Keep this modest; it's a nice-to-have engagement layer, not a core feature.

### Home screen widget
A small Android widget showing today's plan (Workout A/B/Rest) + a
one-tap "Start Workout" or supplement quick-log, so the user doesn't have
to open the app to see what's up today.

### Custom exercise creation
Let the user add their own exercises (name, muscle group, optional
illustration/fallback badge, default rep range) beyond the seeded program,
and add them into existing or new workout templates via the program editor
from `05_screens_and_navigation.md`'s Settings screen.

### Voice/audio cues (accessibility + hands-free)
Optional spoken cues during a session ("Rest starting," "15 seconds left,"
"Next: Overhead Press") using Android's TextToSpeech API — genuinely useful
mid-set when looking at the phone isn't convenient, and doubles as an
accessibility win. Toggle in Settings, off by default to avoid surprising
new users.

## Later

### Wear OS companion
A minimal Wear OS app/tile showing just the rest timer and "log set"
button, so the phone can stay in a bag. Meaningful effort (separate
module, separate UI) — worth it only once the phone app is fully mature
and if the user actually has a Wear OS device.

### Progress photos
Optional photo log alongside body measurements, with a simple side-by-side
comparison view. Needs camera/storage permission handling and privacy
consideration (all local, never uploaded) — clearly communicate that.

### Training programs / periodization beyond the fixed rotation
Right now the app models exactly one fixed A/B/rest rotation. A future
version could support defining *multiple* programs and switching between
them (e.g. a deload week, a different rotation later). Significant data
model expansion — only worth it if/when the user's actual training needs
change beyond the current program.

### Social/sharing
Explicitly **not recommended** for this app given its stated scope
(`01_overview.md`: single user, no social features). Flagging only to
rule it out deliberately rather than by omission.

## Explicitly out of scope (confirmed, don't build)

- Multi-user accounts / login
- Cloud sync as a requirement (export/import covers the backup need instead)
- Nutrition/macro tracking beyond the whey protein estimate already planned
- Any social, sharing, or leaderboard feature
