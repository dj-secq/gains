import re

with open("app/src/main/java/com/example/repsgrams/ui/progress/ProgressViewModel.kt", "r") as f:
    text = f.read()

state_old = """    val bodyweightHistory: List<Pair<LocalDate, Float>> = emptyList(),
    
    val creatineAdherence30d: Float = 0f,"""
state_new = """    val bodyweightHistory: List<Pair<LocalDate, Float>> = emptyList(),
    val bodyMeasurements: Map<String, List<Pair<LocalDate, Float>>> = emptyMap(),
    val trackedMeasurements: Set<String> = emptySet(),
    
    val creatineAdherence30d: Float = 0f,"""
text = text.replace(state_old, state_new)

imports = """import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import com.example.repsgrams.data.db.BodyMeasurementLogEntity
"""
text = text.replace("import kotlinx.coroutines.flow.combine", imports)

stats_old = """    val bwHistory: List<BodyweightLogEntity>,
    val suppLogs: List<SupplementLogEntity>,
    val supplies: List<SupplyInventoryEntity>,
    val settings: CycleSettings
)"""
stats_new = """    val bwHistory: List<BodyweightLogEntity>,
    val suppLogs: List<SupplementLogEntity>,
    val supplies: List<SupplyInventoryEntity>,
    val settings: CycleSettings,
    val measurements: Map<String, List<BodyMeasurementLogEntity>>
)"""
text = text.replace(stats_old, stats_new)


combine_old = """        combine(
            progressRepository.observeStreakInfo(),
            progressRepository.observeBodyweight(),
            progressRepository.observeSupplements(),
            progressRepository.observeSupplies(),
            cycleSettingsRepository.settings
        ) { streak, bw, supps, supplies, settings ->
            UserStats(streak, bw, supps, supplies, settings)
        }"""
combine_new = """        combine(
            progressRepository.observeStreakInfo(),
            progressRepository.observeBodyweight(),
            progressRepository.observeSupplements(),
            progressRepository.observeSupplies(),
            cycleSettingsRepository.settings
        ) { streak, bw, supps, supplies, settings ->
            UserStats(streak, bw, supps, supplies, settings, emptyMap()) // populated in flatMapLatest
        }.flatMapLatest { stats ->
            if (stats.settings.trackedMeasurements.isEmpty()) {
                flowOf(stats)
            } else {
                val flows = stats.settings.trackedMeasurements.map { m ->
                    progressRepository.observeBodyMeasurementsByType(m)
                }
                combine(flows) { lists ->
                    val map = stats.settings.trackedMeasurements.zip(lists).toMap()
                    stats.copy(measurements = map)
                }
            }
        }"""
text = text.replace(combine_old, combine_new)

calc_old = """            bodyweightHistory = stats.bwHistory.map { it.date to it.weightKg },
            creatineAdherence30d = statsCalculator.calculateCreatineAdherence(stats.suppLogs, today, 30),"""
calc_new = """            bodyweightHistory = stats.bwHistory.map { it.date to it.weightKg },
            bodyMeasurements = stats.measurements.mapValues { (_, list) -> list.map { it.date to it.valueCm } },
            trackedMeasurements = stats.settings.trackedMeasurements,
            creatineAdherence30d = statsCalculator.calculateCreatineAdherence(stats.suppLogs, today, 30),"""
text = text.replace(calc_old, calc_new)

method = """
    fun logBodyMeasurement(type: String, valueCm: Float) {
        viewModelScope.launch { progressRepository.logBodyMeasurement(today, type, valueCm) }
    }
"""
text = text.replace("    fun logBodyweight(weightKg: Float) {", method + "    fun logBodyweight(weightKg: Float) {")

with open("app/src/main/java/com/example/repsgrams/ui/progress/ProgressViewModel.kt", "w") as f:
    f.write(text)
