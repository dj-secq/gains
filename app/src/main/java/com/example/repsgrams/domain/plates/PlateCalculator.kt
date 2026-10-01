package com.example.repsgrams.domain.plates

import java.util.Locale
import kotlin.math.roundToInt

data class PlateBreakdown(
    val perSide: List<Float>,
    val underBar: Boolean,
    val makeable: Float?,
    val difference: Float?,
)

private const val CENT = 100

private fun cents(value: Float): Int = (value * CENT.toFloat()).roundToInt()

private fun fromCents(value: Int): Float = value / CENT.toFloat()

/**
 * One bar, unlimited pairs, heaviest denomination first.
 * A remainder stays on the lower side: makeable is the target minus both sides' leftover.
 */
fun calculatePlates(target: Float, bar: Float, plates: List<Float>): PlateBreakdown {
    val targetCents = cents(target)
    val barCents = cents(bar)
    if (targetCents < barCents) {
        return PlateBreakdown(emptyList(), underBar = true, makeable = null, difference = null)
    }
    val denominations = plates.map(::cents).filter { it > 0 }.sortedDescending()
    var remain = targetCents - barCents
    val odd = remain % 2
    var side = remain / 2
    val chosen = mutableListOf<Int>()
    for (plate in denominations) {
        while (side >= plate) {
            chosen += plate
            side -= plate
        }
    }
    val remainderCents = side * 2 + odd
    return if (remainderCents > 0) {
        PlateBreakdown(
            perSide = chosen.map(::fromCents),
            underBar = false,
            makeable = fromCents(targetCents - remainderCents),
            difference = fromCents(remainderCents),
        )
    } else {
        PlateBreakdown(chosen.map(::fromCents), underBar = false, makeable = null, difference = null)
    }
}

fun formatPlateNumber(value: Float): String {
    val text = String.format(Locale.US, "%.2f", value).trimEnd('0').trimEnd('.')
    return text.ifEmpty { "0" }
}

fun formatPlateStack(perSide: List<Float>): String =
    if (perSide.isEmpty()) "Bar" else perSide.joinToString("+") { formatPlateNumber(it) }

fun plateCaption(breakdown: PlateBreakdown, unitWord: String): String = when {
    breakdown.underBar -> "Under the bar"
    breakdown.makeable != null -> "${formatPlateStack(breakdown.perSide)} · ${formatPlateNumber(breakdown.makeable)} $unitWord"
    else -> formatPlateStack(breakdown.perSide)
}
