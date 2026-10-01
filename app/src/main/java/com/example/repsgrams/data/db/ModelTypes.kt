package com.example.repsgrams.data.db

enum class RepType { REPS, SECONDS }

enum class BlockKind { WARM_UP, SUPERSET, STANDARD }

/** A finished training day, or an explicit rest. Freestyle is a later schema. */
enum class SessionKind { WORKOUT, REST }

/** Rotation keeps the A/B gap. Weekly names a weekday on each template. */
enum class ScheduleMode { ROTATION, WEEKLY }

/** Working is the only value current logging writes. Warm-up, drop, and failure are later. */
enum class SetType { WARMUP, WORKING, DROP, FAILURE }

/** Seeded names are backfilled. Anything else, including a custom exercise, stays other. */
enum class Equipment { BARBELL, DUMBBELL, MACHINE, BODYWEIGHT, BAND, OTHER }
