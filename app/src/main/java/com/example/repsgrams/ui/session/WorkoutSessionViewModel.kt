package com.example.repsgrams.ui.session

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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface WorkoutSessionUiState {
    data object Loading : WorkoutSessionUiState
    data class Active(
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
    ) : WorkoutSessionUiState

    data class Summary(
        val workoutName: String,
        val setCount: Int,
        val durationSeconds: Int,
        val maxDurationMinutes: Int,
        val wheyTaken: Boolean,
        val creatineTaken: Boolean,
    ) : WorkoutSessionUiState

    data class Error(val message: String) : WorkoutSessionUiState
}

class WorkoutSessionViewModel(
    private val sessionId: Long,
    private val workoutRepository: WorkoutRepository,
    private val progressStore: SessionProgressStore,
    private val supplementRepository: SupplementRepository,
    private val cycleSettingsRepository: CycleSettingsRepository,
    private val clock: Clock,
) : ViewModel() {
    private val _uiState = MutableStateFlow<WorkoutSessionUiState>(WorkoutSessionUiState.Loading)
    val uiState: StateFlow<WorkoutSessionUiState> = _uiState.asStateFlow()

    private val _restFinished = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val restFinished: SharedFlow<Unit> = _restFinished.asSharedFlow()
    private val _summaryDone = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val summaryDone: SharedFlow<Unit> = _summaryDone.asSharedFlow()

    private lateinit var session: WorkoutSessionEntity
    private lateinit var plan: WorkoutPlan
    private var cursor = SessionCursor(0, 0, 1)
    private var restEndEpochMillis: Long? = null
    private var valueInput = ""
    private var weightInput = ""
    private var isSaving = false
    private var unitSystem = UnitSystem.KG
    private var progressionSuggestion: ProgressionSuggestion? = null

    init {
        viewModelScope.launch {
            runCatching { load() }.onFailure {
                _uiState.value = WorkoutSessionUiState.Error(it.message ?: "Couldn't load this workout.")
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
        } else {
            persistProgress()
        }
        if (restEndEpochMillis != null && restEndEpochMillis!! <= clock.millis()) {
            restEndEpochMillis = null
            persistProgress()
        }
        loadInputDefaults()
        publishActive()
    }

    fun updateValue(value: String) {
        if (value.all(Char::isDigit)) {
            valueInput = value.take(4)
            publishActive()
        }
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
                        ?.let { if (unitSystem == UnitSystem.LB) it / POUNDS_PER_KILOGRAM else it }
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
        restEndEpochMillis = end + seconds * 1_000L
        viewModelScope.launch { persistProgress() }
        publishActive()
    }

    fun skipRest() {
        if (restEndEpochMillis == null) return
        restEndEpochMillis = null
        viewModelScope.launch { persistProgress() }
        publishActive()
    }

    fun finishWorkout() {
        if (!::session.isInitialized || session.completed) return
        viewModelScope.launch { finishAndSummarize() }
    }

    fun setWheyTaken(taken: Boolean) {
        val summary = _uiState.value as? WorkoutSessionUiState.Summary ?: return
        _uiState.value = summary.copy(wheyTaken = taken)
    }

    fun setCreatineTaken(taken: Boolean) {
        val summary = _uiState.value as? WorkoutSessionUiState.Summary ?: return
        _uiState.value = summary.copy(creatineTaken = taken)
    }

    fun saveSummary() {
        val summary = _uiState.value as? WorkoutSessionUiState.Summary ?: return
        viewModelScope.launch {
            supplementRepository.setWheyTaken(session.date, summary.wheyTaken)
            supplementRepository.setCreatineTaken(session.date, summary.creatineTaken)
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
                persistProgress()
                loadInputDefaults()
                publishActive()
            }
            SessionAdvance.Finished -> finishAndSummarize()
        }
    }

    private suspend fun finishAndSummarize() {
        session = workoutRepository.finishSession(sessionId)
        progressStore.clear()
        restEndEpochMillis = null
        showSummary()
    }

    private suspend fun showSummary() {
        val supplement = supplementRepository.observeForDate(session.date).first()
        _uiState.value = WorkoutSessionUiState.Summary(
            workoutName = plan.name,
            setCount = workoutRepository.setCount(sessionId),
            durationSeconds = session.durationSeconds ?: elapsedSeconds(),
            maxDurationMinutes = plan.maxDurationMinutes,
            wheyTaken = supplement?.wheyTaken ?: true,
            creatineTaken = supplement?.creatineTaken ?: true,
        )
    }

    private suspend fun loadInputDefaults() {
        val exercise = currentExercise()
        val previous = workoutRepository.previousSet(exercise.id, sessionId)
        progressionSuggestion = null
        if (exercise.repType == RepType.REPS && plan.blocks[cursor.blockIndex].kind != BlockKind.WARM_UP) {
            progressionSuggestion = ProgressionCalculator.suggestionFor(
                exercise.targetValueHigh,
                exercise.tracksWeight,
                workoutRepository.previousSessionRounds(exercise.id, sessionId).map {
                    PriorRound(it.reps, it.weightKg)
                },
            )
        }
        valueInput = when (exercise.repType) {
            RepType.REPS -> previous?.reps
            RepType.SECONDS -> previous?.durationSeconds
        }?.toString() ?: exercise.targetValueLow.toString()
        weightInput = previous?.weightKg?.let {
            formatWeight(if (unitSystem == UnitSystem.LB) it * POUNDS_PER_KILOGRAM else it)
        } ?: if (exercise.tracksWeight) "0" else ""
    }

    private fun tick() {
        val end = restEndEpochMillis
        if (end != null && end <= clock.millis()) {
            restEndEpochMillis = null
            _restFinished.tryEmit(Unit)
            viewModelScope.launch { persistProgress() }
        }
        publishActive()
    }

    private fun publishActive() {
        if (!::plan.isInitialized || session.completed) return
        val block = plan.blocks[cursor.blockIndex]
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
        )
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
        ),
    )

    companion object {
        fun factory(
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
                    sessionId, workoutRepository, progressStore, supplementRepository,
                    cycleSettingsRepository, clock,
                ) as T
            }
        }
    }
}

private fun formatWeight(value: Float): String =
    if (value % 1f == 0f) value.toInt().toString() else "%.2f".format(value).trimEnd('0').trimEnd('.')

private const val POUNDS_PER_KILOGRAM = 2.2046226f
