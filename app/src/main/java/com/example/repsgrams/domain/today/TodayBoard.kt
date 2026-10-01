package com.example.repsgrams.domain.today

import com.example.repsgrams.data.db.BlockKind
import com.example.repsgrams.data.db.RepType
import com.example.repsgrams.data.db.SupplementEntity
import com.example.repsgrams.data.db.SupplyInventoryEntity
import com.example.repsgrams.data.db.WorkoutTemplateEntity
import com.example.repsgrams.domain.calendar.CalendarDay
import com.example.repsgrams.domain.progress.ProgressStatsCalculator
import com.example.repsgrams.domain.schedule.ScheduleSuggestion
import com.example.repsgrams.domain.schedule.SuggestionStatus
import com.example.repsgrams.domain.session.WorkoutExercise
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

enum class TodayHeroMode { LIVE, DONE, REST, NO_PROGRAM, START }

enum class TodayAlternate { SHEET, TEMPLATES, NONE }

sealed interface TodayMark {
    data class Token(val text: String) : TodayMark
    data class Words(val text: String) : TodayMark
    data object Rest : TodayMark
    data object Elapsed : TodayMark
    data object None : TodayMark
}

data class TodayHero(
    val mode: TodayHeroMode,
    val label: String,
    val mark: TodayMark,
    val caption: String?,
    val pill: String?,
    val alternate: TodayAlternate,
)

data class TodayExerciseLine(
    val name: String,
    val low: Int,
    val high: Int,
    val unit: String,
    val perSide: Boolean,
    val supersetWithPrevious: Boolean,
)

data class TodaySupply(
    val supplementId: Long,
    val name: String,
    val servings: Int,
)

fun TodayExerciseLine.figures(): String {
    val range = if (low == high) low.toString() else "$low\u2013$high"
    return "$range $unit"
}

/**
 * Live session, then a workout finished today, then rest, then no program, then start.
 * Done stays ahead of a rest gap, so finishing A today still names A.
 */
fun heroFor(
    hasActiveSession: Boolean,
    completedToday: WorkoutTemplateEntity?,
    suggestion: ScheduleSuggestion,
    hasTemplates: Boolean,
    activeTemplateName: String? = null,
): TodayHero {
    if (hasActiveSession) {
        return TodayHero(
            mode = TodayHeroMode.LIVE,
            label = "LIVE",
            mark = TodayMark.Elapsed,
            caption = activeTemplateName,
            pill = "RESUME",
            alternate = TodayAlternate.NONE,
        )
    }
    if (completedToday != null) {
        return TodayHero(
            mode = TodayHeroMode.DONE,
            label = "DONE",
            mark = markFor(completedToday),
            caption = doneCaption(suggestion.suggestedTemplate, suggestion.dueDate),
            pill = null,
            alternate = TodayAlternate.SHEET,
        )
    }
    if (suggestion.status == SuggestionStatus.REST_DAY) {
        return TodayHero(
            mode = TodayHeroMode.REST,
            label = "RECOVERY",
            mark = TodayMark.Rest,
            caption = restCaption(suggestion),
            pill = null,
            alternate = TodayAlternate.SHEET,
        )
    }
    val template = suggestion.suggestedTemplate
    if (template == null || !hasTemplates) {
        return TodayHero(
            mode = TodayHeroMode.NO_PROGRAM,
            label = "NO PROGRAM",
            mark = TodayMark.None,
            caption = null,
            pill = null,
            alternate = TodayAlternate.TEMPLATES,
        )
    }
    return TodayHero(
        mode = TodayHeroMode.START,
        label = if (suggestion.status == SuggestionStatus.OVERDUE) "OVERDUE" else "SUGGESTED",
        mark = markFor(template),
        caption = startCaption(template, suggestion),
        pill = "START",
        alternate = TodayAlternate.SHEET,
    )
}

fun upNextTemplateId(
    hasActiveSession: Boolean,
    activeTemplateId: Long?,
    completedToday: WorkoutTemplateEntity?,
    suggestion: ScheduleSuggestion,
    hasTemplates: Boolean,
): Long? = when (heroFor(hasActiveSession, completedToday, suggestion, hasTemplates).mode) {
    TodayHeroMode.LIVE -> activeTemplateId
    TodayHeroMode.START -> suggestion.suggestedTemplate?.id
    else -> null
}

/** Monday through Sunday of the week that contains [today], taken from the calendar grid. */
fun weekContaining(days: List<CalendarDay>, today: LocalDate): List<CalendarDay> {
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return (0L..6L).mapNotNull { offset ->
        val date = monday.plusDays(offset)
        days.find { it.date == date }
    }
}

