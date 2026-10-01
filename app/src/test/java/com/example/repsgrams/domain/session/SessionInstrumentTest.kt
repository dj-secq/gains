package com.example.repsgrams.domain.session

import com.example.repsgrams.data.datastore.UnitSystem
import com.example.repsgrams.data.db.BlockKind
import com.example.repsgrams.data.db.RepType
import com.example.repsgrams.domain.progress.kilogramsToPounds
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionInstrumentTest {
    @Test
    fun cueKeepsArmCirclesAndDropsBlankNotes() {
        assertEquals("10 forward, 10 backward", cueText("10 forward, 10 backward"))
        assertNull(cueText("  "))
        assertNull(cueText(null))
    }

    @Test
    fun elapsedAndRestClocks() {
        assertEquals("0:00", formatSessionElapsed(0))
        assertEquals("0:00", formatSessionElapsed(-5))
        assertEquals("1:05", formatSessionElapsed(65))
        assertEquals("1:00:00", formatSessionElapsed(3_600))
        assertEquals("1:30", formatRestClock(90, 0))
        assertEquals("0:00", formatRestClock(0, 0))
        assertEquals("0:00", formatRestClock(0, null))
        assertEquals("+0:01", formatRestClock(0, 1))
    }

    @Test
    fun plannedDurationCaptionStartsAtTheCap() {
        assertFalse(durationPassed(44 * 60 + 59, 45))
        assertTrue(durationPassed(45 * 60, 45))
        assertFalse(durationPassed(90 * 60, 0))
        assertEquals("Planned duration has passed", PLANNED_DURATION_PASSED)
    }

    @Test
    fun previousColumnFormats() {
        assertEquals("80 × 8", formatPrevious("80", "8", seconds = false))
        assertEquals("8", formatPrevious(null, "8", seconds = false))
        assertEquals("40s", formatPrevious(null, "40", seconds = true))
        assertEquals("80 × 40s", formatPrevious("80", "40", seconds = true))
        assertEquals("—", formatPrevious(null, null, seconds = false))
        assertEquals("—", formatPrevious("  ", "", seconds = false))
    }

    @Test
    fun exerciseStripUsesWorkingIndexNotDayLabels() {
        val pills = exercisePills(
            blocks = listOf(
                block(BlockKind.WARM_UP, 1, exercise(1, "Arm Circles")),
                block(BlockKind.STANDARD, 5, exercise(2, "Pull-Ups")),
                block(BlockKind.STANDARD, 3, exercise(3, "Rows")),
            ),
            cursor = SessionCursor(blockIndex = 1, exerciseIndex = 0, roundNumber = 2),
        )
        assertEquals(listOf("W", "1", "2"), pills.map { it.label })
        assertEquals(listOf("1/1", "2/5", "0/3"), pills.map { it.fraction })
        assertEquals(listOf(false, true, false), pills.map { it.selected })
        assertTrue(pills.none { it.label == "A" || it.label == "B" || it.label == "C" })
    }

    @Test
    fun laterExerciseInTheBlockUsesThePreviousRound() {
        val pills = exercisePills(
            blocks = listOf(
                block(
                    BlockKind.SUPERSET,
                    5,
                    exercise(1, "Pull-Ups"),
                    exercise(2, "DB Floor Press"),
                ),
            ),
            cursor = SessionCursor(blockIndex = 0, exerciseIndex = 0, roundNumber = 2),
        )
        assertEquals(listOf("1", "2"), pills.map { it.label })
        assertEquals(listOf("2/5", "1/5"), pills.map { it.fraction })
        assertTrue(pills.all { it.superset && it.groupId == 0 })
        assertTrue(pills[0].selected)
        assertFalse(pills[1].selected)
    }

    @Test
    fun earlierExerciseShowsTheCurrentRoundAndPastBlocksAreFull() {
        val pills = exercisePills(
            blocks = listOf(
                block(BlockKind.STANDARD, 4, exercise(1, "Squat")),
                block(BlockKind.SUPERSET, 5, exercise(2, "Row"), exercise(3, "Press")),
            ),
            cursor = SessionCursor(blockIndex = 1, exerciseIndex = 1, roundNumber = 2),
        )
        assertEquals("4/4", pills[0].fraction)
        assertEquals("2/5", pills[1].fraction)
        assertEquals("2/5", pills[2].fraction)
        assertTrue(pills[2].selected)
    }

    @Test
    fun warmUpExercisesShareWAndStayOutOfTheWorkingIndex() {
        val pills = exercisePills(
            blocks = listOf(
                block(BlockKind.WARM_UP, 3, exercise(1, "Arm Circles"), exercise(2, "Band Pull")),
                block(BlockKind.STANDARD, 3, exercise(3, "Pull-Ups")),
            ),
            cursor = SessionCursor(0, 0, 1),
        )
        assertEquals(listOf("W", "W", "1"), pills.map { it.label })
        assertEquals(listOf("1/1", "0/1", "0/3"), pills.map { it.fraction })
    }

    @Test
    fun repKeysReplaceThenAppendAndIgnoreTheDecimal() {
        assertEquals(KeypadEdit("8", false), applyRepKey("15", "8", fresh = true))
        assertEquals(KeypadEdit("80", false), applyRepKey("8", "0", fresh = false))
        assertEquals(KeypadEdit("1234", false), applyRepKey("1234", "5", fresh = false))
        assertEquals(KeypadEdit("15", true), applyRepKey("15", ".", fresh = true))
        assertEquals(KeypadEdit("", false), applyRepKey("15", "delete", fresh = true))
        assertEquals(KeypadEdit("1", false), applyRepKey("15", "delete", fresh = false))
        assertEquals(KeypadEdit("0", false), applyRepKey("0", "minus", fresh = false))
        assertEquals(KeypadEdit("9", false), applyRepKey("8", "plus", fresh = false))
        assertEquals(KeypadEdit("16", false), applyRepKey("15", "plus", fresh = true))
    }

    @Test
    fun loadKeysUseTheDisplayIncrement() {
        assertEquals(2.5f, loadStep(UnitSystem.KG))
        assertEquals(5f, loadStep(UnitSystem.LB))
        assertEquals(KeypadEdit("1", false), applyLoadKey("20", "1", fresh = true, step = 2.5f))
        assertEquals(KeypadEdit("1.", false), applyLoadKey("1", ".", fresh = false, step = 2.5f))
        assertEquals(KeypadEdit("1.5", false), applyLoadKey("1.", "5", fresh = false, step = 2.5f))
        assertEquals(KeypadEdit("1.25", false), applyLoadKey("1.25", "9", fresh = false, step = 2.5f))
        assertEquals(KeypadEdit("82.5", false), applyLoadKey("80", "plus", fresh = true, step = 2.5f))
        assertEquals(KeypadEdit("105", false), applyLoadKey("100", "plus", fresh = false, step = 5f))
        assertEquals(KeypadEdit("0", false), applyLoadKey("2.5", "minus", fresh = false, step = 2.5f))
        assertEquals(KeypadEdit("", false), applyLoadKey("20", "delete", fresh = true, step = 2.5f))
        assertEquals(KeypadEdit("12.", false), applyLoadKey("12.3", "delete", fresh = false, step = 2.5f))
    }

    @Test
    fun setTableMarksLoggedRoundsAndTheActivePreviousCell() {
        val rows = buildSetRows(
            warmUp = false,
            roundCount = 2,
            activeRound = 2,
            tracksWeight = true,
            seconds = false,
            activeLoad = "80",
            activeValue = "6",
            holdingSeconds = null,
            logs = listOf(LoggedSetView(1, reps = 8, durationSeconds = null, weightDisplay = "80")),
            previousText = mapOf(1 to "80 × 8", 2 to "80 × 6"),
            previousCopyable = mapOf(1 to true, 2 to true),
        )
        assertEquals("1", rows[0].label)
        assertTrue(rows[0].complete)
        assertFalse(rows[0].copyable)
        assertEquals("80", rows[0].loadText)
        assertEquals("8", rows[0].repsText)
        assertTrue(rows[1].active)
        assertTrue(rows[1].copyable)
        assertEquals("80", rows[1].loadText)
        assertEquals("6", rows[1].repsText)
        assertFalse(rows[1].complete)
    }

    @Test
    fun warmUpIsOneWRowAndAHoldReplacesTheActiveNumeral() {
        val rows = buildSetRows(
            warmUp = true,
            roundCount = 4,
            activeRound = 1,
            tracksWeight = false,
            seconds = true,
            activeLoad = "",
            activeValue = "20",
            holdingSeconds = 12,
            logs = emptyList(),
            previousText = emptyMap(),
            previousCopyable = emptyMap(),
        )
        assertEquals(1, rows.size)
        assertEquals("W", rows[0].label)
        assertEquals("—", rows[0].previousText)
        assertEquals("—", rows[0].loadText)
        assertEquals("12", rows[0].repsText)
        assertFalse(rows[0].copyable)
    }

    @Test
    fun summaryRecordsSkipTheEpleyEstimate() {
        assertNull(formatRecordLine("estimated1RM", 100f, "Squat", UnitSystem.KG))
        assertEquals(RecordLine("Squat", "80 kg"), formatRecordLine("maxWeight", 80f, "Squat", UnitSystem.KG))
        val pounds = formatWeight(kilogramsToPounds(80f))
        assertEquals(RecordLine("Squat", "$pounds lb"), formatRecordLine("maxWeight", 80f, "Squat", UnitSystem.LB))
        assertEquals(RecordLine("Pull-Ups", "8 reps"), formatRecordLine("maxReps_80.00", 8f, "Pull-Ups", UnitSystem.KG))
        assertEquals("Next · DB Floor Press", nextExerciseCaption("DB Floor Press"))
    }

    private fun exercise(id: Long, name: String) = WorkoutExercise(
        id = id,
        name = name,
        imageAssetName = null,
        notes = if (name == "Arm Circles") "10 forward, 10 backward" else null,
        tracksWeight = true,
        targetValueLow = 6,
        targetValueHigh = 10,
        repType = RepType.REPS,
        perSide = false,
    )

    private fun block(kind: BlockKind, rounds: Int, vararg exercises: WorkoutExercise) = WorkoutBlock(
        id = exercises.first().id,
        label = kind.name,
        kind = kind,
        targetRoundsMin = 1,
        targetRoundsMax = rounds,
        restSecondsBetweenRounds = 90,
        restSecondsAfterBlock = 0,
        isOptional = false,
        exercises = exercises.toList(),
    )
}
