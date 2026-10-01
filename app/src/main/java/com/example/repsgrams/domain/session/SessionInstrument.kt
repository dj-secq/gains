package com.example.repsgrams.domain.session

import com.example.repsgrams.data.datastore.UnitSystem
import com.example.repsgrams.data.db.BlockKind
import com.example.repsgrams.domain.progress.kilogramsToPounds
import java.util.Locale

enum class KeypadField { VALUE, LOAD }

data class KeypadEdit(val text: String, val fresh: Boolean)

data class ExercisePill(
    val label: String,
    val fraction: String,
    val selected: Boolean,
    val superset: Boolean,
    val groupId: Int,
    val minCaption: String? = null,
)

data class LoggedSetView(
    val roundNumber: Int,
    val reps: Int?,
    val durationSeconds: Int?,
    val weightDisplay: String?,
    val rpe: Float? = null,
)

data class SetRowModel(
    val label: String,
    val roundNumber: Int,
    val previousText: String,
    val loadText: String,
    val repsText: String,
    val complete: Boolean,
    val active: Boolean,
    val copyable: Boolean,
    val rpeText: String? = null,
)

data class RecordLine(val exerciseName: String, val detail: String)

const val PLANNED_DURATION_PASSED = "Planned duration has passed"

private val LOAD_PATTERN = Regex("\\d{0,4}(\\.\\d{0,2})?")

/** Blank cue notes stay off the logging surface. Arm Circles uses its cue sentence. */
fun cueText(notes: String?): String? = notes?.trim()?.takeIf { it.isNotEmpty() }

fun formatSessionElapsed(totalSeconds: Int): String {
    val seconds = totalSeconds.coerceAtLeast(0)
    val hours = seconds / 3_600
    val minutes = (seconds % 3_600) / 60
    val remain = seconds % 60
    return if (hours == 0) {
        "$minutes:${remain.toString().padStart(2, '0')}"
    } else {
        "$hours:${minutes.toString().padStart(2, '0')}:${remain.toString().padStart(2, '0')}"
    }
}

fun formatRestClock(remaining: Int, overtime: Int?): String {
    val over = overtime ?: 0
    return if (over > 0) "+${formatSessionElapsed(over)}" else formatSessionElapsed(remaining)
}

fun formatWeight(value: Float): String {
    if (!value.isFinite()) return "0"
    val text = String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
    return text.ifEmpty { "0" }
}

/** Product default until a settings increment exists. 2.5 kg or 5 lb. */
fun loadStep(unit: UnitSystem): Float = if (unit == UnitSystem.LB) 5f else 2.5f

fun durationPassed(elapsedSeconds: Int, maxDurationMinutes: Int): Boolean =
    maxDurationMinutes > 0 && elapsedSeconds >= maxDurationMinutes * 60

fun nextExerciseCaption(exerciseName: String): String = "Next · $exerciseName"

/**
 * `80 × 8`, `8`, or `40s`. Seconds with a load read `80 × 40s`.
 * No history is an em dash.
 */
fun formatPrevious(load: String?, value: String?, seconds: Boolean): String {
    val figure = value?.trim()?.takeIf { it.isNotEmpty() }?.let { if (seconds) "${it}s" else it }
    val plate = load?.trim()?.takeIf { it.isNotEmpty() }
    return when {
        plate != null && figure != null -> "$plate × $figure"
        figure != null -> figure
        plate != null -> plate
        else -> "—"
    }
}

/**
 * Working pills are numbered 1, 2, 3. A warm-up block is `W` and is outside that index.
 * Past blocks are max/max. The current exercise is round/max. Earlier exercises in the
 * same block use the current round. Later ones use round−1. Future blocks are 0/max.
 * A warm-up block's max is 1.
 */
fun exercisePills(
    blocks: List<WorkoutBlock>,
    cursor: SessionCursor,
    extraRounds: Int = 0,
): List<ExercisePill> {
    var working = 0
    return blocks.flatMapIndexed { blockIndex, block ->
        val grouped = block.exercises.size > 1
        val templateMax = if (block.kind == BlockKind.WARM_UP) 1 else block.targetRoundsMax.coerceAtLeast(1)
        val max = if (block.kind != BlockKind.WARM_UP && blockIndex == cursor.blockIndex) {
            templateMax + extraRounds.coerceAtLeast(0)
        } else {
            templateMax
        }
        val minCaption = if (block.kind != BlockKind.WARM_UP && block.targetRoundsMin != max) {
            "MIN ${block.targetRoundsMin}"
        } else {
            null
        }
        block.exercises.mapIndexed { exerciseIndex, _ ->
            val label = if (block.kind == BlockKind.WARM_UP) {
                "W"
            } else {
                working += 1
                working.toString()
            }
            val done = when {
                blockIndex < cursor.blockIndex -> max
                blockIndex > cursor.blockIndex -> 0
                exerciseIndex <= cursor.exerciseIndex -> cursor.roundNumber.coerceIn(0, max)
                else -> (cursor.roundNumber - 1).coerceIn(0, max)
            }
            ExercisePill(
                label = label,
                fraction = "$done/$max",
                selected = blockIndex == cursor.blockIndex && exerciseIndex == cursor.exerciseIndex,
                superset = grouped,
                groupId = blockIndex,
                minCaption = minCaption,
            )
        }
    }
}

