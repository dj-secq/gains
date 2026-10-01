package com.example.repsgrams.ui.session

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.repsgrams.service.RestAlarmScheduler
import com.example.repsgrams.service.RestAlertPlayback
import com.example.repsgrams.service.RestTimerService
import com.example.repsgrams.service.restSecondsUntil
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.repsgrams.data.datastore.SessionProgress
import com.example.repsgrams.data.datastore.SessionProgressStore
import com.example.repsgrams.data.datastore.CycleSettingsRepository
import com.example.repsgrams.data.datastore.UnitSystem
import com.example.repsgrams.data.db.BlockKind
import com.example.repsgrams.data.db.RepType
import com.example.repsgrams.data.db.SetLogEntity
import com.example.repsgrams.data.db.WorkoutSessionEntity
import com.example.repsgrams.data.repository.SupplementRepository
import com.example.repsgrams.data.repository.WorkoutRepository
import com.example.repsgrams.domain.session.KeypadField
import com.example.repsgrams.domain.session.LoggedSetView
import com.example.repsgrams.domain.session.RecordLine
import com.example.repsgrams.domain.session.SessionAdvance
import com.example.repsgrams.domain.session.SessionCursor
import com.example.repsgrams.domain.session.SessionNavigator
import com.example.repsgrams.domain.session.WorkoutExercise
import com.example.repsgrams.domain.session.WorkoutPlan
import com.example.repsgrams.domain.session.applyLoadKey
import com.example.repsgrams.domain.session.applyRepKey
import com.example.repsgrams.domain.session.buildSetRows
import com.example.repsgrams.domain.session.durationPassed
import com.example.repsgrams.domain.session.exercisePills
import com.example.repsgrams.domain.session.formatPrevious
import com.example.repsgrams.domain.session.formatRecordLine
import com.example.repsgrams.domain.session.formatWeight
import com.example.repsgrams.domain.session.loadStep
import com.example.repsgrams.domain.session.nextExerciseCaption
import com.example.repsgrams.domain.session.prEligibleExerciseIds
import com.example.repsgrams.ui.today.TodaySupplement
import java.time.Clock
import com.example.repsgrams.domain.progression.PriorRound
import com.example.repsgrams.domain.progression.ProgressionCalculator
import com.example.repsgrams.domain.progression.ProgressionSuggestion
import com.example.repsgrams.domain.progress.kilogramsToPounds
import com.example.repsgrams.domain.progress.poundsToKilograms
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface WorkoutSessionUiState {
    data object Loading : WorkoutSessionUiState
    data class Active(
        val elapsedSeconds: Int,
        val maxDurationMinutes: Int,
        val durationPassed: Boolean,
        val blockLabel: String,
        val exercise: WorkoutExercise,
        val pills: List<com.example.repsgrams.domain.session.ExercisePill>,
        val rows: List<com.example.repsgrams.domain.session.SetRowModel>,
        val keypadOpen: Boolean,
        val keypadField: KeypadField,
        val restRemainingSeconds: Int?,
        val restOvertimeSeconds: Int?,
        val restCaption: String,
        val isOptionalBlock: Boolean,
        val isSaving: Boolean,
        val notesInput: String,
        val isHolding: Boolean,
        val holdElapsedSeconds: Int,
    ) : WorkoutSessionUiState

    data class Summary(
        val workoutName: String,
        val setCount: Int,
        val durationSeconds: Int,
        val notes: String,
        val supplements: List<TodaySupplement>,
        val records: List<RecordLine>,
    ) : WorkoutSessionUiState

    data class Error(val message: String) : WorkoutSessionUiState
}

