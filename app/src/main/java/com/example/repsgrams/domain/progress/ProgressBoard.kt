package com.example.repsgrams.domain.progress

import com.example.repsgrams.data.datastore.UnitSystem
import com.example.repsgrams.data.db.ExerciseSetHistoryRow
import com.example.repsgrams.data.db.PersonalRecordEntity
import com.example.repsgrams.data.db.RepType
import com.example.repsgrams.domain.session.formatWeight
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

enum class ProgressMetric { BEST, VOLUME, EST }

data class ProgressPoint(
    val date: LocalDate,
    val value: Float,
    val record: Boolean,
    val detail: String,
)

data class ProgressSeries(
    val unit: String,
    val loadMetrics: Boolean,
    val points: List<ProgressPoint>,
)

data class ProgressRecord(
    val id: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val label: String,
    val value: String,
    val date: LocalDate,
    val estimate: Boolean,
)

private val PROGRESS_DATE = DateTimeFormatter.ofPattern("d MMM", Locale.US)
private val MEASUREMENT_ORDER = listOf("waist", "chest", "arms", "thighs", "calves", "shoulders", "neck")
private const val CENTIMETERS_PER_INCH = 2.54f

fun formatProgressDate(date: LocalDate): String = date.format(PROGRESS_DATE)

fun orderedMeasurements(selected: Set<String>): List<String> {
    val known = MEASUREMENT_ORDER.filter { it in selected }
    val extra = selected.filter { it !in MEASUREMENT_ORDER }.sorted()
    return known + extra
}

fun measurementToDisplay(centimeters: Float, unit: UnitSystem): Float =
    if (unit == UnitSystem.LB) centimeters / CENTIMETERS_PER_INCH else centimeters

fun measurementToCentimeters(entered: Float, unit: UnitSystem): Float =
    if (unit == UnitSystem.LB) entered * CENTIMETERS_PER_INCH else entered

/**
 * One point per session. A BEST point is red only when its load is strictly
 * heavier than every earlier point. The first point is not a record.
 * EST uses Epley on reps 2 through 10 and is never red.
 */
fun progressSeries(
    rows: List<ExerciseSetHistoryRow>,
    metric: ProgressMetric,
    unit: UnitSystem,
    tracksWeight: Boolean,
): ProgressSeries {
    val timed = isTimed(rows, tracksWeight)
    val loadMetrics = !timed && (tracksWeight || rows.any { (it.weightKg ?: 0f) > 0f })
    val points = when {
        timed -> durationPoints(rows)
        loadMetrics -> when (metric) {
            ProgressMetric.BEST -> bestPoints(rows, unit)
            ProgressMetric.VOLUME -> volumePoints(rows, unit)
            ProgressMetric.EST -> estimatePoints(rows, unit)
        }
        else -> repPoints(rows)
    }
    val label = when {
        timed -> "SEC"
        loadMetrics -> if (unit == UnitSystem.LB) "LB" else "KG"
        else -> "REPS"
    }
    return ProgressSeries(label, loadMetrics, points)
}

fun progressRecords(
    records: List<PersonalRecordEntity>,
    names: Map<Long, String>,
    unit: UnitSystem,
): List<ProgressRecord> {
    return records.mapNotNull { record ->
        val label = recordLabel(record.type, unit) ?: return@mapNotNull null
        val value = recordValue(record, unit) ?: return@mapNotNull null
        ProgressRecord(
            id = record.id,
            exerciseId = record.exerciseId,
            exerciseName = names[record.exerciseId] ?: "Exercise",
            label = label,
            value = value,
            date = record.achievedDate,
            estimate = record.type == "estimated1RM",
        ) to recordSort(record.type)
    }.sortedWith(compareBy({ it.first.exerciseName.lowercase(Locale.US) }, { it.second }, { it.first.date }))
        .map { it.first }
}

private fun recordSort(type: String): Float = when {
    type == "maxWeight" -> 0f
    type == "estimated1RM" -> Float.MAX_VALUE
    type.startsWith("maxReps_") -> 1f + (type.removePrefix("maxReps_").toFloatOrNull() ?: 0f)
    else -> Float.MAX_VALUE
}

private fun isTimed(rows: List<ExerciseSetHistoryRow>, tracksWeight: Boolean): Boolean {
    if (rows.any { it.repType == RepType.SECONDS.name }) return true
    if (tracksWeight || rows.isEmpty()) return false
    return rows.none { it.reps != null } && rows.any { it.durationSeconds != null }
}

