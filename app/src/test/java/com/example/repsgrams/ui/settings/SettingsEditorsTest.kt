package com.example.repsgrams.ui.settings

import com.example.repsgrams.data.datastore.DEFAULT_PLATES_KG
import com.example.repsgrams.data.datastore.DEFAULT_PLATES_LB
import com.example.repsgrams.data.datastore.formatPlateList
import com.example.repsgrams.data.datastore.parsePlateList
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsEditorsTest {
    @Test
    fun plateListsKeepOrderAndDropBlankTokens() {
        assertEquals(listOf(25f, 20f, 15f, 10f, 5f, 2.5f, 1.25f), parsePlateList(DEFAULT_PLATES_KG))
        assertEquals(listOf(45f, 35f, 25f, 10f, 5f, 2.5f), parsePlateList(DEFAULT_PLATES_LB))
        assertEquals(listOf(20f, 2.5f), parsePlateList(" 20, nope, 2.50 "))
        assertEquals("20,2.5", formatPlateList(listOf(20f, 2.5f)))
    }

    @Test
    fun weekdayChipsStayThreeLetterCodes() {
        assertEquals(setOf("MON", "WED"), parseWeekdays("MONDAY,WEDNESDAY"))
        assertEquals(setOf("MON", "WED"), parseWeekdays("mon, wed"))
        assertEquals("MON,WED,FRI", formatWeekdays(setOf("FRI", "MON", "WED")))
        assertEquals("", formatWeekdays(emptySet()))
    }
}
