package com.example.repsgrams.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.repsgrams.data.datastore.CycleSettingsRepository
import com.example.repsgrams.data.datastore.SessionProgress
import com.example.repsgrams.data.datastore.SessionProgressStore
import com.example.repsgrams.data.db.ScheduleMode
import com.example.repsgrams.data.db.SessionKind
import com.example.repsgrams.data.db.SupplementEntity
import com.example.repsgrams.data.db.WorkoutSessionEntity
import com.example.repsgrams.data.db.WorkoutTemplateEntity
import com.example.repsgrams.data.db.WorkoutTemplateWithBlocks
import com.example.repsgrams.data.repository.CalendarRepository
import com.example.repsgrams.data.repository.ProgressRepository
import com.example.repsgrams.data.repository.ScheduleRepository
import com.example.repsgrams.data.repository.SupplementRepository
import com.example.repsgrams.data.repository.WorkoutRepository
import com.example.repsgrams.domain.calendar.CalendarCalculator
import com.example.repsgrams.domain.calendar.CalendarDay
import com.example.repsgrams.domain.progress.ProgressStatsCalculator
import com.example.repsgrams.domain.schedule.ScheduleSuggestion
import com.example.repsgrams.domain.schedule.scopeToActiveProgram
import com.example.repsgrams.domain.session.WorkoutExercise
import com.example.repsgrams.domain.today.TodayExerciseLine
import com.example.repsgrams.domain.today.TodayHero
import com.example.repsgrams.domain.today.TodaySupply
import com.example.repsgrams.domain.today.heroFor
import com.example.repsgrams.domain.today.lowSupplyTile
import com.example.repsgrams.domain.today.previewLines
import com.example.repsgrams.domain.today.upNextTemplateId
import com.example.repsgrams.domain.today.weekContaining
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TodaySupplement(val supplement: SupplementEntity, val taken: Boolean, val actualAmount: Float)

private data class TodayPlan(
    val suggestion: ScheduleSuggestion,
    val supplements: List<TodaySupplement>,
    val activeSession: WorkoutSessionEntity?,
    val templates: List<WorkoutTemplateEntity>,
    val completedToday: WorkoutTemplateEntity?,
    val weekly: Boolean,
)

private data class TodaySnapshot(
    val plan: TodayPlan,
    val progress: SessionProgress?,
    val streak: Int,
    val supply: TodaySupply?,
    val week: List<CalendarDay>,
)

sealed interface TodayUiState {
    data object Loading : TodayUiState

    data class Content(
        val hero: TodayHero,
        val activeSessionId: Long?,
        val sessionStartedAt: Instant?,
        val week: List<CalendarDay>,
        val upNext: List<TodayExerciseLine>,
        val supplements: List<TodaySupplement>,
        val templates: List<WorkoutTemplateEntity>,
        val suggestion: ScheduleSuggestion,
        val currentStreak: Int,
        val supply: TodaySupply?,
        val weekly: Boolean = false,
    ) : TodayUiState

