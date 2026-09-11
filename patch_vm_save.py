import re

with open("app/src/main/java/com/example/repsgrams/ui/session/WorkoutSessionViewModel.kt", "r") as f:
    text = f.read()

log_old = """                    durationSeconds = value.takeIf { exercise.repType == RepType.SECONDS },
                    weightKg = weightInput.toFloatOrNull()
                        ?.let { if (unitSystem == UnitSystem.LB) it / POUNDS_PER_KILOGRAM else it }
                )
            }.onSuccess {"""
log_new = """                    durationSeconds = value.takeIf { exercise.repType == RepType.SECONDS },
                    weightKg = weightInput.toFloatOrNull()
                        ?.let { if (unitSystem == UnitSystem.LB) it / POUNDS_PER_KILOGRAM else it },
                    rpeTag = rpeTagInput,
                )
            }.onSuccess {"""
text = text.replace(log_old, log_new)

finish_old = """    private suspend fun finishAndSummarize() {
        session = workoutRepository.finishSession(sessionId)
        progressStore.clear()
        restEndEpochMillis = null
        showSummary()
    }"""
finish_new = """    private suspend fun finishAndSummarize() {
        session = workoutRepository.finishSession(sessionId, notesInput.takeIf { it.isNotBlank() })
        progressStore.clear()
        restEndEpochMillis = null
        showSummary()
    }"""
text = text.replace(finish_old, finish_new)

with open("app/src/main/java/com/example/repsgrams/ui/session/WorkoutSessionViewModel.kt", "w") as f:
    f.write(text)
