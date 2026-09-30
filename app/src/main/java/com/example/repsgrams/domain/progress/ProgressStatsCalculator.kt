package com.example.repsgrams.domain.progress

import com.example.repsgrams.data.db.SupplyInventoryEntity
import java.time.LocalDate

data class SupplyStatus(
    val isLow: Boolean,
    val estimatedRunOutDate: LocalDate?,
    val servingsRemaining: Float,
)

class ProgressStatsCalculator {
    fun calculateSupplyStatus(
        inventory: SupplyInventoryEntity,
        @Suppress("UNUSED_PARAMETER") today: LocalDate,
        lowThreshold: Float = 5f,
    ): SupplyStatus {
        return SupplyStatus(
            isLow = inventory.servingsRemaining <= lowThreshold,
            estimatedRunOutDate = null,
            servingsRemaining = inventory.servingsRemaining,
        )
    }
}
