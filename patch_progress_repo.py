import re

with open("app/src/main/java/com/example/repsgrams/data/repository/ProgressRepository.kt", "r") as f:
    text = f.read()

method = """
    suspend fun logBodyMeasurement(date: LocalDate, type: String, valueCm: Float) {
        database.bodyMeasurementLogDao().insert(
            com.example.repsgrams.data.db.BodyMeasurementLogEntity(
                date = date,
                type = type,
                valueCm = valueCm
            )
        )
    }

    fun observeBodyMeasurementsByType(type: String): Flow<List<com.example.repsgrams.data.db.BodyMeasurementLogEntity>> {
        return database.bodyMeasurementLogDao().observeByType(type)
    }
"""
text = text.replace("    suspend fun logBodyweight(date: LocalDate, weightKg: Float) {", method + "    suspend fun logBodyweight(date: LocalDate, weightKg: Float) {")

with open("app/src/main/java/com/example/repsgrams/data/repository/ProgressRepository.kt", "w") as f:
    f.write(text)
