package com.example.repsgrams.data.datastore

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class SessionProgressStoreTest {
    private lateinit var store: SessionProgressStore

    @Before
    fun setUp() = runTest {
        store = SessionProgressStore(ApplicationProvider.getApplicationContext<Context>())
        store.clear()
    }

    @After
    fun tearDown() = runTest { store.clear() }

    @Test
    fun persistsAndClearsActiveCursorAndRestDeadline() = runTest {
        val expected = SessionProgress(42, 3, 1, 2, 1_800_000L)
        store.save(expected)

        assertEquals(expected, store.progress.first())

        store.clear()
        assertNull(store.progress.first())
    }
}
