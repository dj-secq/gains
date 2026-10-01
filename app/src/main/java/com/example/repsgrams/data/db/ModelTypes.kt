package com.example.repsgrams.data.db

enum class RepType { REPS, SECONDS }

enum class BlockKind { WARM_UP, SUPERSET, STANDARD }

/** A finished training day, or an explicit rest. Freestyle is a later schema. */
enum class SessionKind { WORKOUT, REST }
