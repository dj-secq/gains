package com.example.repsgrams.domain.progress

import com.example.repsgrams.data.db.SupplementLogEntity
import com.example.repsgrams.data.db.SupplyInventoryEntity
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class ProteinEstimate(
    val targetLow: Float,
    val targetHigh: Float,
    val wheyContributionThisWeek: Float,
)

data class SupplyStatus(
    val remaining: Float,
    val estimatedRunOutDate: LocalDate?,
    val isLow: Boolean,
)

class ProgressStatsCalculator {
    fun calculateCreatineAdherence(
        logs: List<SupplementLogEntity>,
        today: LocalDate,
        days: Int,
    ): Float {
        val startDate = today.minusDays(days.toLong())
        val recentLogs = logs.filter { it.date.isAfter(startDate) && !it.date.isAfter(today) }
        if (recentLogs.isEmpty()) return 0f
        
        val takenCount = recentLogs.count { it.creatineTaken }
        return takenCount.toFloat() / days.toFloat()
    }

    fun calculateProteinEstimate(
        bodyweightKg: Float,
        multiplierLow: Float,
        multiplierHigh: Float,
        recentLogs: List<SupplementLogEntity>,
        wheyServingGrams: Float,
    ): ProteinEstimate {
        val targetLow = bodyweightKg * multiplierLow
        val targetHigh = bodyweightKg * multiplierHigh
        val wheyContributionThisWeek = recentLogs.sumOf { it.wheyServings.toDouble() }.toFloat() * wheyServingGrams
        
        return ProteinEstimate(targetLow, targetHigh, wheyContributionThisWeek)
    }

    fun calculateSupplyStatus(
        inventory: SupplyInventoryEntity,
        today: LocalDate,
        lowThreshold: Float = 7f
    ): SupplyStatus {
        val daysPassed = ChronoUnit.DAYS.between(inventory.startDate, today)
        val consumed = inventory.totalServings - inventory.servingsRemaining
        val rate = if (daysPassed > 0) consumed / daysPassed.toFloat() else 0f
        
        val estimatedRunOutDate = if (rate > 0f) {
            today.plusDays((inventory.servingsRemaining / rate).toLong())
        } else {
            null
        }
        
        return SupplyStatus(
            remaining = inventory.servingsRemaining,
            estimatedRunOutDate = estimatedRunOutDate,
            isLow = inventory.servingsRemaining <= lowThreshold
        )
    }
}