private fun sessions(rows: List<ExerciseSetHistoryRow>): List<List<ExerciseSetHistoryRow>> =
    rows.groupBy { it.sessionId }
        .values
        .sortedWith(compareBy({ it.first().date }, { it.first().sessionId }))

private fun bestPoints(rows: List<ExerciseSetHistoryRow>, unit: UnitSystem): List<ProgressPoint> {
    var heaviest: Float? = null
    val points = mutableListOf<ProgressPoint>()
    for (sets in sessions(rows)) {
        val best = sets.filter { (it.weightKg ?: 0f) > 0f }
            .maxWithOrNull(compareBy<ExerciseSetHistoryRow> { it.weightKg ?: 0f }.thenBy { it.reps ?: 0 })
            ?: continue
        val kilograms = best.weightKg ?: continue
        val earlier = heaviest
        val record = earlier != null && kilograms > earlier
        if (earlier == null || kilograms > earlier) heaviest = kilograms
        val shown = displayLoad(kilograms, unit)
        val load = "${formatWeight(shown)} ${unitWord(unit)}"
        val reps = best.reps?.takeIf { it > 0 }
        points += ProgressPoint(
            date = sets.first().date,
            value = shown,
            record = record,
            detail = if (reps != null) "$reps × $load" else load,
        )
    }
    return points
}

private fun volumePoints(rows: List<ExerciseSetHistoryRow>, unit: UnitSystem): List<ProgressPoint> =
    sessions(rows).mapNotNull { sets ->
        var total = 0.0
        var any = false
        for (set in sets) {
            val kilograms = set.weightKg ?: continue
            val reps = set.reps ?: continue
            if (reps <= 0 || kilograms <= 0f || !kilograms.isFinite()) continue
            total += displayLoad(kilograms, unit).toDouble() * reps
            any = true
        }
        if (!any) return@mapNotNull null
        val shown = total.toFloat()
        ProgressPoint(
            date = sets.first().date,
            value = shown,
            record = false,
            detail = "${formatWeight(shown)} ${unitWord(unit)}",
        )
    }

private fun estimatePoints(rows: List<ExerciseSetHistoryRow>, unit: UnitSystem): List<ProgressPoint> =
    sessions(rows).mapNotNull { sets ->
        val best = sets.mapNotNull { set ->
            val kilograms = set.weightKg ?: return@mapNotNull null
            val reps = set.reps ?: return@mapNotNull null
            if (reps !in 2..10 || kilograms <= 0f) return@mapNotNull null
            kilograms * (1f + reps / 30f)
        }.maxOrNull() ?: return@mapNotNull null
        val shown = displayLoad(best, unit)
        ProgressPoint(
            date = sets.first().date,
            value = shown,
            record = false,
            detail = "${formatWeight(shown)} ${unitWord(unit)}",
        )
    }

private fun durationPoints(rows: List<ExerciseSetHistoryRow>): List<ProgressPoint> =
    sessions(rows).mapNotNull { sets ->
        val seconds = sets.mapNotNull { it.durationSeconds?.takeIf { value -> value > 0 } }.maxOrNull()
            ?: return@mapNotNull null
        ProgressPoint(sets.first().date, seconds.toFloat(), false, "${seconds}s")
    }

private fun repPoints(rows: List<ExerciseSetHistoryRow>): List<ProgressPoint> =
    sessions(rows).mapNotNull { sets ->
        val reps = sets.mapNotNull { it.reps?.takeIf { value -> value > 0 } }.maxOrNull() ?: return@mapNotNull null
        ProgressPoint(sets.first().date, reps.toFloat(), false, reps.toString())
    }

private fun displayLoad(kilograms: Float, unit: UnitSystem): Float =
    if (unit == UnitSystem.LB) kilogramsToPounds(kilograms) else kilograms

private fun unitWord(unit: UnitSystem): String = if (unit == UnitSystem.LB) "lb" else "kg"

private fun recordLabel(type: String, unit: UnitSystem): String? = when {
    type == "maxWeight" -> "Heaviest"
    type == "estimated1RM" -> "EST"
    type.startsWith("maxReps_") -> {
        val kilograms = type.removePrefix("maxReps_").toFloatOrNull() ?: return null
        "Reps at ${formatWeight(displayLoad(kilograms, unit))} ${unitWord(unit)}"
    }
    else -> null
}

private fun recordValue(record: PersonalRecordEntity, unit: UnitSystem): String? = when {
    record.type == "maxWeight" || record.type == "estimated1RM" ->
        "${formatWeight(displayLoad(record.value, unit))} ${unitWord(unit)}"
    record.type.startsWith("maxReps_") -> formatWeight(record.value)
    else -> null
}

