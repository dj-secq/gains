import re

with open("app/src/main/java/com/example/repsgrams/data/repository/WorkoutRepository.kt", "r") as f:
    text = f.read()

finish_old = """    suspend fun finishSession(sessionId: Long): WorkoutSessionEntity {
        val session = requireNotNull(database.workoutSessionDao().getById(sessionId))
        if (session.completed) return session
        val end = Instant.now(clock)
        val duration = session.startTime?.let { start ->
            ((end.toEpochMilli() - start.toEpochMilli()).coerceAtLeast(0) / 1_000).toInt()
        }
        val finished = session.copy(endTime = end, completed = true, durationSeconds = duration)"""
finish_new = """    suspend fun finishSession(sessionId: Long, notes: String? = null): WorkoutSessionEntity {
        val session = requireNotNull(database.workoutSessionDao().getById(sessionId))
        if (session.completed) return session
        val end = Instant.now(clock)
        val duration = session.startTime?.let { start ->
            ((end.toEpochMilli() - start.toEpochMilli()).coerceAtLeast(0) / 1_000).toInt()
        }
        val finished = session.copy(endTime = end, completed = true, durationSeconds = duration, notes = notes)"""
text = text.replace(finish_old, finish_new)

with open("app/src/main/java/com/example/repsgrams/data/repository/WorkoutRepository.kt", "w") as f:
    f.write(text)
