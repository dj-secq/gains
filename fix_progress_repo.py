import re

with open("app/src/main/java/com/example/repsgrams/data/repository/ProgressRepository.kt", "r") as f:
    text = f.read()

interface_method = """
    suspend fun logBodyMeasurement(date: LocalDate, type: String, valueCm: Float)
    fun observeBodyMeasurementsByType(type: String): Flow<List<com.example.repsgrams.data.db.BodyMeasurementLogEntity>>
"""
text = text.replace("    suspend fun logBodyweight(date: LocalDate, weightKg: Float)", interface_method + "    suspend fun logBodyweight(date: LocalDate, weightKg: Float)")

impl_method = """
    override suspend fun logBodyMeasurement(date: LocalDate, type: String, valueCm: Float) {
        database.bodyMeasurementLogDao().insert(
            com.example.repsgrams.data.db.BodyMeasurementLogEntity(
                date = date,
                type = type,
                valueCm = valueCm
            )
        )
    }

    override fun observeBodyMeasurementsByType(type: String): Flow<List<com.example.repsgrams.data.db.BodyMeasurementLogEntity>> {
        return database.bodyMeasurementLogDao().observeByType(type)
    }
"""
text = text.replace("    override suspend fun logBodyweight(date: LocalDate, weightKg: Float) {", impl_method + "    override suspend fun logBodyweight(date: LocalDate, weightKg: Float) {")

with open("app/src/main/java/com/example/repsgrams/data/repository/ProgressRepository.kt", "w") as f:
    f.write(text)
