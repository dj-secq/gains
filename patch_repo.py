import re

with open("app/src/main/java/com/example/repsgrams/data/repository/WorkoutRepository.kt", "r") as f:
    text = f.read()

logset_old = """    suspend fun logSet(
        sessionId: Long,
        exerciseId: Long,
        roundNumber: Int,
        reps: Int?,
        durationSeconds: Int?,
        weightKg: Float?,
    ) {
        database.setLogDao().insert(
            SetLogEntity(
                sessionId = sessionId,
                exerciseId = exerciseId,
                roundNumber = roundNumber,
                reps = reps,
                durationSeconds = durationSeconds,
                weightKg = weightKg,
                loggedAt = clock.instant(),
            )
        )
    }"""
logset_new = """    suspend fun logSet(
        sessionId: Long,
        exerciseId: Long,
        roundNumber: Int,
        reps: Int?,
        durationSeconds: Int?,
        weightKg: Float?,
        rpeTag: String? = null,
        substitutedFrom: Long? = null,
    ) {
        database.setLogDao().insert(
            SetLogEntity(
                sessionId = sessionId,
                exerciseId = exerciseId,
                roundNumber = roundNumber,
                reps = reps,
                durationSeconds = durationSeconds,
                weightKg = weightKg,
                loggedAt = clock.instant(),
                rpeTag = rpeTag,
                substitutedFrom = substitutedFrom,
            )
        )
    }"""
text = text.replace(logset_old, logset_new)

finish_old = """    suspend fun finishSession(sessionId: Long): WorkoutSessionEntity {
        val session = requireNotNull(database.workoutSessionDao().getById(sessionId)) { "Session not found" }
        val updated = session.copy(
            completed = true,
            endTime = clock.instant(),
            durationSeconds = session.startTime?.let { clock.instant().epochSecond - it.epochSecond }?.toInt()
        )
        database.workoutSessionDao().update(updated)"""
finish_new = """    suspend fun finishSession(sessionId: Long, notes: String? = null): WorkoutSessionEntity {
        val session = requireNotNull(database.workoutSessionDao().getById(sessionId)) { "Session not found" }
        val updated = session.copy(
            completed = true,
            endTime = clock.instant(),
            durationSeconds = session.startTime?.let { clock.instant().epochSecond - it.epochSecond }?.toInt(),
            notes = notes,
        )
        database.workoutSessionDao().update(updated)"""
text = text.replace(finish_old, finish_new)

with open("app/src/main/java/com/example/repsgrams/data/repository/WorkoutRepository.kt", "w") as f:
    f.write(text)
