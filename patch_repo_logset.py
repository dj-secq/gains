import re

with open("app/src/main/java/com/example/repsgrams/data/repository/WorkoutRepository.kt", "r") as f:
    text = f.read()

log_old = """    suspend fun logSet(
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
                loggedAt = Instant.now(clock),
                rpeTag = rpeTag,
                substitutedFrom = substitutedFrom,
            ),
        )
    }"""
log_new = """    suspend fun logSet(
        sessionId: Long,
        exerciseId: Long,
        roundNumber: Int,
        reps: Int?,
        durationSeconds: Int?,
        weightKg: Float?,
        rpeTag: String? = null,
        substitutedFrom: Long? = null,
    ): List<com.example.repsgrams.data.db.PersonalRecordEntity> {
        val session = database.workoutSessionDao().getById(sessionId) ?: return emptyList()
        val setId = database.setLogDao().insert(
            SetLogEntity(
                sessionId = sessionId,
                exerciseId = exerciseId,
                roundNumber = roundNumber,
                reps = reps,
                durationSeconds = durationSeconds,
                weightKg = weightKg,
                loggedAt = Instant.now(clock),
                rpeTag = rpeTag,
                substitutedFrom = substitutedFrom,
            ),
        )
        if (reps != null && weightKg != null) {
            val prManager = PRManager(database)
            return prManager.checkAndSavePR(exerciseId, reps, weightKg, setId, session.date)
        }
        return emptyList()
    }"""
if log_old in text:
    text = text.replace(log_old, log_new)
else:
    # try simpler match because of formatting
    log_old_2 = """    suspend fun logSet(
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
                loggedAt = Instant.now(clock),
            ),
        )
    }"""
    text = text.replace(log_old_2, log_new)

with open("app/src/main/java/com/example/repsgrams/data/repository/WorkoutRepository.kt", "w") as f:
    f.write(text)