fun buildSetRows(
    warmUp: Boolean,
    roundCount: Int,
    activeRound: Int,
    tracksWeight: Boolean,
    seconds: Boolean,
    activeLoad: String,
    activeValue: String,
    holdingSeconds: Int?,
    logs: List<LoggedSetView>,
    previousText: Map<Int, String>,
    previousCopyable: Map<Int, Boolean>,
    activeRpe: String? = null,
    loggedRpe: Map<Int, String> = emptyMap(),
): List<SetRowModel> {
    val count = if (warmUp) 1 else roundCount.coerceAtLeast(1)
    return (1..count).map { round ->
        val active = round == activeRound
        val log = logs.lastOrNull { it.roundNumber == round }
        val load = when {
            active && tracksWeight -> activeLoad.ifBlank { "—" }
            log != null && tracksWeight -> log.weightDisplay?.ifBlank { "—" } ?: "—"
            else -> "—"
        }
        val reps = when {
            active && holdingSeconds != null -> holdingSeconds.toString()
            active -> activeValue.ifBlank { "—" }
            log != null -> {
                val figure = if (seconds) log.durationSeconds else log.reps
                figure?.toString() ?: "—"
            }
            else -> "—"
        }
        SetRowModel(
            label = if (warmUp) "W" else round.toString(),
            roundNumber = round,
            previousText = previousText[round] ?: "—",
            loadText = load,
            repsText = reps,
            complete = log != null,
            active = active,
            copyable = active && previousCopyable[round] == true,
            rpeText = if (active) activeRpe else loggedRpe[round],
        )
    }
}

fun applyRepKey(current: String, key: String, fresh: Boolean): KeypadEdit {
    return when (key) {
        "." -> KeypadEdit(current, fresh)
        "delete" -> if (fresh) KeypadEdit("", false) else KeypadEdit(current.dropLast(1), false)
        "minus" -> KeypadEdit(((current.toIntOrNull() ?: 0) - 1).coerceAtLeast(0).toString(), false)
        "plus" -> KeypadEdit(((current.toIntOrNull() ?: 0) + 1).coerceAtLeast(0).toString(), false)
        else -> {
            if (key.length != 1 || !key[0].isDigit()) return KeypadEdit(current, fresh)
            val next = if (fresh) key else current + key
            if (next.length <= 4 && next.all(Char::isDigit)) KeypadEdit(next, false) else KeypadEdit(current, fresh)
        }
    }
}

fun applyLoadKey(current: String, key: String, fresh: Boolean, step: Float): KeypadEdit {
    fun accept(text: String): KeypadEdit? =
        if (text.isEmpty() || LOAD_PATTERN.matches(text)) KeypadEdit(text, false) else null
    return when (key) {
        "delete" -> accept(if (fresh) "" else current.dropLast(1)) ?: KeypadEdit(current, fresh)
        "minus" -> KeypadEdit(formatWeight(((current.toFloatOrNull() ?: 0f) - step).coerceAtLeast(0f)), false)
        "plus" -> KeypadEdit(formatWeight((current.toFloatOrNull() ?: 0f) + step), false)
        else -> {
            if (key != "." && (key.length != 1 || !key[0].isDigit())) return KeypadEdit(current, fresh)
            val next = if (fresh) key else current + key
            accept(next) ?: KeypadEdit(current, fresh)
        }
    }
}

/** Epley estimates are a chart series, not a summary mark. */
fun formatRecordLine(type: String, value: Float, exerciseName: String, unit: UnitSystem): RecordLine? {
    if (type == "estimated1RM") return null
    if (type == "maxWeight") {
        val display = if (unit == UnitSystem.LB) kilogramsToPounds(value) else value
        val suffix = if (unit == UnitSystem.LB) "lb" else "kg"
        return RecordLine(exerciseName, "${formatWeight(display)} $suffix")
    }
    if (type.startsWith("maxReps_")) {
        return RecordLine(exerciseName, "${formatWeight(value)} reps")
    }
    return null
}
