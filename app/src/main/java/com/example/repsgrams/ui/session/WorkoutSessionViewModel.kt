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
import com.example.repsgrams.data.db.Equipment
import com.example.repsgrams.data.db.ExerciseEntity
import com.example.repsgrams.data.db.RepType
import com.example.repsgrams.data.db.SessionKind
import com.example.repsgrams.data.db.SetLogEntity
import com.example.repsgrams.data.db.SetType
import com.example.repsgrams.data.db.WorkoutSessionEntity
import com.example.repsgrams.data.repository.SupplementRepository
import com.example.repsgrams.data.repository.WorkoutRepository
import com.example.repsgrams.domain.session.KeypadField
import com.example.repsgrams.domain.session.LoggedSetView
import com.example.repsgrams.domain.session.RecordLine
import com.example.repsgrams.domain.session.SessionAdvance
import com.example.repsgrams.domain.session.SessionCursor
import com.example.repsgrams.domain.session.SessionNavigator
import com.example.repsgrams.domain.session.SetRowModel
import com.example.repsgrams.domain.session.WorkoutBlock
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
import com.example.repsgrams.domain.session.nextExerciseCaption
import com.example.repsgrams.domain.session.prEligibleExerciseIds
import com.example.repsgrams.ui.today.TodaySupplement
import java.time.Clock
import com.example.repsgrams.domain.plates.calculatePlates
import com.example.repsgrams.domain.plates.plateCaption
import com.example.repsgrams.domain.progression.PriorRound
import com.example.repsgrams.domain.progression.ProgressionCalculator
import com.example.repsgrams.domain.progression.ProgressionMaps
import com.example.repsgrams.domain.progression.ProgressionResult
import com.example.repsgrams.domain.progression.formatProgressionMap
import com.example.repsgrams.domain.progression.parseProgressionMap
import com.example.repsgrams.domain.progression.progressionSentence
import com.example.repsgrams.domain.progress.kilogramsToPounds
import com.example.repsgrams.domain.progress.poundsToKilograms
import com.example.repsgrams.data.datastore.parsePlateList
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
        val hapticsEnabled: Boolean = true,
        val rpeEnabled: Boolean = false,
        val rpe: Float? = null,
        val showEndBlock: Boolean = false,
        val showAddRound: Boolean = false,
        val insteadOf: String? = null,
        val progressionLine: String? = null,
        val progressionSkipped: Boolean = false,
        val endLines: List<String> = emptyList(),
        val plate: PlateView? = null,
        val catalog: List<CatalogExercise> = emptyList(),
        val showAddWarmup: Boolean = false,
        val showAddExercise: Boolean = false,
    ) : WorkoutSessionUiState

    /** Empty workout before the first exercise. The logging surface is not built yet. */
    data class FreestyleEmpty(
        val elapsedSeconds: Int,
        val notesInput: String,
        val catalog: List<CatalogExercise>,
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

data class CatalogExercise(val id: Long, val name: String)

data class PlateView(
    val caption: String,
    val struck: String?,
    val makeable: String?,
    val difference: String?,
    val assumption: String?,
    val dumbbell: Boolean,
    val underBar: Boolean,
)

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
    private var progressionEnabled = true
    private var incrementKg = 2.5f
    private var incrementLb = 5f
    private var rpeEnabled = false
    private var hapticsEnabled = true
    private var barbellKg = 20f
    private var barbellLb = 45f
    private var platesKg = com.example.repsgrams.data.datastore.DEFAULT_PLATES_KG
    private var platesLb = com.example.repsgrams.data.datastore.DEFAULT_PLATES_LB
    private var progressionMaps = ProgressionMaps()
    private var rpeInput: Float? = null
    private var extraRounds = 0
    private var substitutes: Map<Long, Long> = emptyMap()
    private val consumedExercises = mutableSetOf<Long>()
    private val appliedSessionByExercise = mutableMapOf<Long, Long>()
    private var catalog: List<CatalogExercise> = emptyList()
    private var notesInput: String = ""
    private var holdStartEpochMillis: Long? = null
    private var keypadOpen = true
    private var keypadField = KeypadField.VALUE
    private var keypadFresh = true
    private var sessionLogs: List<SetLogEntity> = emptyList()
    private var previousByRound: Map<Int, SetLogEntity?> = emptyMap()
    private var warmupExerciseIds: Set<Long> = emptySet()
    private var freestyleOrder: List<Long> = emptyList()

    init {
        viewModelScope.launch {
            cycleSettingsRepository.settings.collect { settings ->
                _keepScreenOn.value = settings.keepScreenOn
                applySettings(settings)
                val shown = _uiState.value
                if (shown is WorkoutSessionUiState.Active || shown is WorkoutSessionUiState.FreestyleEmpty) {
                    publishActive()
                }
            }
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
                val shown = _uiState.value
                if (changed && (shown is WorkoutSessionUiState.Active || shown is WorkoutSessionUiState.FreestyleEmpty)) {
                    publishActive()
                }
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
        if (session.sessionKind == SessionKind.FREESTYLE) {
            loadFreestyle()
            return
        }
        val templateId = requireNotNull(session.templateId) { "This workout template was deleted" }
        plan = workoutRepository.loadPlan(templateId)
        val settings = cycleSettingsRepository.settings.first()
        applySettings(settings)
        progressionMaps = ProgressionMaps(settings.progressionQualified, settings.progressionSkipped)
        catalog = workoutRepository.listExercises().map { CatalogExercise(it.id, it.name) }
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
        if (saved != null) {
            substitutes = parseSubstitutes(saved.substitutes)
            appliedSessionByExercise.putAll(parseProgressionMap(saved.appliedProgression))
            consumedExercises.addAll(appliedSessionByExercise.keys)
            warmupExerciseIds = parseIdList(saved.warmupExercises).toSet()
            if (saved.notes.isNotEmpty()) notesInput = saved.notes
            val cursorFits = saved.blockIndex in plan.blocks.indices &&
                saved.exerciseIndex in plan.blocks[saved.blockIndex].exercises.indices
            if (cursorFits) {
                cursor = SessionCursor(saved.blockIndex, saved.exerciseIndex, saved.roundNumber)
                restEndEpochMillis = saved.restEndEpochMillis
                restCaption = saved.restCaption
                restAlertFired = saved.restAlertFired
                restToken = saved.restToken
                extraRounds = saved.extraRounds.coerceIn(0, MAX_EXTRA_ROUNDS)
            } else {
                extraRounds = 0
                persistProgress()
            }
        } else {
            persistProgress()
        }
        applySubstitutes()
        // A past deadline stays on screen as overtime. Do not clear it here.
        loadInputDefaults()
        if (restEndEpochMillis != null) {
            if (restCaption.isBlank()) {
                restCaption = restCaptionForCursor()
                persistProgress()
            }
            startRestTimerService(restEndEpochMillis!!, restCaption)
        }
        ready = true
        publishActive()
    }

    private suspend fun loadFreestyle() {
        val settings = cycleSettingsRepository.settings.first()
        applySettings(settings)
        progressionMaps = ProgressionMaps(settings.progressionQualified, settings.progressionSkipped)
        catalog = workoutRepository.listExercises().map { CatalogExercise(it.id, it.name) }
        _keepScreenOn.value = settings.keepScreenOn
        val saved = progressStore.progress.first()?.takeIf { it.sessionId == sessionId }
        sessionLogs = workoutRepository.getSessionSets(sessionId)
        val storedIds = parseIdList(saved?.freestyleExercises.orEmpty())
        freestyleOrder = if (storedIds.isNotEmpty()) storedIds else freestyleIdsFromLogs(sessionLogs)
        warmupExerciseIds = parseIdList(saved?.warmupExercises.orEmpty()).toSet()
        if (saved != null) {
            substitutes = parseSubstitutes(saved.substitutes)
            appliedSessionByExercise.putAll(parseProgressionMap(saved.appliedProgression))
            consumedExercises.addAll(appliedSessionByExercise.keys)
            if (saved.notes.isNotEmpty()) notesInput = saved.notes
        }
        plan = freestylePlan(freestyleOrder)
        if (session.completed) {
            showSummary()
            return
        }
        if (freestyleOrder.isEmpty() || plan.blocks.isEmpty() || plan.blocks.all { it.exercises.isEmpty() }) {
            ready = true
            persistProgress()
            publishActive()
            return
        }
        if (saved != null) {
            val cursorFits = saved.blockIndex in plan.blocks.indices &&
                saved.exerciseIndex in plan.blocks[saved.blockIndex].exercises.indices
            if (cursorFits) {
                cursor = SessionCursor(saved.blockIndex, saved.exerciseIndex, saved.roundNumber)
                restEndEpochMillis = saved.restEndEpochMillis
                restCaption = saved.restCaption
                restAlertFired = saved.restAlertFired
                restToken = saved.restToken
                extraRounds = saved.extraRounds.coerceIn(0, MAX_EXTRA_ROUNDS)
            } else {
                cursor = SessionCursor(0, 0, 1)
                extraRounds = 0
            }
        }
        loadInputDefaults()
        if (restEndEpochMillis != null) {
            if (restCaption.isBlank()) restCaption = restCaptionForCursor()
            startRestTimerService(restEndEpochMillis!!, restCaption)
        }
        ready = true
        persistProgress()
        publishActive()
    }

    fun addWarmupRow() {
        if (!ready || !::plan.isInitialized || plan.blocks.isEmpty()) return
        val block = plan.blocks.getOrNull(cursor.blockIndex) ?: return
        if (block.kind == BlockKind.WARM_UP) return
        val id = currentExercise().id
        if (id in warmupExerciseIds) return
        warmupExerciseIds = warmupExerciseIds + id
        viewModelScope.launch {
            persistProgress()
            publishActive()
        }
    }

    fun logWarmup() {
        if (isSaving || restEndEpochMillis != null || !ready || !::plan.isInitialized) return
        if (plan.blocks.isEmpty()) return
        val exercise = currentExercise()
        if (exercise.id !in warmupExerciseIds) return
        if (sessionLogs.any { it.exerciseId == exercise.id && it.setType == SetType.WARMUP }) return
        val value = valueInput.toIntOrNull() ?: return
        isSaving = true
        publishActive()
        val weightKg = weightInput.toFloatOrNull()
            ?.let { if (unitSystem == UnitSystem.LB) poundsToKilograms(it) else it }
            .takeIf { exercise.tracksWeight }
        val rpe = rpeInput.takeIf { rpeEnabled }
        viewModelScope.launch {
            runCatching {
                workoutRepository.logSet(
                    sessionId = sessionId,
                    exerciseId = exercise.id,
                    roundNumber = 1,
                    reps = value.takeIf { exercise.repType == RepType.REPS },
                    durationSeconds = value.takeIf { exercise.repType == RepType.SECONDS },
                    weightKg = weightKg,
                    rpeTag = rpe?.let { formatWeight(it) },
                    setType = SetType.WARMUP,
                    rpe = rpe,
                )
                refreshSessionLogs()
            }.onFailure {
                _uiState.value = WorkoutSessionUiState.Error(it.message ?: "Couldn't save this set.")
            }
            isSaving = false
            if (_uiState.value is WorkoutSessionUiState.Active) publishActive()
        }
    }

    fun addFreestyleExercise(exerciseId: Long) {
        if (!::session.isInitialized || session.sessionKind != SessionKind.FREESTYLE) return
        if (exerciseId in freestyleOrder) return
        freestyleOrder = freestyleOrder + exerciseId
        viewModelScope.launch {
            plan = freestylePlan(freestyleOrder)
            val exercises = plan.blocks.firstOrNull()?.exercises.orEmpty()
            if (exercises.none { it.id == exerciseId }) {
                freestyleOrder = freestyleOrder.filter { it != exerciseId }
                ready = true
                publishActive()
                return@launch
            }
            val index = exercises.indexOfFirst { it.id == exerciseId }
            cursor = SessionCursor(0, index.coerceAtLeast(0), 1)
            extraRounds = 0
            restEndEpochMillis = null
            loadInputDefaults()
            ready = true
            persistProgress()
            publishActive()
        }
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
    fun adjustRpe(delta: Float) {
        if (!rpeEnabled) return
        val current = rpeInput
        rpeInput = if (current == null) {
            if (delta > 0f) 6f else null
        } else {
            val next = current + delta
            if (next < 6f) null else next.coerceAtMost(10f)
        }
        publishActive()
    }

    fun skipProgressionOnce() {
        if (!ready) return
        val exercise = currentExercise()
        progressionMaps = ProgressionCalculator.recordOnFinish(
            progressionMaps,
            exercise.id,
            sessionId,
            qualified = false,
            skipped = true,
        )
        viewModelScope.launch {
            cycleSettingsRepository.setProgressionQualified(progressionMaps.qualified)
            cycleSettingsRepository.setProgressionSkipped(progressionMaps.skipped)
            publishActive()
        }
    }

    fun endBlock() {
        val block = plan.blocks.getOrNull(cursor.blockIndex) ?: return
        if (block.kind == BlockKind.WARM_UP) return
        if (cursor.roundNumber <= block.targetRoundsMin) return
        viewModelScope.launch { applyAdvance(SessionNavigator.finishBlock(plan, cursor.blockIndex)) }
    }

    fun addRound() {
        val block = plan.blocks.getOrNull(cursor.blockIndex) ?: return
        if (block.kind == BlockKind.WARM_UP || restEndEpochMillis != null) return
        if (extraRounds >= MAX_EXTRA_ROUNDS) return
        extraRounds += 1
        viewModelScope.launch {
            refreshPrevious()
            persistProgress()
            publishActive()
        }
    }

    fun substituteThisSession(exerciseId: Long) {
        if (!ready) return
        val exercise = currentExercise()
        if (exercise.linkId == 0L) return
        substitutes = substitutes + (exercise.linkId to exerciseId)
        viewModelScope.launch {
            applySubstitutes()
            loadInputDefaults()
            persistProgress()
            publishActive()
        }
    }

    fun replaceInProgram(exerciseId: Long) {
        if (!ready) return
        val block = plan.blocks[cursor.blockIndex]
        val exercise = currentExercise()
        if (exercise.linkId == 0L) return
        viewModelScope.launch {
            workoutRepository.replaceLinkExercise(block.id, exercise.linkId, exerciseId)
            val chosen = workoutRepository.getExercise(exerciseId) ?: return@launch
            substitutes = substitutes - exercise.linkId
            replacePlanned(block.id, exercise.linkId, chosen)
            loadInputDefaults()
            persistProgress()
            publishActive()
        }
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
            applyLoadKey(weightInput, key, keypadFresh, loadIncrementDisplay())
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
        if (isSaving || restEndEpochMillis != null || !::plan.isInitialized || plan.blocks.isEmpty()) return
        val value = valueInput.toIntOrNull() ?: return
        val exercise = currentExercise()
        isSaving = true
        publishActive()
        val block = plan.blocks[cursor.blockIndex]
        val weightKg = weightInput.toFloatOrNull()
            ?.let { if (unitSystem == UnitSystem.LB) poundsToKilograms(it) else it }
            .takeIf { exercise.tracksWeight }
        val rpe = rpeInput.takeIf { rpeEnabled }
        val substitutedFrom = exercise.plannedExerciseId.takeIf { it != 0L && it != exercise.id }
        viewModelScope.launch {
            runCatching {
                workoutRepository.logSet(
                    sessionId = sessionId,
                    exerciseId = exercise.id,
                    roundNumber = cursor.roundNumber,
                    reps = value.takeIf { exercise.repType == RepType.REPS },
                    durationSeconds = value.takeIf { exercise.repType == RepType.SECONDS },
                    weightKg = weightKg,
                    rpeTag = rpe?.let { formatWeight(it) },
                    substitutedFrom = substitutedFrom,
                    setType = if (block.kind == BlockKind.WARM_UP) SetType.WARMUP else SetType.WORKING,
                    rpe = rpe,
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
                val cap = effectiveMax(block)
                applyAdvance(SessionNavigator.afterExercise(plan, cursor, cap))
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
        if (!::plan.isInitialized || plan.blocks.isEmpty()) return
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

        if (prevCursor.blockIndex != cursor.blockIndex) extraRounds = 0
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
        rpeInput = null
        when (advance) {
            is SessionAdvance.Continue -> {
                moveCursor(advance.cursor)
                restEndEpochMillis = null
                restToken = 0L
                loadInputDefaults()
                persistProgress()
                publishActive()
            }
            is SessionAdvance.Rest -> {
                moveCursor(advance.cursorAfterRest)
                restEndEpochMillis = clock.millis() + advance.seconds * 1_000L
                restToken = clock.millis().let { minted -> if (minted == restToken) minted + 1 else minted }
                restAlertFired = false
                loadInputDefaults()
                restCaption = restCaptionForCursor()
                persistProgress()
                startRestTimerService(restEndEpochMillis!!, restCaption)
                publishActive()
            }
            SessionAdvance.Finished -> {
                if (isFreestyle()) {
                    refreshSessionLogs()
                    persistProgress()
                    publishActive()
                } else {
                    finishAndSummarize()
                }
            }
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
            recordProgression()
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
        val workoutName = if (::plan.isInitialized && plan.name.isNotBlank()) plan.name else "Empty workout"
        _uiState.value = WorkoutSessionUiState.Summary(
            workoutName = workoutName,
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
        val block = plan.blocks[cursor.blockIndex]
        val warmUp = block.kind == BlockKind.WARM_UP
        if (!warmUp && exercise.id !in consumedExercises) {
            val consume = ProgressionCalculator.consumeOnLoad(progressionMaps, exercise.id, sessionId)
            progressionMaps = consume.maps
            consumedExercises += exercise.id
            if (consume.applySessionId != null) appliedSessionByExercise[exercise.id] = consume.applySessionId
            cycleSettingsRepository.setProgressionQualified(progressionMaps.qualified)
            cycleSettingsRepository.setProgressionSkipped(progressionMaps.skipped)
        }
        val previous = workoutRepository.previousSet(exercise.id, cursor.roundNumber, sessionId)
        val applySession = if (warmUp) null else appliedSessionByExercise[exercise.id]
        val qualifying = if (applySession == null) emptyList() else qualifyingLogs(applySession, exercise.id)
        val result = if (applySession == null) {
            null
        } else {
            ProgressionCalculator.decide(
                enabled = progressionEnabled,
                workingSets = qualifying.map { priorRound(exercise, it) },
                targetHigh = exercise.targetValueHigh,
                targetRoundsMin = block.targetRoundsMin,
                repType = exercise.repType,
                tracksWeight = exercise.tracksWeight,
            )
        }
        val midpoint = ((exercise.targetValueLow + exercise.targetValueHigh) / 2).toString()
        val roundLog = qualifying.firstOrNull { it.roundNumber == cursor.roundNumber }
        val lastWeighted = qualifying.lastOrNull { it.weightKg != null }
        when (result) {
            ProgressionResult.INCREASE_LOAD -> {
                val base = roundLog?.weightKg ?: lastWeighted?.weightKg ?: previous?.weightKg ?: 0f
                val next = base + loadIncrementKg(exercise)
                weightInput = formatWeight(displayWeight(next))
                valueInput = if (exercise.repType == RepType.REPS) {
                    exercise.targetValueLow.toString()
                } else {
                    (roundLog?.durationSeconds ?: previous?.durationSeconds)?.toString() ?: midpoint
                }
            }
            ProgressionResult.ADVANCE_TARGET -> {
                val base = roundLog?.weightKg ?: lastWeighted?.weightKg ?: previous?.weightKg
                weightInput = if (exercise.tracksWeight && base != null) formatWeight(displayWeight(base)) else {
                    if (exercise.tracksWeight) "0" else ""
                }
                valueInput = when (exercise.repType) {
                    RepType.REPS -> exercise.targetValueLow.toString()
                    RepType.SECONDS -> (roundLog?.durationSeconds ?: previous?.durationSeconds)?.toString() ?: midpoint
                }
            }
            else -> {
                valueInput = when (exercise.repType) {
                    RepType.REPS -> previous?.reps
                    RepType.SECONDS -> previous?.durationSeconds
                }?.toString() ?: midpoint
                weightInput = if (previous?.weightKg != null) {
                    formatWeight(displayWeight(previous.weightKg))
                } else if (exercise.tracksWeight) {
                    "0"
                } else {
                    ""
                }
            }
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
        if (plan.blocks.isEmpty() || plan.blocks.all { it.exercises.isEmpty() }) {
            _uiState.value = WorkoutSessionUiState.FreestyleEmpty(
                elapsedSeconds = elapsedSeconds(),
                notesInput = notesInput,
                catalog = catalog,
            )
            return
        }
        val block = plan.blocks[cursor.blockIndex]
        val exercise = currentExercise()
        val warmUp = block.kind == BlockKind.WARM_UP
        val seconds = exercise.repType == RepType.SECONDS
        val elapsed = elapsedSeconds()
        val holding = holdStartEpochMillis != null
        val logs = sessionLogs.filter { log ->
            log.exerciseId == exercise.id && (warmUp || log.setType != SetType.WARMUP)
        }
            .map { log ->
                LoggedSetView(
                    roundNumber = log.roundNumber,
                    reps = log.reps,
                    durationSeconds = log.durationSeconds,
                    weightDisplay = log.weightKg?.let { formatWeight(displayWeight(it)) },
                    rpe = log.rpe,
                )
            }
        val warmupLogged = sessionLogs.find { log ->
            log.exerciseId == exercise.id && log.setType == SetType.WARMUP
        }?.let { log ->
            LoggedSetView(
                roundNumber = log.roundNumber,
                reps = log.reps,
                durationSeconds = log.durationSeconds,
                weightDisplay = log.weightKg?.let { formatWeight(displayWeight(it)) },
                rpe = log.rpe,
            )
        }
        val workingRows = buildSetRows(
            warmUp = warmUp,
            roundCount = effectiveMax(block),
            activeRound = cursor.roundNumber,
            tracksWeight = exercise.tracksWeight,
            seconds = seconds,
            activeLoad = weightInput,
            activeValue = valueInput,
            holdingSeconds = if (holding) ((clock.millis() - holdStartEpochMillis!!) / 1000).toInt() else null,
            logs = logs,
            previousText = previousByRound.mapValues { (_, prior) -> previousLabel(prior, seconds) },
            previousCopyable = previousByRound.mapValues { (_, prior) -> prior != null },
            activeRpe = rpeInput?.let { formatWeight(it) },
            loggedRpe = logs.mapNotNull { view -> view.rpe?.let { view.roundNumber to formatWeight(it) } }.toMap(),
        )
        val rows = if (!warmUp && exercise.id in warmupExerciseIds) {
            listOf(warmupRow(warmupLogged, exercise.tracksWeight, seconds)) + workingRows
        } else {
            workingRows
        }
        val rest = restEndEpochMillis?.let { restSecondsUntil(it, clock.millis()) }
        val plate = plateView(exercise)
        val line = if (warmUp) null else progressionLine(exercise, block, sessionLogs)
        _uiState.value = WorkoutSessionUiState.Active(
            elapsedSeconds = elapsed,
            maxDurationMinutes = plan.maxDurationMinutes,
            durationPassed = durationPassed(elapsed, plan.maxDurationMinutes),
            blockLabel = block.label,
            exercise = exercise,
            pills = exercisePills(plan.blocks, cursor, extraRounds),
            rows = rows,
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
            hapticsEnabled = hapticsEnabled,
            rpeEnabled = rpeEnabled,
            rpe = rpeInput,
            showEndBlock = !warmUp && cursor.roundNumber > block.targetRoundsMin &&
                cursor.roundNumber <= effectiveMax(block),
            showAddRound = !warmUp && restEndEpochMillis == null && extraRounds < MAX_EXTRA_ROUNDS,
            insteadOf = exercise.plannedExerciseId.takeIf { it != 0L && it != exercise.id }?.let {
                "INSTEAD OF ${exercise.plannedName.ifBlank { "planned" }}"
            },
            progressionLine = line,
            progressionSkipped = progressionMaps.skipped[exercise.id] == sessionId,
            endLines = endLines(),
            plate = plate,
            catalog = catalog,
            showAddWarmup = !warmUp && exercise.id !in warmupExerciseIds,
            showAddExercise = isFreestyle(),
        )
    }

    private fun restCaptionForCursor(): String {
        val exercise = plan.blocks[cursor.blockIndex].exercises[cursor.exerciseIndex]
        val base = nextExerciseCaption(exercise.name)
        val plates = plateView(exercise)?.caption?.takeIf { it.isNotBlank() }
        return if (plates == null) base else "$base · $plates"
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
        val rounds = effectiveMax(block)
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

    private fun isFreestyle(): Boolean =
        ::session.isInitialized && session.sessionKind == SessionKind.FREESTYLE

    private suspend fun freestylePlan(ids: List<Long>): WorkoutPlan {
        val exercises = ids.mapNotNull { id ->
            val entity = workoutRepository.getExercise(id) ?: return@mapNotNull null
            WorkoutExercise(
                id = entity.id,
                name = entity.name,
                imageAssetName = entity.imageAssetName,
                notes = entity.notes,
                tracksWeight = entity.tracksWeight,
                targetValueLow = 8,
                targetValueHigh = 12,
                repType = RepType.REPS,
                perSide = false,
                equipment = entity.equipment,
            )
        }
        val block = if (exercises.isEmpty()) {
            emptyList()
        } else {
            listOf(
                WorkoutBlock(
                    id = 0,
                    label = "Empty workout",
                    kind = BlockKind.STANDARD,
                    targetRoundsMin = 1,
                    targetRoundsMax = 3,
                    restSecondsBetweenRounds = null,
                    restSecondsAfterBlock = null,
                    isOptional = false,
                    exercises = exercises,
                ),
            )
        }
        return WorkoutPlan(
            templateId = 0,
            name = "Empty workout",
            dayLabel = "",
            maxDurationMinutes = 0,
            category = "Custom",
            blocks = block,
        )
    }

    private fun warmupRow(logged: LoggedSetView?, tracksWeight: Boolean, seconds: Boolean): SetRowModel {
        val figure = if (seconds) logged?.durationSeconds else logged?.reps
        val load = logged?.weightDisplay?.takeIf { it.isNotBlank() }
        return SetRowModel(
            label = "W",
            roundNumber = logged?.roundNumber ?: 1,
            previousText = "—",
            loadText = if (tracksWeight) load ?: "—" else "—",
            repsText = figure?.toString() ?: "—",
            complete = logged != null,
            active = false,
            copyable = false,
            rpeText = logged?.rpe?.let { formatWeight(it) },
            warmup = true,
        )
    }

    private fun moveCursor(next: SessionCursor) {
        if (next.blockIndex != cursor.blockIndex) extraRounds = 0
        cursor = next
    }

    private fun effectiveMax(block: com.example.repsgrams.domain.session.WorkoutBlock): Int =
        if (block.kind == BlockKind.WARM_UP) 1 else block.targetRoundsMax.coerceAtLeast(1) + extraRounds

    private fun applySettings(settings: com.example.repsgrams.data.datastore.CycleSettings) {
        unitSystem = settings.unitSystem
        progressionEnabled = settings.progressionEnabled
        incrementKg = settings.progressionIncrementKg
        incrementLb = settings.progressionIncrementLb
        rpeEnabled = settings.rpeEnabled
        hapticsEnabled = settings.hapticsEnabled
        barbellKg = settings.barbellKg
        barbellLb = settings.barbellLb
        platesKg = settings.platesKg
        platesLb = settings.platesLb
    }

    private fun loadIncrementDisplay(): Float =
        if (unitSystem == UnitSystem.LB) incrementLb else incrementKg

    private fun loadIncrementKg(exercise: WorkoutExercise): Float {
        val override = exercise.progressionIncrementKg
        if (override != null && override.isFinite() && override > 0f) return override
        return if (unitSystem == UnitSystem.LB) poundsToKilograms(incrementLb) else incrementKg
    }

    private suspend fun applySubstitutes() {
        if (substitutes.isEmpty() || !::plan.isInitialized) return
        val byId = workoutRepository.listExercises().associateBy { it.id }
        plan = plan.copy(blocks = plan.blocks.map { block ->
            block.copy(exercises = block.exercises.map { exercise ->
                val chosenId = substitutes[exercise.linkId] ?: return@map exercise
                val chosen = byId[chosenId] ?: return@map exercise
                exercise.copy(
                    id = chosen.id,
                    name = chosen.name,
                    imageAssetName = chosen.imageAssetName,
                    notes = chosen.notes,
                    tracksWeight = chosen.tracksWeight,
                    equipment = chosen.equipment,
                )
            })
        })
    }

    private fun replacePlanned(blockId: Long, linkId: Long, chosen: ExerciseEntity) {
        plan = plan.copy(blocks = plan.blocks.map { block ->
            if (block.id != blockId) block else block.copy(exercises = block.exercises.map { exercise ->
                if (exercise.linkId != linkId) exercise else exercise.copy(
                    id = chosen.id,
                    name = chosen.name,
                    imageAssetName = chosen.imageAssetName,
                    notes = chosen.notes,
                    tracksWeight = chosen.tracksWeight,
                    equipment = chosen.equipment,
                    plannedExerciseId = chosen.id,
                    plannedName = chosen.name,
                )
            })
        })
    }

    private suspend fun qualifyingLogs(session: Long, exerciseId: Long): List<SetLogEntity> =
        workoutRepository.getSessionSets(session)
            .filter { it.exerciseId == exerciseId && it.setType != SetType.WARMUP }

    private fun priorRound(exercise: WorkoutExercise, log: SetLogEntity): PriorRound {
        val value = if (exercise.repType == RepType.REPS) log.reps else log.durationSeconds
        return PriorRound(value, log.weightKg)
    }

    private fun locate(exerciseId: Long): Pair<com.example.repsgrams.domain.session.WorkoutBlock, WorkoutExercise>? {
        val hits = plan.blocks.mapNotNull { block ->
            block.exercises.firstOrNull { it.id == exerciseId }?.let { block to it }
        }
        return hits.firstOrNull { it.first.kind != BlockKind.WARM_UP } ?: hits.firstOrNull()
    }

    private fun progressionLine(
        exercise: WorkoutExercise,
        block: com.example.repsgrams.domain.session.WorkoutBlock,
        logs: List<SetLogEntity>,
    ): String? {
        val sets = logs.filter { it.exerciseId == exercise.id && it.setType != SetType.WARMUP }
        if (sets.isEmpty() || block.kind == BlockKind.WARM_UP) return null
        return sentenceFor(exercise, block, sets)
    }

    private fun sentenceFor(
        exercise: WorkoutExercise,
        block: com.example.repsgrams.domain.session.WorkoutBlock,
        sets: List<SetLogEntity>,
    ): String {
        val result = ProgressionCalculator.decide(
            enabled = progressionEnabled,
            workingSets = sets.map { priorRound(exercise, it) },
            targetHigh = exercise.targetValueHigh,
            targetRoundsMin = block.targetRoundsMin,
            repType = exercise.repType,
            tracksWeight = exercise.tracksWeight,
        )
        val unit = if (unitSystem == UnitSystem.LB) "lb" else "kg"
        val last = sets.lastOrNull { it.weightKg != null }?.weightKg
        return when (result) {
            ProgressionResult.HOLD -> {
                val named = last?.let { "${formatWeight(displayWeight(it))} $unit" }
                progressionSentence(result, named, exercise.targetValueLow)
            }
            ProgressionResult.INCREASE_LOAD -> {
                val next = (last ?: 0f) + loadIncrementKg(exercise)
                progressionSentence(result, "${formatWeight(displayWeight(next))} $unit", exercise.targetValueLow)
            }
            ProgressionResult.ADVANCE_TARGET -> progressionSentence(result, null, exercise.targetValueLow)
        }
    }

    private fun endLines(): List<String> {
        if (!::plan.isInitialized) return emptyList()
        val working = sessionLogs.filter { it.setType != SetType.WARMUP }.groupBy { it.exerciseId }
        return working.mapNotNull { (exerciseId, sets) ->
            val located = locate(exerciseId) ?: return@mapNotNull null
            if (located.first.kind == BlockKind.WARM_UP) return@mapNotNull null
            "${located.second.name} — ${sentenceFor(located.second, located.first, sets)}"
        }
    }

    private suspend fun recordProgression() {
        val logs = workoutRepository.getSessionSets(sessionId).filter { it.setType != SetType.WARMUP }
        var maps = progressionMaps
        for ((exerciseId, sets) in logs.groupBy { it.exerciseId }) {
            val located = locate(exerciseId) ?: continue
            if (located.first.kind == BlockKind.WARM_UP) continue
            val result = ProgressionCalculator.decide(
                enabled = progressionEnabled,
                workingSets = sets.map { priorRound(located.second, it) },
                targetHigh = located.second.targetValueHigh,
                targetRoundsMin = located.first.targetRoundsMin,
                repType = located.second.repType,
                tracksWeight = located.second.tracksWeight,
            )
            val skipped = maps.skipped[exerciseId] == sessionId
            maps = ProgressionCalculator.recordOnFinish(
                maps,
                exerciseId,
                sessionId,
                qualified = result != ProgressionResult.HOLD,
                skipped = skipped,
            )
        }
        progressionMaps = maps
        cycleSettingsRepository.setProgressionQualified(maps.qualified)
        cycleSettingsRepository.setProgressionSkipped(maps.skipped)
    }

    private fun plateView(exercise: WorkoutExercise): PlateView? {
        if (!exercise.tracksWeight) return null
        val dumbbell = exercise.equipment == Equipment.DUMBBELL
        val barbell = exercise.equipment == Equipment.BARBELL || exercise.equipment == Equipment.OTHER
        if (!dumbbell && !barbell) return null
        val unit = if (unitSystem == UnitSystem.LB) "lb" else "kg"
        val target = weightInput.toFloatOrNull()
        if (dumbbell) {
            val text = target?.let { "${formatWeight(it)} $unit" } ?: "—"
            return PlateView(text, null, null, null, null, dumbbell = true, underBar = false)
        }
        val bar = if (unitSystem == UnitSystem.LB) barbellLb else barbellKg
        val plates = parsePlateList(if (unitSystem == UnitSystem.LB) platesLb else platesKg)
        val breakdown = calculatePlates(target ?: 0f, bar, plates)
        val assumption = if (exercise.equipment == Equipment.OTHER) {
            "Assumes a ${formatWeight(bar)} $unit bar"
        } else {
            null
        }
        return PlateView(
            caption = plateCaption(breakdown, unit),
            struck = breakdown.makeable?.let { "${formatWeight(target ?: 0f)} $unit" },
            makeable = breakdown.makeable?.let { "${formatWeight(it)} $unit" },
            difference = breakdown.difference?.let { "${formatWeight(it)} $unit" },
            assumption = assumption,
            dumbbell = false,
            underBar = breakdown.underBar,
        )
    }

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
            extraRounds = extraRounds,
            substitutes = formatSubstitutes(substitutes),
            appliedProgression = formatProgressionMap(appliedSessionByExercise),
            warmupExercises = formatIdList(warmupExerciseIds),
            freestyleExercises = formatIdList(freestyleOrder),
        ),
    )

    companion object {
        private const val MAX_EXTRA_ROUNDS = 10

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

private fun formatIdList(ids: Collection<Long>): String = ids.joinToString(",")

private fun parseIdList(stored: String): List<Long> =
    stored.split(',').mapNotNull { it.trim().toLongOrNull() }.distinct()

private fun freestyleIdsFromLogs(logs: List<SetLogEntity>): List<Long> {
    val ids = mutableListOf<Long>()
    for (log in logs) {
        if (log.exerciseId !in ids) ids += log.exerciseId
    }
    return ids
}

private fun formatSubstitutes(map: Map<Long, Long>): String =
    map.entries.joinToString(",") { (linkId, exerciseId) -> "$linkId:$exerciseId" }

private fun parseSubstitutes(stored: String): Map<Long, Long> {
    if (stored.isBlank()) return emptyMap()
    val parsed = LinkedHashMap<Long, Long>()
    for (token in stored.split(',')) {
        val parts = token.split(':')
        if (parts.size != 2) continue
        val linkId = parts[0].trim().toLongOrNull() ?: continue
        val exerciseId = parts[1].trim().toLongOrNull() ?: continue
        parsed[linkId] = exerciseId
    }
    return parsed
}
