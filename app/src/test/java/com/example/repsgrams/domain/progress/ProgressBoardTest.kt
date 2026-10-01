package com.example.repsgrams.domain.progress

import com.example.repsgrams.data.datastore.UnitSystem
import com.example.repsgrams.data.db.ExerciseSetHistoryRow
import com.example.repsgrams.data.db.PersonalRecordEntity
import com.example.repsgrams.data.db.RepType
import com.example.repsgrams.domain.session.formatWeight
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProgressBoardTest {
    private val monday = LocalDate.of(2026, 9, 7)
    private val wednesday = LocalDate.of(2026, 9, 9)
    private val friday = LocalDate.of(2026, 9, 11)

    @Test
    fun bestMarksOnlyAStrictlyHeavierLoad() {
        val rows = listOf(
            set(1, monday, 80f, 5),
            set(1, monday, 70f, 8),
            set(2, wednesday, 82.5f, 3),
            set(3, friday, 82.5f, 6),
            set(4, friday.plusDays(2), 60f, 10),
        )
        val series = progressSeries(rows, ProgressMetric.BEST, UnitSystem.KG, tracksWeight = true)
        assertEquals("KG", series.unit)
        assertTrue(series.loadMetrics)
        assertEquals(listOf(80f, 82.5f, 82.5f, 60f), series.points.map { it.value })
        assertEquals(listOf(false, true, false, false), series.points.map { it.record })
        assertEquals("5 × 80 kg", series.points[0].detail)
        assertEquals("3 × 82.5 kg", series.points[1].detail)
        assertEquals(listOf(monday, wednesday, friday, friday.plusDays(2)), series.points.map { it.date })
    }

    @Test
    fun poundsUseTheDisplayLoadForTheSameRecordRule() {
        val rows = listOf(set(1, monday, 80f, 5), set(2, wednesday, 80f, 6))
        val series = progressSeries(rows, ProgressMetric.BEST, UnitSystem.LB, tracksWeight = true)
        val shown = kilogramsToPounds(80f)
        assertEquals("LB", series.unit)
        assertEquals(shown, series.points[0].value, 0.001f)
        assertEquals("5 × ${formatWeight(shown)} lb", series.points[0].detail)
        assertFalse(series.points[0].record)
        assertFalse(series.points[1].record)
    }

    @Test
    fun volumeSumsLoadTimesRepsAndIsNeverRed() {
        val rows = listOf(set(1, monday, 80f, 5), set(1, monday, 70f, 8), set(2, wednesday, 90f, 4))
        val series = progressSeries(rows, ProgressMetric.VOLUME, UnitSystem.KG, tracksWeight = true)
        assertEquals(listOf(960f, 360f), series.points.map { it.value })
        assertTrue(series.points.none { it.record })
        assertEquals("960 kg", series.points[0].detail)
    }

    @Test
    fun estimateUsesRepsFromTwoToTenAndSkipsASingle() {
        val rows = listOf(
            set(1, monday, 100f, 1),
            set(1, monday, 100f, 11),
            set(1, monday, 100f, 10),
            set(1, monday, 80f, 2),
        )
        val best = progressSeries(rows, ProgressMetric.BEST, UnitSystem.KG, tracksWeight = true)
        assertEquals(100f, best.points.single().value, 0.001f)
        val estimate = progressSeries(rows, ProgressMetric.EST, UnitSystem.KG, tracksWeight = true)
        val expected = 100f * (1f + 10f / 30f)
        assertEquals(expected, estimate.points.single().value, 0.001f)
        assertEquals("${formatWeight(expected)} kg", estimate.points.single().detail)
        assertFalse(estimate.points.single().record)
    }

    @Test
    fun secondsPlotDurationAndHideTheLoadChips() {
        val rows = listOf(
            set(1, monday, weight = 20f, reps = null, seconds = 30, repType = RepType.SECONDS.name),
            set(1, monday, weight = 20f, reps = null, seconds = 40, repType = RepType.SECONDS.name),
        )
        val series = progressSeries(rows, ProgressMetric.BEST, UnitSystem.KG, tracksWeight = true)
        assertFalse(series.loadMetrics)
        assertEquals("SEC", series.unit)
        assertEquals(40f, series.points.single().value, 0.001f)
        assertEquals("40s", series.points.single().detail)
        assertFalse(series.points.single().record)
    }

    @Test
    fun repsWithoutALoadStayOffTheKilogramChips() {
        val rows = listOf(set(1, monday, weight = null, reps = 8), set(1, monday, weight = null, reps = 6))
        val series = progressSeries(rows, ProgressMetric.BEST, UnitSystem.KG, tracksWeight = false)
        assertFalse(series.loadMetrics)
        assertEquals("REPS", series.unit)
        assertEquals(8f, series.points.single().value, 0.001f)
    }

    @Test
    fun recordsUsePlainLabelsAndStillReadOldRepKeys() {
        val names = mapOf(2L to "Pull-Ups", 1L to "DB Floor Press")
        val rows = progressRecords(
            listOf(
                record(4, 1, "estimated1RM", 100f, friday),
                record(3, 1, "maxReps_20", 8f, wednesday),
                record(2, 1, "maxReps_20.50", 6f, monday),
                record(1, 1, "maxWeight", 82.5f, friday),
                record(5, 2, "maxWeight", 40f, monday),
            ),
            names,
            UnitSystem.KG,
        )
        assertEquals(
            listOf("DB Floor Press", "DB Floor Press", "DB Floor Press", "DB Floor Press", "Pull-Ups"),
            rows.map { it.exerciseName },
        )
        assertEquals(listOf("Heaviest", "Reps at 20 kg", "Reps at 20.5 kg", "EST", "Heaviest"), rows.map { it.label })
        assertEquals(listOf("82.5 kg", "8", "6", "100 kg", "40 kg"), rows.map { it.value })
        assertEquals(listOf(false, false, false, true, false), rows.map { it.estimate })
        val estimate = progressRecords(listOf(record(4, 1, "estimated1RM", 100f, friday)), names, UnitSystem.KG).single()
        assertEquals("EST", estimate.label)
        assertEquals("100 kg", estimate.value)
        assertTrue(estimate.estimate)
    }

    @Test
    fun inchesConvertBothWaysAndKeepTheSettingsOrder() {
        assertEquals(84f / 2.54f, measurementToDisplay(84f, UnitSystem.LB), 0.001f)
        assertEquals(84f, measurementToDisplay(84f, UnitSystem.KG), 0.001f)
        assertEquals(33f * 2.54f, measurementToCentimeters(33f, UnitSystem.LB), 0.001f)
        assertEquals(listOf("waist", "neck", "hips"), orderedMeasurements(setOf("neck", "hips", "waist")))
        assertEquals("12 Sep", formatProgressDate(LocalDate.of(2026, 9, 12)))
    }

    private fun set(
        sessionId: Long,
        date: LocalDate,
        weight: Float?,
        reps: Int?,
        seconds: Int? = null,
        repType: String? = null,
    ) = ExerciseSetHistoryRow(
        sessionId = sessionId,
        date = date,
        roundNumber = 1,
        reps = reps,
        durationSeconds = seconds,
        weightKg = weight,
        repType = repType,
    )

    private fun record(id: Long, exerciseId: Long, type: String, value: Float, date: LocalDate) = PersonalRecordEntity(
        id = id,
        exerciseId = exerciseId,
        type = type,
        value = value,
        achievedDate = date,
        sourceSetLogId = null,
    )
}
