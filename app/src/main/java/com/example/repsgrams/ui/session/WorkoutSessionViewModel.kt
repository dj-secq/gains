package com.example.repsgrams.ui.session

import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.repsgrams.service.RestAlarmScheduler
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
import com.example.repsgrams.data.db.WorkoutSessionEntity
import com.example.repsgrams.data.repository.SupplementRepository
import com.example.repsgrams.data.repository.WorkoutRepository
import com.example.repsgrams.domain.session.SessionAdvance
import com.example.repsgrams.domain.session.SessionCursor
import com.example.repsgrams.domain.session.SessionNavigator
import com.example.repsgrams.domain.session.WorkoutExercise
import com.example.repsgrams.domain.session.WorkoutPlan
import java.time.Clock
import com.example.repsgrams.domain.progression.PriorRound
import com.example.repsgrams.domain.progression.ProgressionCalculator
import com.example.repsgrams.domain.progression.ProgressionSuggestion
import com.example.repsgrams.domain.progress.kilogramsToPounds
import com.example.repsgrams.domain.progress.poundsToKilograms
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
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
        val workoutName: String,
        val dayLabel: String,
        val category: String,
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
        val restOvertimeSeconds: Int?,
        val isOptionalBlock: Boolean,
        val isSaving: Boolean,
        val progressionSuggestion: ProgressionSuggestion?,
        val lastTimeRound: PriorRound?,
        val upNextExercises: List<String>,
        val rpeTagInput: String?,
        val notesInput: String,
        val isHolding: Boolean,
        val holdElapsedSeconds: Int,
    ) : WorkoutSessionUiState

    data class Summary(
        val workoutName: String,
        val category: String,
        val setCount: Int,
        val durationSeconds: Int,
        val maxDurationMinutes: Int,
        val supplements: List<com.example.repsgrams.ui.today.TodaySupplement>,
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

    private val _prAchieved = MutableSharedFlow<List<com.example.repsgrams.data.db.PersonalRecordEntity>>(extraBufferCapacity = 1)
    val prAchieved: SharedFlow<List<com.example.repsgrams.data.db.PersonalRecordEntity>> = _prAchieved.asSharedFlow()

    private lateinit var session: WorkoutSessionEntity
    private lateinit var plan: WorkoutPlan
    private var cursor = SessionCursor(0, 0, 1)
    private var restEndEpochMillis: Long? = null
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
    private var lastTimeRound: PriorRound? = null
    private var holdStartEpochMillis: Long? = null

    init {
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
                    restCaption != saved.restCaption
                restEndEpochMillis = saved.restEndEpochMillis
                restAlertFired = saved.restAlertFired
                restCaption = saved.restCaption
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
        unitSystem = cycleSettingsRepository.settings.first().unitSystem
        if (session.completed) {
            showSummary()
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
        holdStartEpochMillis = clock.millis()
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
    }
    fun updateRpeTag(tag: String?) {
        rpeTagInput = tag
        publishActive()
    }
    fun adjustWeight(delta: Float) {
        weightInput = ((weightInput.toFloatOrNull() ?: 0f) + delta).coerceAtLeast(0f).let(::formatWeight)
        publishActive()
    }

    fun logCurrent() {
        if (isSaving || restEndEpochMillis != null) return
        val value = valueInput.toIntOrNull() ?: return
        val exercise = currentExercise()
        isSaving = true
        publishActive()
        viewModelScope.launch {
            runCatching {
                workoutRepository.logSet(
                    sessionId = sessionId,
                    exerciseId = exercise.id,
                    roundNumber = cursor.roundNumber,
                    reps = value.takeIf { exercise.repType == RepType.REPS },
                    durationSeconds = value.takeIf { exercise.repType == RepType.SECONDS },
                    weightKg = weightInput.toFloatOrNull()
                        ?.let { if (unitSystem == UnitSystem.LB) poundsToKilograms(it) else it }
                        .takeIf { exercise.tracksWeight },
                )
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
        if (restEndEpochMillis == null) return
        restEndEpochMillis = null
        restCaption = ""
        restAlertFired = false
        publishActive()
        viewModelScope.launch {
            persistProgress()
            stopRestTimerService()
        }
    }

    fun cancelWorkout() {
        viewModelScope.launch {
            workoutRepository.deleteSession(sessionId)
            progressStore.clear()
            stopRestTimerService()
        }
    }

    fun previousStep() {
        if (cursor.blockIndex == 0 && cursor.exerciseIndex == 0 && cursor.roundNumber == 1) return

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
            val logged = workoutRepository.getSessionSets(sessionId)
                .find { it.exerciseId == currentExercise().id && it.roundNumber == prevCursor.roundNumber }

            if (logged != null) {
                valueInput = (logged.reps ?: logged.durationSeconds)?.toString() ?: ""
                weightInput = logged.weightKg?.let { formatWeight(if (unitSystem == UnitSystem.LB) kilogramsToPounds(it) else it) } ?: ""
            } else {
                loadInputDefaults()
            }

            restEndEpochMillis = null
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
        when (advance) {
            is SessionAdvance.Continue -> {
                progressionSuggestion = null
                cursor = advance.cursor
                restEndEpochMillis = null
                persistProgress()
                loadInputDefaults()
                publishActive()
            }
            is SessionAdvance.Rest -> {
                progressionSuggestion = null
                cursor = advance.cursorAfterRest
                restEndEpochMillis = clock.millis() + advance.seconds * 1_000L
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
        restCaption = ""
        restAlertFired = false
        stopRestTimerService()
        session = workoutRepository.finishSession(sessionId, notesInput.takeIf { it.isNotBlank() })
        progressStore.clear()
        showSummary()
    }

    private suspend fun showSummary() {
        val allSupps = supplementRepository.observeAllSupplements().first()
        val logs = supplementRepository.observeIntakesForDate(session.date).first()
        _uiState.value = WorkoutSessionUiState.Summary(
            workoutName = plan.name,
            category = plan.category,
            setCount = workoutRepository.setCount(sessionId),
            durationSeconds = session.durationSeconds ?: elapsedSeconds(),
            maxDurationMinutes = plan.maxDurationMinutes,
            supplements = allSupps.filter { it.isActive }.map { supp ->
                val log = logs.firstOrNull { it.supplementId == supp.id }
                com.example.repsgrams.ui.today.TodaySupplement(
                    supplement = supp,
                    taken = log?.taken == true,
                    actualAmount = log?.actualAmount?.takeIf { it > 0 } ?: supp.doseAmount,
                )
            },
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
    }

    private fun tick() {
        // Overtime stays on screen. Hitting zero does not advance the exercise.
        publishActive()
    }

    private fun publishActive() {
        if (!::plan.isInitialized || session.completed) return
        val block = plan.blocks[cursor.blockIndex]

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

        val rest = restEndEpochMillis?.let { restSecondsUntil(it, clock.millis()) }
        _uiState.value = WorkoutSessionUiState.Active(
            workoutName = plan.name,
            dayLabel = plan.dayLabel,
            category = plan.category,
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
            restRemainingSeconds = rest?.remaining,
            restOvertimeSeconds = rest?.overtime?.takeIf { it > 0 },
            isOptionalBlock = block.isOptional,
            isSaving = isSaving,
            progressionSuggestion = progressionSuggestion,
            lastTimeRound = lastTimeRound,
            upNextExercises = upNext,
            rpeTagInput = rpeTagInput,
            notesInput = notesInput,
            isHolding = holdStartEpochMillis != null,
            holdElapsedSeconds = holdStartEpochMillis?.let { ((clock.millis() - it) / 1000).toInt() } ?: 0,
        )
    }

    private fun restCaptionForCursor(): String {
        val exercise = plan.blocks[cursor.blockIndex].exercises[cursor.exerciseIndex]
        return "Up next: Round ${cursor.roundNumber} - ${exercise.name}"
    }

    private fun startRestTimerService(endMillis: Long, upNext: String) {
        val intent = Intent(applicationContext, RestTimerService::class.java).apply {
            action = RestTimerService.ACTION_START
            putExtra(RestTimerService.EXTRA_END_MILLIS, endMillis)
            putExtra(RestTimerService.EXTRA_UP_NEXT, upNext)
            putExtra(RestTimerService.EXTRA_SESSION_ID, sessionId)
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

    private fun stopRestTimerService() {
        RestAlarmScheduler.cancel(applicationContext)
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

private fun formatWeight(value: Float): String =
    if (value % 1f == 0f) value.toInt().toString() else "%.2f".format(value).trimEnd('0').trimEnd('.')
