package com.example.repsgrams.data.db

enum class RepType { REPS, SECONDS }

enum class BlockKind { WARM_UP, SUPERSET, STANDARD }

/** A finished training day, an explicit rest, or an empty workout with no template. */
enum class SessionKind { WORKOUT, REST, FREESTYLE }

/** Rotation keeps the A/B gap. Weekly names a weekday on each template. */
enum class ScheduleMode { ROTATION, WEEKLY }

/** Working is the only value current logging writes. Warm-up, drop, and failure are later. */
enum class SetType { WARMUP, WORKING, DROP, FAILURE }

/** Seeded names are backfilled. Anything else, including a custom exercise, stays other. */
enum class Equipment { BARBELL, DUMBBELL, MACHINE, BODYWEIGHT, BAND, OTHER }
