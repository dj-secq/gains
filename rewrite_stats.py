with open("app/src/main/java/com/example/repsgrams/domain/progress/ProgressStatsCalculator.kt", "w") as f:
    f.write("""package com.example.repsgrams.domain.progress

import com.example.repsgrams.data.db.SupplyInventoryEntity
import java.time.LocalDate

data class SupplyStatus(
    val isLow: Boolean,
    val estimatedRunOutDate: LocalDate?
)

class ProgressStatsCalculator {
    fun calculateSupplyStatus(inventory: SupplyInventoryEntity, today: LocalDate, lowThreshold: Float = 5f): SupplyStatus {
        return SupplyStatus(false, null)
    }
}
""")
