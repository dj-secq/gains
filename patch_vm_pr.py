import re

with open("app/src/main/java/com/example/repsgrams/ui/session/WorkoutSessionViewModel.kt", "r") as f:
    text = f.read()

events_old = """    private val _summaryDone = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val summaryDone: SharedFlow<Unit> = _summaryDone.asSharedFlow()"""
events_new = """    private val _summaryDone = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val summaryDone: SharedFlow<Unit> = _summaryDone.asSharedFlow()
    
    private val _prAchieved = MutableSharedFlow<List<com.example.repsgrams.data.db.PersonalRecordEntity>>(extraBufferCapacity = 1)
    val prAchieved: SharedFlow<List<com.example.repsgrams.data.db.PersonalRecordEntity>> = _prAchieved.asSharedFlow()"""
text = text.replace(events_old, events_new)

log_old = """                workoutRepository.logSet(
                    sessionId = sessionId,
                    exerciseId = exercise.id,
                    roundNumber = cursor.roundNumber,
                    reps = value.takeIf { exercise.repType == RepType.REPS },
                    durationSeconds = value.takeIf { exercise.repType == RepType.SECONDS },
                    weightKg = weightInput.toFloatOrNull()
                        ?.let { if (unitSystem == UnitSystem.LB) it / POUNDS_PER_KILOGRAM else it },
                    rpeTag = rpeTagInput,
                )
            }.onSuccess {"""
log_new = """                workoutRepository.logSet(
                    sessionId = sessionId,
                    exerciseId = exercise.id,
                    roundNumber = cursor.roundNumber,
                    reps = value.takeIf { exercise.repType == RepType.REPS },
                    durationSeconds = value.takeIf { exercise.repType == RepType.SECONDS },
                    weightKg = weightInput.toFloatOrNull()
                        ?.let { if (unitSystem == UnitSystem.LB) it / POUNDS_PER_KILOGRAM else it },
                    rpeTag = rpeTagInput,
                )
            }.onSuccess { prs ->
                if (prs.isNotEmpty()) {
                    _prAchieved.tryEmit(prs)
                }"""
text = text.replace(log_old, log_new)

with open("app/src/main/java/com/example/repsgrams/ui/session/WorkoutSessionViewModel.kt", "w") as f:
    f.write(text)
