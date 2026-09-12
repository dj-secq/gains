package com.example.repsgrams.data.repository

import androidx.room.withTransaction
import com.example.repsgrams.data.db.AppDatabase
import com.example.repsgrams.data.db.SupplementEntity
import com.example.repsgrams.data.db.SupplementIntakeLogEntity
import com.example.repsgrams.data.db.SupplyInventoryEntity
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface SupplementRepository {
    fun observeAllSupplements(): Flow<List<SupplementEntity>>
    fun observeIntakesForDate(date: LocalDate): Flow<List<SupplementIntakeLogEntity>>
    fun observeIntakesInRange(start: LocalDate, end: LocalDate): Flow<List<SupplementIntakeLogEntity>>
    suspend fun setSupplementTaken(date: LocalDate, supplement: SupplementEntity, taken: Boolean, amount: Float? = null)
    
    suspend fun addSupplement(supplement: SupplementEntity): Long
    suspend fun updateSupplement(supplement: SupplementEntity)
    suspend fun deleteSupplement(supplement: SupplementEntity)
    
    fun observeInventory(): Flow<List<SupplyInventoryEntity>>
    suspend fun restock(supplementId: Long, newTotalServings: Int)
}

class DefaultSupplementRepository(
    private val database: AppDatabase,
    private val clock: Clock,
) : SupplementRepository {
    override fun observeAllSupplements() = database.supplementDao().observeAll()
    override fun observeIntakesForDate(date: LocalDate) = database.supplementIntakeLogDao().observeForDate(date)
    override fun observeIntakesInRange(start: LocalDate, end: LocalDate) = database.supplementIntakeLogDao().observeInRange(start, end)
    
    override suspend fun setSupplementTaken(date: LocalDate, supplement: SupplementEntity, taken: Boolean, amount: Float?) {
        database.withTransaction {
            val dao = database.supplementIntakeLogDao()
            val existing = dao.getForDateAndSupplement(date, supplement.id)
            val wasTaken = existing?.taken ?: false
            if (wasTaken != taken) {
                val actualAmount = amount ?: supplement.doseAmount
                if (existing != null) {
                    dao.update(existing.copy(taken = taken, actualAmount = if (taken) actualAmount else 0f))
                } else {
                    dao.upsert(SupplementIntakeLogEntity(supplementId = supplement.id, date = date, taken = taken, actualAmount = if (taken) actualAmount else 0f))
                }
                
                // Adjust inventory
                val delta = if (taken) -1f else 1f
                val invDao = database.supplyInventoryDao()
                val inv = invDao.get(supplement.id)
                if (inv != null) {
                    invDao.update(inv.copy(servingsRemaining = inv.servingsRemaining + delta))
                }
            }
        }
    }
    
    override suspend fun addSupplement(supplement: SupplementEntity): Long {
        return database.withTransaction {
            val id = database.supplementDao().insert(supplement)
            database.supplyInventoryDao().insert(SupplyInventoryEntity(supplementId = id, totalServings = supplement.containerSize, servingsRemaining = supplement.containerSize.toFloat(), startDate = LocalDate.now(clock)))
            id
        }
    }
    
    override suspend fun updateSupplement(supplement: SupplementEntity) {
        database.supplementDao().update(supplement)
    }
    
    override suspend fun deleteSupplement(supplement: SupplementEntity) {
        database.supplementDao().delete(supplement)
    }
    
    override fun observeInventory() = database.supplyInventoryDao().observeAll()
    
    override suspend fun restock(supplementId: Long, newTotalServings: Int) {
        val dao = database.supplyInventoryDao()
        val inv = dao.get(supplementId)
        if (inv != null) {
            dao.update(inv.copy(totalServings = newTotalServings, servingsRemaining = newTotalServings.toFloat(), startDate = LocalDate.now(clock)))
        }
    }
}