fun previewLines(
    blocks: List<Pair<BlockKind, List<WorkoutExercise>>>,
    startBlock: Int = 0,
    startExercise: Int = 0,
    limit: Int = 3,
): List<TodayExerciseLine> {
    if (blocks.isEmpty() || limit <= 0) return emptyList()
    val inRange = startBlock in blocks.indices && startExercise in blocks[startBlock].second.indices
    val fromBlock = if (inRange) startBlock else 0
    val fromExercise = if (inRange) startExercise else 0
    val lines = mutableListOf<TodayExerciseLine>()
    for (blockIndex in fromBlock..blocks.lastIndex) {
        val (kind, exercises) = blocks[blockIndex]
        val from = if (blockIndex == fromBlock) fromExercise else 0
        var shownInBlock = 0
        for (exerciseIndex in from..exercises.lastIndex) {
            if (lines.size == limit) return lines
            val exercise = exercises[exerciseIndex]
            lines += TodayExerciseLine(
                name = exercise.name,
                low = exercise.targetValueLow,
                high = exercise.targetValueHigh,
                unit = if (exercise.repType == RepType.SECONDS) "SEC" else "REPS",
                perSide = exercise.perSide,
                supersetWithPrevious = kind == BlockKind.SUPERSET && shownInBlock > 0,
            )
            shownInBlock++
        }
    }
    return lines
}

fun lowSupplyTile(
    inventory: List<SupplyInventoryEntity>,
    supplements: List<SupplementEntity>,
    today: LocalDate,
    calculator: ProgressStatsCalculator = ProgressStatsCalculator(),
): TodaySupply? {
    val ranked = supplements.filter { it.isActive }.mapNotNull { supplement ->
        val row = inventory.find { it.supplementId == supplement.id } ?: return@mapNotNull null
        val status = calculator.calculateSupplyStatus(row, today, supplement.lowSupplyThreshold.toFloat())
        if (!status.isLow) null else Triple(supplement.id, supplement.name, status.servingsRemaining)
    }
    val pick = ranked.minWithOrNull(compareBy<Triple<Long, String, Float>> { it.third }.thenBy { it.first })
        ?: return null
    return TodaySupply(pick.first, pick.second, pick.third.toInt())
}

const val DOSE_DELETE = "delete"

/** Digits and one decimal point. Anything else is ignored. */
fun applyDoseKey(current: String, key: String): String = when (key) {
    DOSE_DELETE -> current.dropLast(1)
    "." -> when {
        current.contains('.') -> current
        current.isEmpty() -> "0."
        current.length >= 6 -> current
        else -> current + "."
    }
    else -> {
        if (key.length != 1 || !key[0].isDigit() || current.length >= 6) current
        else current + key
    }
}

fun parseDose(text: String): Float? {
    val normalized = text.removeSuffix(".")
    if (normalized.isEmpty() || normalized.count { it == '.' } > 1) return null
    return normalized.toFloatOrNull()?.takeIf { it.isFinite() && it >= 0f }
}

fun formatDose(amount: Float): String =
    if (amount.isFinite() && amount % 1f == 0f) amount.toInt().toString() else amount.toString()

fun formatElapsed(start: Instant, now: Instant): String {
    val seconds = ((now.toEpochMilli() - start.toEpochMilli()).coerceAtLeast(0L) / 1_000L).toInt()
    val hours = seconds / 3_600
    val minutes = (seconds % 3_600) / 60
    val remain = seconds % 60
    return if (hours == 0) {
        "$minutes:${remain.toString().padStart(2, '0')}"
    } else {
        "$hours:${minutes.toString().padStart(2, '0')}:${remain.toString().padStart(2, '0')}"
    }
}

fun restRowLabel(status: SuggestionStatus): String =
    if (status == SuggestionStatus.REST_DAY) "Log rest" else "Rest today"

fun restDaysCaption(restDaysAfter: Int): String =
    if (restDaysAfter == 1) "1 rest day" else "$restDaysAfter rest days"

fun isSupplementScrollTarget(target: String?): Boolean =
    target == "supplements" || target == "creatine" || target == "whey"

/** Hero, week, optional up-next, then the supplement block. */
fun supplementItemIndex(showUpNext: Boolean): Int = if (showUpNext) 3 else 2

private fun markFor(template: WorkoutTemplateEntity): TodayMark {
    val token = template.dayLabel.trim()
    return if (token.length in 1..2) TodayMark.Token(token) else TodayMark.Words(template.name)
}

private fun doneCaption(next: WorkoutTemplateEntity?, due: LocalDate?): String? {
    if (next == null) return null
    val token = next.dayLabel.trim()
    val who = if (token.length in 1..2) token else next.name
    if (due == null) return who
    val day = due.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.US)
    return "$who \u00B7 due $day"
}

private fun restCaption(suggestion: ScheduleSuggestion): String? {
    val name = suggestion.suggestedTemplate?.name ?: return null
    val days = suggestion.daysUntilDue
    val unit = if (days == 1) "day" else "days"
    return "$days $unit until $name"
}

private fun startCaption(template: WorkoutTemplateEntity, suggestion: ScheduleSuggestion): String? {
    if (suggestion.status == SuggestionStatus.OVERDUE) {
        val days = suggestion.overdueByDays
        val unit = if (days == 1) "day" else "days"
        return "Due $days $unit ago"
    }
    return if (template.dayLabel.trim().length in 1..2) template.name else null
}