class WorkoutSessionViewModel(
    context: Context,
    private val sessionId: Long,
    private val workoutRepository: WorkoutRepository,
    private val progressStore: SessionProgressStore,
    private val supplementRepository: SupplementRepository,
    private val cycleSettingsRepository: CycleSettingsRepository,
    private val clock: Clock,
) : ViewModel() {
    private val applicationContext = context.applicationContext
    private val _uiState = MutableStateFlow<WorkoutSessionUiState>(WorkoutSessionUiState.Loading)
    val uiState: StateFlow<WorkoutSessionUiState> = _uiState.asStateFlow()

    private val _restFinished = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val restFinished: SharedFlow<Unit> = _restFinished.asSharedFlow()
    private val _summaryDone = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val summaryDone: SharedFlow<Unit> = _summaryDone.asSharedFlow()

    private val _provisionalRecord = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val provisionalRecord: SharedFlow<Unit> = _provisionalRecord.asSharedFlow()

    private val _keepScreenOn = MutableStateFlow(true)
    val keepScreenOn: StateFlow<Boolean> = _keepScreenOn.asStateFlow()

    private lateinit var session: WorkoutSessionEntity
    private lateinit var plan: WorkoutPlan
    private var cursor = SessionCursor(0, 0, 1)
    private var restEndEpochMillis: Long? = null
    private var restToken: Long = 0L
    private var restCaption: String = ""
    private var restAlertFired: Boolean = false
    private var ready = false
    private val loaded = CompletableDeferred<Unit>()
    private var valueInput = ""
    private var weightInput = ""
    private var isSaving = false
    private var unitSystem = UnitSystem.KG
    private var progressionSuggestion: ProgressionSuggestion? = null
    private var rpeTagInput: String? = null
    private var notesInput: String = ""
    private var holdStartEpochMillis: Long? = null
    private var keypadOpen = true
    private var keypadField = KeypadField.VALUE
    private var keypadFresh = true
    private var sessionLogs: List<SetLogEntity> = emptyList()
    private var previousByRound: Map<Int, SetLogEntity?> = emptyMap()

    init {
        viewModelScope.launch {
            cycleSettingsRepository.settings.collect { _keepScreenOn.value = it.keepScreenOn }
        }
        viewModelScope.launch {
            runCatching { load() }.onFailure {
                _uiState.value = WorkoutSessionUiState.Error(it.message ?: "Couldn't load this workout.")
            }
            if (!loaded.isCompleted) loaded.complete(Unit)
        }
        // The service flow is a mirror. Null emissions must not be written back over the stored deadline.
        viewModelScope.launch {
            loaded.await()
            if (!ready) return@launch
            progressStore.progress.collect { saved ->
                if (saved == null || saved.sessionId != sessionId) return@collect
                val changed = restEndEpochMillis != saved.restEndEpochMillis ||
                    restAlertFired != saved.restAlertFired ||
                    restCaption != saved.restCaption ||
                    restToken != saved.restToken
                restEndEpochMillis = saved.restEndEpochMillis
                restAlertFired = saved.restAlertFired
                restCaption = saved.restCaption
                restToken = saved.restToken
                if (changed && _uiState.value is WorkoutSessionUiState.Active) publishActive()
            }
        }
        viewModelScope.launch {
            while (isActive) {
                delay(500)
                if (_uiState.value is WorkoutSessionUiState.Active && !isSaving && !session.completed) tick()
            }
        }
    }

    private suspend fun load() {
        session = requireNotNull(workoutRepository.getSession(sessionId)) { "Workout session not found" }
        val templateId = requireNotNull(session.templateId) { "This workout template was deleted" }
        plan = workoutRepository.loadPlan(templateId)
        val settings = cycleSettingsRepository.settings.first()
        unitSystem = settings.unitSystem
        _keepScreenOn.value = settings.keepScreenOn
        if (session.completed) {
            showSummary()
            return
        }
        if (plan.blocks.isEmpty() || plan.blocks.any { it.exercises.isEmpty() }) {
            _uiState.value = WorkoutSessionUiState.Error("This workout has a block with no exercises.")
            return
        }
        val saved = progressStore.progress.first()?.takeIf { it.sessionId == sessionId }
        if (saved != null && saved.blockIndex in plan.blocks.indices &&
            saved.exerciseIndex in plan.blocks[saved.blockIndex].exercises.indices
        ) {
            cursor = SessionCursor(saved.blockIndex, saved.exerciseIndex, saved.roundNumber)
            restEndEpochMillis = saved.restEndEpochMillis
            restCaption = saved.restCaption
            restAlertFired = saved.restAlertFired
            restToken = saved.restToken
            if (saved.notes.isNotEmpty()) notesInput = saved.notes
        } else {
            persistProgress()
        }
        // A past deadline stays on screen as overtime. Do not clear it here.
        if (restEndEpochMillis != null) {
            if (restCaption.isBlank()) {
                restCaption = restCaptionForCursor()
                persistProgress()
            }
            startRestTimerService(restEndEpochMillis!!, restCaption)
        }
        loadInputDefaults()
        ready = true
        publishActive()
    }

    fun updateValue(value: String) {
        if (value.all(Char::isDigit)) {
            valueInput = value.take(4)
            publishActive()
        }
    }

    fun startHold() {
        if (restEndEpochMillis != null || currentExercise().repType != RepType.SECONDS) return
        holdStartEpochMillis = clock.millis()
        keypadOpen = false
        publishActive()
    }

    fun stopHold() {
        val start = holdStartEpochMillis ?: return
        val elapsed = ((clock.millis() - start) / 1000).toInt()
        holdStartEpochMillis = null
        valueInput = elapsed.toString()
        logCurrent()
    }

    fun updateWeight(value: String) {
        if (value.isEmpty() || value.matches(Regex("\\d{0,4}(\\.\\d{0,2})?"))) {
            weightInput = value
            publishActive()
        }
    }

    fun adjustValue(delta: Int) {
        valueInput = ((valueInput.toIntOrNull() ?: 0) + delta).coerceAtLeast(0).toString()
        publishActive()
    }

    fun updateNotes(notes: String) {
        notesInput = notes
        publishActive()
        viewModelScope.launch {
            withContext(NonCancellable) { progressStore.saveNotes(sessionId, notes) }
        }
    }

    /** Leaves the session on screen. The row and its sets stay so Today can resume it. */
    suspend fun leaveWorkout() {
        withContext(NonCancellable) { progressStore.saveNotes(sessionId, notesInput) }
    }
    fun updateRpeTag(tag: String?) {
        rpeTagInput = tag
        publishActive()
    }
    fun adjustWeight(delta: Float) {
        weightInput = ((weightInput.toFloatOrNull() ?: 0f) + delta).coerceAtLeast(0f).let(::formatWeight)
        publishActive()
    }

    fun aimKeypad(field: KeypadField) {
        if (restEndEpochMillis != null || holdStartEpochMillis != null) return
        if (field == KeypadField.LOAD && !currentExercise().tracksWeight) return
        keypadField = field
        keypadOpen = true
        keypadFresh = true
        publishActive()
    }

    fun dismissKeypad() {
        keypadOpen = false
        publishActive()
    }

    fun keypadKey(key: String) {
        if (restEndEpochMillis != null || holdStartEpochMillis != null || !keypadOpen) return
        val edited = if (keypadField == KeypadField.LOAD) {
            applyLoadKey(weightInput, key, keypadFresh, loadStep(unitSystem))
        } else {
            applyRepKey(valueInput, key, keypadFresh)
        }
        if (keypadField == KeypadField.LOAD) weightInput = edited.text else valueInput = edited.text
        keypadFresh = edited.fresh
        publishActive()
    }

    /** Copies the previous column into the active inputs. Does not complete the set. */
    fun copyPreviousIntoActive() {
        if (restEndEpochMillis != null || holdStartEpochMillis != null) return
        val previous = previousByRound[cursor.roundNumber] ?: return
        val exercise = currentExercise()
        val figure = if (exercise.repType == RepType.SECONDS) previous.durationSeconds else previous.reps
        if (figure != null) valueInput = figure.toString()
        if (exercise.tracksWeight && previous.weightKg != null) {
            weightInput = formatWeight(displayWeight(previous.weightKg))
        }
        keypadOpen = true
        keypadFresh = true
        publishActive()
    }

    fun toggleSummarySupplement(supplementId: Long) {
        val summary = _uiState.value as? WorkoutSessionUiState.Summary ?: return
        _uiState.value = summary.copy(
            supplements = summary.supplements.map { row ->
                if (row.supplement.id == supplementId) row.copy(taken = !row.taken) else row
            },
        )
    }

    fun logCurrent() {
        if (isSaving || restEndEpochMillis != null) return
        val value = valueInput.toIntOrNull() ?: return
        val exercise = currentExercise()
        isSaving = true
        publishActive()
        val block = plan.blocks[cursor.blockIndex]
        val weightKg = weightInput.toFloatOrNull()
            ?.let { if (unitSystem == UnitSystem.LB) poundsToKilograms(it) else it }
            .takeIf { exercise.tracksWeight }
        viewModelScope.launch {
            runCatching {
                workoutRepository.logSet(
                    sessionId = sessionId,
                    exerciseId = exercise.id,
                    roundNumber = cursor.roundNumber,
                    reps = value.takeIf { exercise.repType == RepType.REPS },
                    durationSeconds = value.takeIf { exercise.repType == RepType.SECONDS },
                    weightKg = weightKg,
                    rpeTag = null,
                )
                val reps = value.takeIf { exercise.repType == RepType.REPS }
                if (block.kind != BlockKind.WARM_UP &&
                    reps != null &&
                    weightKg != null &&
                    exercise.id in prEligibleExerciseIds(plan) &&
                    workoutRepository.wouldRecordPersonalRecord(exercise.id, reps, weightKg)
                ) {
                    _provisionalRecord.emit(Unit)
                }
                applyAdvance(SessionNavigator.afterExercise(plan, cursor))
            }.onFailure {
                _uiState.value = WorkoutSessionUiState.Error(it.message ?: "Couldn't save this set.")
            }
            isSaving = false
            if (_uiState.value is WorkoutSessionUiState.Active) publishActive()
        }
    }

    fun skipOptionalBlock() {
        if (!plan.blocks[cursor.blockIndex].isOptional) return
        viewModelScope.launch { applyAdvance(SessionNavigator.skipBlock(plan, cursor.blockIndex)) }
    }

    fun addRestSeconds(seconds: Int = 15) {
        val end = restEndEpochMillis ?: return
        val updated = end + seconds * 1_000L
        restEndEpochMillis = updated
        if (updated > clock.millis()) restAlertFired = false
        publishActive()
        adjustRestTimerService(seconds * 1_000L)
    }

    fun onSessionResumed() {
        val end = restEndEpochMillis ?: return
        if (end <= clock.millis()) return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val alarmManager = applicationContext.getSystemService(AlarmManager::class.java) ?: return
        if (!alarmManager.canScheduleExactAlarms()) return
        RestAlarmScheduler.schedule(applicationContext, end)
    }

    fun skipRest() {
        val skippedEnd = restEndEpochMillis ?: return
        val skippedToken = restToken
        restEndEpochMillis = null
        restToken = 0L
        restCaption = ""
        restAlertFired = false
        publishActive()
        viewModelScope.launch {
            val cleared = if (skippedToken != 0L) {
                progressStore.clearRestForToken(skippedToken)
            } else {
                progressStore.clearRestDeadlineIfMatch(skippedEnd)
            }
            val saved = progressStore.progress.first()
            val replaced = saved?.restEndEpochMillis != null &&
                (skippedToken == 0L || saved.restToken != skippedToken)
            if (!cleared || replaced) {
                if (saved != null && saved.sessionId == sessionId && saved.restEndEpochMillis != null) {
                    restEndEpochMillis = saved.restEndEpochMillis
                    restToken = saved.restToken
                    restCaption = saved.restCaption
                    restAlertFired = saved.restAlertFired
                    publishActive()
                }
                return@launch
            }
            stopRestIfStill(skippedEnd, skippedToken)
        }
    }

    /**
     * Stops the timer, alarm, and wake lock before the caller leaves the screen.
     * The navigation scope is cancelled on pop, so this work is non-cancellable.
     */
    suspend fun discardWorkout() {
        withContext(NonCancellable) {
            restEndEpochMillis = null
            restToken = 0L
            restCaption = ""
            restAlertFired = false
            stopRestTimerService()
            progressStore.clear()
            workoutRepository.deleteSession(sessionId)
        }
    }

    fun previousStep() {
        if (cursor.blockIndex == 0 && cursor.exerciseIndex == 0 && cursor.roundNumber == 1) return
        holdStartEpochMillis = null

        var prevCursor = cursor
        if (cursor.exerciseIndex > 0) {
            prevCursor = cursor.copy(exerciseIndex = cursor.exerciseIndex - 1)
        } else if (cursor.roundNumber > 1) {
            prevCursor = SessionCursor(cursor.blockIndex, plan.blocks[cursor.blockIndex].exercises.lastIndex, cursor.roundNumber - 1)
        } else {
            var prevBlockIndex = cursor.blockIndex - 1
            while (prevBlockIndex >= 0 && plan.blocks[prevBlockIndex].exercises.isEmpty()) prevBlockIndex -= 1
            if (prevBlockIndex < 0) return
            val prevBlock = plan.blocks[prevBlockIndex]
            prevCursor = SessionCursor(prevBlockIndex, prevBlock.exercises.lastIndex, prevBlock.targetRoundsMax)
        }

        cursor = prevCursor

        viewModelScope.launch {
            refreshSessionLogs()
            val logged = sessionLogs
                .find { it.exerciseId == currentExercise().id && it.roundNumber == prevCursor.roundNumber }

            if (logged != null) {
                valueInput = (logged.reps ?: logged.durationSeconds)?.toString() ?: ""
                weightInput = logged.weightKg?.let { formatWeight(displayWeight(it)) } ?: ""
                refreshPrevious()
                openKeypad()
            } else {
                loadInputDefaults()
            }

            restEndEpochMillis = null
            restToken = 0L
            restCaption = ""
            restAlertFired = false
            stopRestTimerService()
            persistProgress()
            publishActive()
        }
    }

    fun finishWorkout() {
        if (!::session.isInitialized || session.completed) return
        viewModelScope.launch { finishAndSummarize() }
    }





    fun saveSummary() {
        val summary = _uiState.value as? WorkoutSessionUiState.Summary ?: return
        viewModelScope.launch {
            for (supp in summary.supplements) {
                supplementRepository.setSupplementTaken(session.date, supp.supplement, supp.taken)
            }
            _summaryDone.emit(Unit)
        }
    }

    private suspend fun applyAdvance(advance: SessionAdvance) {
        holdStartEpochMillis = null
        when (advance) {
            is SessionAdvance.Continue -> {
                progressionSuggestion = null
                cursor = advance.cursor
                restEndEpochMillis = null
                restToken = 0L
                persistProgress()
                loadInputDefaults()
                publishActive()
            }
            is SessionAdvance.Rest -> {
                progressionSuggestion = null
                cursor = advance.cursorAfterRest
                restEndEpochMillis = clock.millis() + advance.seconds * 1_000L
                restToken = clock.millis().let { minted -> if (minted == restToken) minted + 1 else minted }
                restCaption = restCaptionForCursor()
                restAlertFired = false
                persistProgress()
                startRestTimerService(restEndEpochMillis!!, restCaption)
                loadInputDefaults()
                publishActive()
            }
            SessionAdvance.Finished -> finishAndSummarize()
        }
    }

    private suspend fun finishAndSummarize() {
        restEndEpochMillis = null
        restToken = 0L
        restCaption = ""
        restAlertFired = false
        stopRestTimerService()
        session = workoutRepository.finishSession(sessionId, notesInput.takeIf { it.isNotBlank() })
        if (::plan.isInitialized) {
            workoutRepository.recordSessionRecords(sessionId, prEligibleExerciseIds(plan))
        }
        progressStore.clear()
        showSummary()
    }

    private suspend fun showSummary() {
        val allSupps = supplementRepository.observeAllSupplements().first()
        val logs = supplementRepository.observeIntakesForDate(session.date).first()
        val records = workoutRepository.recordsForSession(sessionId).mapNotNull { row ->
            formatRecordLine(row.record.type, row.record.value, row.exerciseName, unitSystem)
        }
        _uiState.value = WorkoutSessionUiState.Summary(
            workoutName = plan.name,
            setCount = workoutRepository.setCount(sessionId),
            durationSeconds = session.durationSeconds ?: elapsedSeconds(),
            notes = session.notes?.takeIf { it.isNotBlank() }.orEmpty(),
            supplements = allSupps.filter { it.isActive }.map { supp ->
                val log = logs.firstOrNull { it.supplementId == supp.id }
                TodaySupplement(
                    supplement = supp,
                    taken = log?.taken == true,
                    actualAmount = log?.actualAmount?.takeIf { it > 0 } ?: supp.doseAmount,
                )
            },
            records = records,
        )
    }

    private suspend fun loadInputDefaults() {
        val exercise = currentExercise()
        val previous = workoutRepository.previousSet(exercise.id, cursor.roundNumber, sessionId)
        progressionSuggestion = null
        if (plan.blocks[cursor.blockIndex].kind != BlockKind.WARM_UP) {
            progressionSuggestion = ProgressionCalculator.suggestionFor(
                exercise.targetValueHigh,
                exercise.tracksWeight,
                workoutRepository.previousSessionRounds(exercise.id, sessionId).map {
                    PriorRound(if (exercise.repType == RepType.REPS) it.reps else it.durationSeconds, it.weightKg)
                },
            )
        }
        valueInput = when (exercise.repType) {
            RepType.REPS -> previous?.reps
            RepType.SECONDS -> previous?.durationSeconds
        }?.toString() ?: ((exercise.targetValueLow + exercise.targetValueHigh) / 2).toString()
        val baseWeightKg = previous?.weightKg ?: 0f
        val suggestedWeightKg = if (progressionSuggestion == ProgressionSuggestion.INCREASE_WEIGHT) {
            baseWeightKg + if (unitSystem == UnitSystem.LB) poundsToKilograms(2.5f) else 1f
        } else baseWeightKg

        weightInput = if (previous?.weightKg != null || progressionSuggestion == ProgressionSuggestion.INCREASE_WEIGHT) {
            formatWeight(if (unitSystem == UnitSystem.LB) kilogramsToPounds(suggestedWeightKg) else suggestedWeightKg)
        } else {
            if (exercise.tracksWeight) "0" else ""
        }
        refreshSessionLogs()
        refreshPrevious()
        openKeypad()
    }

    private fun tick() {
        // Overtime stays on screen. Hitting zero does not advance the exercise.
        publishActive()
    }

    private fun publishActive() {
        if (!::plan.isInitialized || session.completed) return
        val block = plan.blocks[cursor.blockIndex]
        val exercise = currentExercise()
        val warmUp = block.kind == BlockKind.WARM_UP
        val seconds = exercise.repType == RepType.SECONDS
        val elapsed = elapsedSeconds()
        val holding = holdStartEpochMillis != null
        val logs = sessionLogs.filter { it.exerciseId == exercise.id }.map { log ->
            LoggedSetView(
                roundNumber = log.roundNumber,
                reps = log.reps,
                durationSeconds = log.durationSeconds,
                weightDisplay = log.weightKg?.let { formatWeight(displayWeight(it)) },
            )
        }
        val rest = restEndEpochMillis?.let { restSecondsUntil(it, clock.millis()) }
        _uiState.value = WorkoutSessionUiState.Active(
            elapsedSeconds = elapsed,
            maxDurationMinutes = plan.maxDurationMinutes,
            durationPassed = durationPassed(elapsed, plan.maxDurationMinutes),
            blockLabel = block.label,
            exercise = exercise,
            pills = exercisePills(plan.blocks, cursor),
            rows = buildSetRows(
                warmUp = warmUp,
                roundCount = block.targetRoundsMax,
                activeRound = cursor.roundNumber,
                tracksWeight = exercise.tracksWeight,
                seconds = seconds,
                activeLoad = weightInput,
                activeValue = valueInput,
                holdingSeconds = if (holding) ((clock.millis() - holdStartEpochMillis!!) / 1000).toInt() else null,
                logs = logs,
                previousText = previousByRound.mapValues { (_, prior) -> previousLabel(prior, seconds) },
                previousCopyable = previousByRound.mapValues { (_, prior) -> prior != null },
            ),
            keypadOpen = keypadOpen,
            keypadField = keypadField,
            restRemainingSeconds = rest?.remaining,
            restOvertimeSeconds = rest?.overtime?.takeIf { it > 0 },
            restCaption = restCaption,
            isOptionalBlock = block.isOptional,
            isSaving = isSaving,
            notesInput = notesInput,
            isHolding = holding,
            holdElapsedSeconds = holdStartEpochMillis?.let { ((clock.millis() - it) / 1000).toInt() } ?: 0,
        )
    }

    private fun restCaptionForCursor(): String {
        val exercise = plan.blocks[cursor.blockIndex].exercises[cursor.exerciseIndex]
        return nextExerciseCaption(exercise.name)
    }

    private fun previousLabel(prior: SetLogEntity?, seconds: Boolean): String {
        if (prior == null) return "—"
        val figure = if (seconds) prior.durationSeconds?.toString() else prior.reps?.toString()
        val load = prior.weightKg?.let { formatWeight(displayWeight(it)) }
        return formatPrevious(load, figure, seconds)
    }

    private fun displayWeight(kilograms: Float): Float =
        if (unitSystem == UnitSystem.LB) kilogramsToPounds(kilograms) else kilograms

    private fun openKeypad() {
        keypadOpen = true
        keypadField = KeypadField.VALUE
        keypadFresh = true
    }

    private suspend fun refreshSessionLogs() {
        sessionLogs = workoutRepository.getSessionSets(sessionId)
    }

    private suspend fun refreshPrevious() {
        val exercise = currentExercise()
        val block = plan.blocks[cursor.blockIndex]
        val rounds = if (block.kind == BlockKind.WARM_UP) 1 else block.targetRoundsMax.coerceAtLeast(1)
        val loaded = LinkedHashMap<Int, SetLogEntity?>()
        for (round in 1..rounds) {
            loaded[round] = workoutRepository.previousColumn(exercise.id, round, sessionId)
        }
        previousByRound = loaded
    }

    private fun startRestTimerService(endMillis: Long, upNext: String) {
        val intent = Intent(applicationContext, RestTimerService::class.java).apply {
            action = RestTimerService.ACTION_START
            putExtra(RestTimerService.EXTRA_END_MILLIS, endMillis)
            putExtra(RestTimerService.EXTRA_UP_NEXT, upNext)
            putExtra(RestTimerService.EXTRA_SESSION_ID, sessionId)
            if (restToken != 0L) putExtra(RestTimerService.EXTRA_REST_TOKEN, restToken)
        }
        ContextCompat.startForegroundService(applicationContext, intent)
    }

    private fun adjustRestTimerService(deltaMs: Long) {
        val intent = Intent(applicationContext, RestTimerService::class.java).apply {
            action = RestTimerService.ACTION_ADJUST
            putExtra(RestTimerService.EXTRA_DELTA_MS, deltaMs)
        }
        ContextCompat.startForegroundService(applicationContext, intent)
    }

    private fun stopRestIfStill(skippedEnd: Long, skippedToken: Long) {
        val intent = Intent(applicationContext, RestTimerService::class.java).apply {
            action = RestTimerService.ACTION_STOP
            putExtra(RestTimerService.EXTRA_END_MILLIS, skippedEnd)
            if (skippedToken != 0L) putExtra(RestTimerService.EXTRA_REST_TOKEN, skippedToken)
        }
        try {
            ContextCompat.startForegroundService(applicationContext, intent)
        } catch (_: Exception) {
            RestAlarmScheduler.cancel(applicationContext)
            RestAlertPlayback.stop()
            RestTimerService.releaseRetainedWakeLock()
        }
    }

    private fun stopRestTimerService() {
        RestAlarmScheduler.cancel(applicationContext)
        RestAlertPlayback.stop()
        RestTimerService.releaseRetainedWakeLock()
        try {
            applicationContext.stopService(Intent(applicationContext, RestTimerService::class.java))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun currentExercise() = plan.blocks[cursor.blockIndex].exercises[cursor.exerciseIndex]

    private fun elapsedSeconds(): Int {
        val start = session.startTime?.toEpochMilli() ?: clock.millis()
        val end = session.endTime?.toEpochMilli() ?: clock.millis()
        return ((end - start).coerceAtLeast(0) / 1_000L).toInt()
    }

    private suspend fun persistProgress() = progressStore.save(
        SessionProgress(
            sessionId,
            cursor.blockIndex,
            cursor.exerciseIndex,
            cursor.roundNumber,
            restEndEpochMillis,
            notes = notesInput,
            restCaption = restCaption,
            restAlertFired = restAlertFired,
            restToken = restToken,
        ),
    )

    companion object {
        fun factory(
            context: Context,
            sessionId: Long,
            workoutRepository: WorkoutRepository,
            progressStore: SessionProgressStore,
            supplementRepository: SupplementRepository,
            cycleSettingsRepository: CycleSettingsRepository,
            clock: Clock,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(WorkoutSessionViewModel::class.java))
                return WorkoutSessionViewModel(
                    context, sessionId, workoutRepository, progressStore, supplementRepository,
                    cycleSettingsRepository, clock,
                ) as T
            }
        }
    }
}
