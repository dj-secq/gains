import re

with open("app/src/main/java/com/example/repsgrams/ui/session/WorkoutSessionViewModel.kt", "r") as f:
    text = f.read()

# Add service control methods
service_methods = """    private fun startRestTimerService(endMillis: Long, upNext: String) {
        val intent = Intent(context, RestTimerService::class.java).apply {
            action = RestTimerService.ACTION_START
            putExtra(RestTimerService.EXTRA_END_MILLIS, endMillis)
            putExtra(RestTimerService.EXTRA_UP_NEXT, upNext)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    private fun addTimeToRestTimerService() {
        val intent = Intent(context, RestTimerService::class.java).apply {
            action = RestTimerService.ACTION_ADD_TIME
        }
        ContextCompat.startForegroundService(context, intent)
    }

    private fun stopRestTimerService() {
        val intent = Intent(context, RestTimerService::class.java).apply {
            action = RestTimerService.ACTION_STOP
        }
        context.startService(intent) // stop action is safe for startService
    }

"""
text = text.replace("    private fun currentExercise(): WorkoutExercise {", service_methods + "    private fun currentExercise(): WorkoutExercise {")

# Update addRestSeconds
add_rest_old = """    fun addRestSeconds(seconds: Int = 15) {
        if (isSaving) return
        val end = restEndEpochMillis ?: return
        restEndEpochMillis = end + seconds * 1_000L
        viewModelScope.launch { persistProgress() }
        publishActive()
    }"""
add_rest_new = """    fun addRestSeconds(seconds: Int = 15) {
        if (isSaving) return
        val end = restEndEpochMillis ?: return
        restEndEpochMillis = end + seconds * 1_000L
        addTimeToRestTimerService()
        viewModelScope.launch { persistProgress() }
        publishActive()
    }"""
text = text.replace(add_rest_old, add_rest_new)

# Update skipRest
skip_rest_old = """    fun skipRest() {
        if (restEndEpochMillis == null) return
        restEndEpochMillis = null
        viewModelScope.launch { persistProgress() }
        publishActive()
    }"""
skip_rest_new = """    fun skipRest() {
        if (restEndEpochMillis == null) return
        restEndEpochMillis = null
        stopRestTimerService()
        viewModelScope.launch { persistProgress() }
        publishActive()
    }"""
text = text.replace(skip_rest_old, skip_rest_new)

# Update applyAdvance(Rest)
# Needs to calculate upNext text. We need to look at the advance.cursorAfterRest to figure out what's next.
apply_advance_old = """            is SessionAdvance.Rest -> {
                progressionSuggestion = null
                cursor = advance.cursorAfterRest
                restEndEpochMillis = clock.millis() + advance.seconds * 1_000L
                persistProgress()
                loadInputDefaults()
                publishActive()
            }"""
apply_advance_new = """            is SessionAdvance.Rest -> {
                progressionSuggestion = null
                cursor = advance.cursorAfterRest
                restEndEpochMillis = clock.millis() + advance.seconds * 1_000L
                val nextExercise = plan.blocks[cursor.blockIndex].exercises[cursor.exerciseIndex]
                val upNext = "Up next: Round ${cursor.roundNumber} - ${nextExercise.name}"
                startRestTimerService(restEndEpochMillis!!, upNext)
                persistProgress()
                loadInputDefaults()
                publishActive()
            }"""
text = text.replace(apply_advance_old, apply_advance_new)

# Update tick
tick_old = """    private fun tick() {
        val end = restEndEpochMillis
        if (end != null && end <= clock.millis()) {
            restEndEpochMillis = null
            _restFinished.tryEmit(Unit)
            viewModelScope.launch { persistProgress() }
        }
        publishActive()
    }"""
tick_new = """    private fun tick() {
        val end = restEndEpochMillis
        if (end != null && end <= clock.millis()) {
            restEndEpochMillis = null
            _restFinished.tryEmit(Unit)
            stopRestTimerService()
            viewModelScope.launch { persistProgress() }
            
            // Check auto-advance (Phase 10)
            // TODO: Actually check cycle settings. Assuming true for now.
            // Wait, actually the ViewModel emits _restFinished and then UI does something, but if auto-advance is on, it shouldn't just sit there.
            // Oh wait, `restEndEpochMillis = null` means the rest UI closes, and the normal active exercise UI shows. That IS auto-advance.
            // If auto-advance is OFF, it would just sit at a "Ready?" state. We don't have that yet.
        }
        publishActive()
    }"""
text = text.replace(tick_old, tick_new)


with open("app/src/main/java/com/example/repsgrams/ui/session/WorkoutSessionViewModel.kt", "w") as f:
    f.write(text)