    data class Error(val message: String) : TodayUiState
}

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(
    private val scheduleRepository: ScheduleRepository,
    private val supplementRepository: SupplementRepository,
    private val workoutRepository: WorkoutRepository,
    private val cycleSettingsRepository: CycleSettingsRepository,
    private val progressRepository: ProgressRepository,
    private val calendarRepository: CalendarRepository,
    private val sessionProgressStore: SessionProgressStore,
    private val today: LocalDate = LocalDate.now(),
) : ViewModel() {
    private val _openSession = MutableSharedFlow<Long>()
    val openSession: SharedFlow<Long> = _openSession.asSharedFlow()

    private val progressStatsCalculator = ProgressStatsCalculator()

    private val planFlow: Flow<TodayPlan> = combine(
        scheduleRepository.observeSuggestion(today),
        combine(
            supplementRepository.observeAllSupplements(),
            supplementRepository.observeIntakesForDate(today),
        ) { allSupps, logs ->
            allSupps.filter { it.isActive }.map { supplement ->
                val log = logs.firstOrNull { it.supplementId == supplement.id }
                if (log != null && log.taken) {
                    TodaySupplement(supplement, true, log.actualAmount)
                } else {
                    TodaySupplement(supplement, false, supplement.doseAmount)
                }
            }
        },
        workoutRepository.observeActiveSession(),
        combine(
            workoutRepository.observeAllTemplates(),
            workoutRepository.observePrograms(),
        ) { templates, programs -> templates to programs },
        workoutRepository.observeSessionsForDate(today),
    ) { suggestion, todaySupps, activeSession, programTemplates, sessionsToday ->
        val (templates, programs) = programTemplates
        val ordered = templates.sortedWith(compareBy({ it.orderIndex }, { it.dayLabel }))
        val scope = scopeToActiveProgram(programs, ordered)
        val activeTemplates = scope.templates.sortedWith(compareBy({ it.orderIndex }, { it.dayLabel }))
        val logged = CalendarCalculator.completedWorkoutOn(sessionsToday, ordered)
        val completed = ordered.find { it.id == logged?.templateId }
        val trained = completed != null || sessionsToday.any {
            it.completed && it.sessionKind == SessionKind.FREESTYLE
        }
        TodayPlan(
            suggestion = suggestion,
            supplements = dueToday(todaySupps, trained),
            activeSession = activeSession,
            templates = activeTemplates,
            completedToday = completed,
            weekly = scope.mode == ScheduleMode.WEEKLY,
        )
    }

    val uiState: StateFlow<TodayUiState> = combine(
        planFlow,
        sessionProgressStore.progress,
        cycleSettingsRepository.settings.flatMapLatest { settings ->
            progressRepository.observeStreakInfo(today, settings.adherenceGraceDays)
        },
        combine(
            progressRepository.observeSupplyInventory(),
            supplementRepository.observeAllSupplements(),
        ) { supplies, allSupps ->
            lowSupplyTile(supplies, allSupps, today, progressStatsCalculator)
        },
        calendarRepository.observeMonth(YearMonth.from(today)),
    ) { plan, progress, streak, supply, month ->
        TodaySnapshot(plan, progress, streak.currentStreak, supply, weekContaining(month.days, today))
    }.flatMapLatest { snapshot ->
        val plan = snapshot.plan
        val templateId = upNextTemplateId(
            hasActiveSession = plan.activeSession != null,
            activeTemplateId = plan.activeSession?.templateId,
            completedToday = plan.completedToday,
            suggestion = plan.suggestion,
            hasTemplates = plan.templates.isNotEmpty(),
        )
        val cursor = snapshot.progress?.takeIf { it.sessionId == plan.activeSession?.id }
        val lines = if (templateId == null) {
            flowOf(emptyList())
        } else {
            workoutRepository.observePlanGraph(templateId).map { graph ->
                exerciseLines(graph, cursor?.blockIndex ?: 0, cursor?.exerciseIndex ?: 0)
            }
        }
        lines.map { snapshot to it }
    }.map { (snapshot, lines) ->
        val plan = snapshot.plan
        TodayUiState.Content(
            hero = heroFor(
                hasActiveSession = plan.activeSession != null,
                completedToday = plan.completedToday,
                suggestion = plan.suggestion,
                hasTemplates = plan.templates.isNotEmpty(),
                activeTemplateName = plan.templates.find { it.id == plan.activeSession?.templateId }?.name
                    ?: if (plan.activeSession?.sessionKind == SessionKind.FREESTYLE) "Empty workout" else null,
            ),
            activeSessionId = plan.activeSession?.id,
            sessionStartedAt = plan.activeSession?.startTime,
            week = snapshot.week,
            upNext = lines,
            supplements = plan.supplements,
            templates = plan.templates,
            suggestion = plan.suggestion,
            currentStreak = snapshot.streak,
            supply = snapshot.supply,
            weekly = plan.weekly,
        ) as TodayUiState
    }.flowOn(Dispatchers.Default).catch {
        emit(TodayUiState.Error("Couldn't load today's plan."))
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = TodayUiState.Loading,
    )

    private fun dueToday(supplements: List<TodaySupplement>, workoutCompletedToday: Boolean): List<TodaySupplement> =
        supplements.filter {
            val supplement = it.supplement
            when (supplement.scheduleType) {
                "daily" -> true
                "workoutDayOnly" -> workoutCompletedToday
                "customDays" -> {
                    val currentDay = today.dayOfWeek.name.take(3).replaceFirstChar { char -> char.uppercase() }
                    supplement.customDays?.contains(currentDay, ignoreCase = true) == true
                }
                else -> true
            }
        }

    fun setSupplementTaken(supplement: SupplementEntity, taken: Boolean, amount: Float? = null) {
        viewModelScope.launch { supplementRepository.setSupplementTaken(today, supplement, taken, amount) }
    }

    fun startWorkout(templateId: Long) {
        viewModelScope.launch { _openSession.emit(workoutRepository.startSession(templateId)) }
    }

    fun startEmptyWorkout() {
        viewModelScope.launch { _openSession.emit(workoutRepository.startFreestyle()) }
    }

    fun resumeWorkout(sessionId: Long) {
        viewModelScope.launch { _openSession.emit(sessionId) }
    }

    fun logRestDay() {
        viewModelScope.launch { workoutRepository.logRestDay(today) }
    }

    companion object {
        fun factory(
            scheduleRepository: ScheduleRepository,
            supplementRepository: SupplementRepository,
            workoutRepository: WorkoutRepository,
            cycleSettingsRepository: CycleSettingsRepository,
            progressRepository: ProgressRepository,
            calendarRepository: CalendarRepository,
            sessionProgressStore: SessionProgressStore,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(TodayViewModel::class.java))
                return TodayViewModel(
                    scheduleRepository,
                    supplementRepository,
                    workoutRepository,
                    cycleSettingsRepository,
                    progressRepository,
                    calendarRepository,
                    sessionProgressStore,
                ) as T
            }
        }
    }
}

private fun exerciseLines(
    graph: WorkoutTemplateWithBlocks?,
    blockIndex: Int,
    exerciseIndex: Int,
): List<TodayExerciseLine> {
    if (graph == null) return emptyList()
    val blocks = graph.blocks.sortedBy { it.block.orderIndex }.map { block ->
        block.block.kind to block.exercises.sortedBy { it.assignment.orderIndex }.map { row ->
            val link = row.assignment
            val exercise = row.exercise
            WorkoutExercise(
                id = exercise.id,
                name = exercise.name,
                imageAssetName = exercise.imageAssetName,
                notes = exercise.notes,
                tracksWeight = exercise.tracksWeight,
                targetValueLow = link.targetValueLow,
                targetValueHigh = link.targetValueHigh,
                repType = link.repType,
                perSide = link.perSide,
            )
        }
    }
    return previewLines(blocks, blockIndex, exerciseIndex)
}
