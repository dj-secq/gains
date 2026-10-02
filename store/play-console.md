# Play Console — Gains 1.1

Upload `dist/Gains-1.1.aab`. The package is `com.example.repsgrams`. Version name `1.1`, version code `2`. The file is signed with the same certificate as the previous release (CN=Dhan Francisco).

Category: Health & Fitness. Tags: workout tracker, strength training.

## App name

Gains

## Short description

Local workout log for one phone. Programs, sets, rest, and progress.

## Full description

Gains is a workout log for one person on one phone. Your sessions, measurements, and supplement logs stay on the device. There is no account and no cloud.

Today tells you what to train, shows the week, and lists the next exercises. A program can rotate through workouts and rest days, or put one workout on each weekday. You log sets with a keypad. The rest timer can ring after you leave the screen. If you step out of a workout, it stays resumable. Discard is the only way to delete it.

The calendar and the progress chart use the same day marks. Green means you trained. Red means a day was due and missed, or a set was a personal record. Progress also keeps bodyweight, measurements, and how much of a supplement you have left.

You can switch kilograms and pounds, keep a home screen widget for today, and write a finished workout or a bodyweight entry to Health Connect when you turn that on. Gains does not read Health Connect. A backup is a zip you export yourself.

Gains does not include a social feed, nutrition tracking, ads, or a watch app.

## Release notes

The board is black and white. Green means done or trained. Red means not yet, missed, or a personal record.

Cards are simpler, and the type and spacing are easier to read. Today, Calendar, Progress, and Settings are unchanged in purpose. A workout still resumes after you leave it, and only Discard deletes it.

## Data safety

Answer these from the app’s behavior. Do not claim encryption or a server.

- Data is stored on the device in the app database. Gains has no account and does not upload workouts.
- No analytics SDK and no ads.
- Health Connect is optional. When it is on, Gains can write an exercise session for a finished workout and a weight record for a logged bodyweight. It does not read Health Connect.
- Reminders are scheduled on the device. The rest timer uses a foreground service and, when allowed, an exact alarm.
- Backup is a zip the user creates and stores wherever they choose. The app does not send that zip anywhere.
