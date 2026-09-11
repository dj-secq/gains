package com.example.repsgrams.data.repository

import androidx.room.withTransaction
import com.example.repsgrams.data.db.AppDatabase
import com.example.repsgrams.data.db.SupplementLogEntity
import com.example.repsgrams.data.db.SupplyType
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface SupplementRepository {
    fun observeToday(): Flow<SupplementLogEntity?>
    fun observeForDate(date: LocalDate): Flow<SupplementLogEntity?>
    suspend fun setCreatineTaken(date: LocalDate, taken: Boolean)
    suspend fun setWheyTaken(date: LocalDate, taken: Boolean)
}

class DefaultSupplementRepository(
    private val database: AppDatabase,
    private val clock: Clock,
    private val onWheyTaken: (LocalDate) -> Unit = {},
) : SupplementRepository {
    private val dao = database.supplementLogDao()

    override fun observeToday(): Flow<SupplementLogEntity?> = observeForDate(LocalDate.now(clock))

    override fun observeForDate(date: LocalDate): Flow<SupplementLogEntity?> = dao.observeForDate(date)

    override suspend fun setCreatineTaken(date: LocalDate, taken: Boolean) {
        database.withTransaction {
            val current = dao.getForDate(date) ?: SupplementLogEntity(date = date)
            val wasTaken = current.creatineTaken
            if (wasTaken != taken) {
                dao.upsert(current.copy(creatineTaken = taken, creatineGrams = if (taken) 5f else 0f))
                val delta = if (taken) -1f else 1f
                database.supplyInventoryDao().adjustRemaining(SupplyType.CREATINE, delta)
            }
        }
    }

    override suspend fun setWheyTaken(date: LocalDate, taken: Boolean) {
        var stateChanged = false
        database.withTransaction {
            val current = dao.getForDate(date) ?: SupplementLogEntity(date = date)
            val wasTaken = current.wheyTaken
            if (wasTaken != taken) {
                stateChanged = true
                dao.upsert(current.copy(wheyTaken = taken, wheyServings = if (taken) 1f else 0f))
                val delta = if (taken) -1f else 1f
                database.supplyInventoryDao().adjustRemaining(SupplyType.WHEY, delta)
            }
        }
        if (stateChanged && taken) onWheyTaken(date)
    }
}
