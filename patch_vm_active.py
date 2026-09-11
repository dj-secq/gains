import re

with open("app/src/main/java/com/example/repsgrams/ui/session/WorkoutSessionViewModel.kt", "r") as f:
    text = f.read()

active_old = """    data class Active(
        val workoutName: String,
        val dayLabel: String,
        val elapsedSeconds: Int,
        val maxDurationMinutes: Int,
        val blockLabel: String,
        val blockKind: BlockKind,
        val blockNumber: Int,
        val blockCount: Int,
        val roundNumber: Int,
        val roundCount: Int,
        val exercise: WorkoutExercise,
        val valueInput: String,
        val weightInput: String,
        val unitSystem: UnitSystem,
        val restRemainingSeconds: Int?,
        val isOptionalBlock: Boolean,
        val isSaving: Boolean,
        val progressionSuggestion: ProgressionSuggestion?,
    ) : WorkoutSessionUiState"""
active_new = """    data class Active(
        val workoutName: String,
        val dayLabel: String,
        val elapsedSeconds: Int,
        val maxDurationMinutes: Int,
        val blockLabel: String,
        val blockKind: BlockKind,
        val blockNumber: Int,
        val blockCount: Int,
        val roundNumber: Int,
        val roundCount: Int,
        val exercise: WorkoutExercise,
        val valueInput: String,
        val weightInput: String,
        val unitSystem: UnitSystem,
        val restRemainingSeconds: Int?,
        val isOptionalBlock: Boolean,
        val isSaving: Boolean,
        val progressionSuggestion: ProgressionSuggestion?,
        val lastTimeRound: PriorRound?,
        val upNextExercises: List<String>,
        val rpeTagInput: String?,
        val notesInput: String,
    ) : WorkoutSessionUiState"""
text = text.replace(active_old, active_new)

# Add vars to ViewModel
vars_old = """    private var restEndEpochMillis: Long? = null
    private var isSaving: Boolean = false

    private val _restFinished = MutableSharedFlow<Unit>(extraBufferCapacity = 1)"""
vars_new = """    private var restEndEpochMillis: Long? = null
    private var isSaving: Boolean = false
    private var rpeTagInput: String? = null
    private var notesInput: String = ""
    private var lastTimeRound: PriorRound? = null

    private val _restFinished = MutableSharedFlow<Unit>(extraBufferCapacity = 1)"""
text = text.replace(vars_old, vars_new)

# In publishActive, fetch upNext
publish_old = """        _uiState.value = WorkoutSessionUiState.Active(
            workoutName = plan.name,
            dayLabel = plan.dayLabel,
            elapsedSeconds = elapsedSeconds(),
            maxDurationMinutes = plan.maxDurationMinutes,
            blockLabel = block.label,
            blockKind = block.kind,
            blockNumber = cursor.blockIndex + 1,
            blockCount = plan.blocks.size,
            roundNumber = cursor.roundNumber,
            roundCount = if (block.kind == BlockKind.WARM_UP) 1 else block.targetRoundsMax,
            exercise = currentExercise(),
            valueInput = valueInput,
            weightInput = weightInput,
            unitSystem = unitSystem,
            restRemainingSeconds = restEndEpochMillis?.let { end ->
                ((end - clock.millis()).coerceAtLeast(0) / 1_000L).toInt() + 1
            },
            isOptionalBlock = block.isOptional,
            isSaving = isSaving,
            progressionSuggestion = progressionSuggestion,
        )"""
publish_new = """        
        val upNext = mutableListOf<String>()
        var nextCursor = SessionNavigator.afterExercise(plan, cursor)
        var i = 0
        while (nextCursor is SessionAdvance.Continue && i < 2) {
            upNext.add(plan.blocks[nextCursor.cursor.blockIndex].exercises[nextCursor.cursor.exerciseIndex].name)
            nextCursor = SessionNavigator.afterExercise(plan, nextCursor.cursor)
            i++
        }
        if (nextCursor is SessionAdvance.Rest) {
             val c = nextCursor.cursorAfterRest
             upNext.add(plan.blocks[c.blockIndex].exercises[c.exerciseIndex].name)
        }

        _uiState.value = WorkoutSessionUiState.Active(
            workoutName = plan.name,
            dayLabel = plan.dayLabel,
            elapsedSeconds = elapsedSeconds(),
            maxDurationMinutes = plan.maxDurationMinutes,
            blockLabel = block.label,
            blockKind = block.kind,
            blockNumber = cursor.blockIndex + 1,
            blockCount = plan.blocks.size,
            roundNumber = cursor.roundNumber,
            roundCount = if (block.kind == BlockKind.WARM_UP) 1 else block.targetRoundsMax,
            exercise = currentExercise(),
            valueInput = valueInput,
            weightInput = weightInput,
            unitSystem = unitSystem,
            restRemainingSeconds = restEndEpochMillis?.let { end ->
                ((end - clock.millis()).coerceAtLeast(0) / 1_000L).toInt() + 1
            },
            isOptionalBlock = block.isOptional,
            isSaving = isSaving,
            progressionSuggestion = progressionSuggestion,
            lastTimeRound = lastTimeRound,
            upNextExercises = upNext,
            rpeTagInput = rpeTagInput,
            notesInput = notesInput,
        )"""
text = text.replace(publish_old, publish_new)

# Add loadInputDefaults updates
load_old = """    private suspend fun loadInputDefaults() {
        val exercise = currentExercise()
        valueInput = ""
        weightInput = ""
        val lastSessionId = workoutRepository.getMostRecentSessionIdWithExercise(exercise.id, beforeSessionId = sessionId)
        if (lastSessionId != null) {
            val priorRounds = workoutRepository.getSets(lastSessionId, exercise.id)
                .map { PriorRound(it.reps ?: it.durationSeconds, it.weightKg) }
            val relevantRound = priorRounds.getOrNull(cursor.roundNumber - 1) ?: priorRounds.lastOrNull()
            if (relevantRound != null) {
                valueInput = relevantRound.reps?.toString() ?: ""
                weightInput = relevantRound.weightKg?.let { formatWeight(it) } ?: ""
            }
            if (cursor.roundNumber == 1) {
                progressionSuggestion = ProgressionCalculator.suggestionFor(
                    exercise.targetValueHigh, exercise.tracksWeight, priorRounds
                )
            }
        }
    }"""
load_new = """    private suspend fun loadInputDefaults() {
        val exercise = currentExercise()
        valueInput = ""
        weightInput = ""
        rpeTagInput = null
        lastTimeRound = null
        val lastSessionId = workoutRepository.getMostRecentSessionIdWithExercise(exercise.id, beforeSessionId = sessionId)
        if (lastSessionId != null) {
            val priorRounds = workoutRepository.getSets(lastSessionId, exercise.id)
                .map { PriorRound(it.reps ?: it.durationSeconds, it.weightKg) }
            val relevantRound = priorRounds.getOrNull(cursor.roundNumber - 1) ?: priorRounds.lastOrNull()
            if (relevantRound != null) {
                valueInput = relevantRound.reps?.toString() ?: ""
                weightInput = relevantRound.weightKg?.let { formatWeight(it) } ?: ""
                lastTimeRound = relevantRound
            }
            if (cursor.roundNumber == 1) {
                progressionSuggestion = ProgressionCalculator.suggestionFor(
                    exercise.targetValueHigh, exercise.tracksWeight, priorRounds
                )
            }
        }
    }"""
text = text.replace(load_old, load_new)

with open("app/src/main/java/com/example/repsgrams/ui/session/WorkoutSessionViewModel.kt", "w") as f:
    f.write(text)
