import re

with open("app/src/main/java/com/example/repsgrams/data/repository/ProgressRepository.kt", "r") as f:
    text = f.read()

target = """    override fun observeAllExercises(): Flow<List<ExerciseEntity>> {
        return database.exerciseDao().observeAll()
    }"""
    
replacement = """    override fun observeAllExercises(): Flow<List<ExerciseEntity>> {
        return database.exerciseDao().observeAll()
    }

    override fun observePersonalRecords(exerciseId: Long): Flow<List<com.example.repsgrams.data.db.PersonalRecordEntity>> {
        return database.personalRecordDao().observeAll().map { list -> list.filter { it.exerciseId == exerciseId } }
    }

    override fun observeBodyMeasurements(type: String): Flow<List<com.example.repsgrams.data.db.BodyMeasurementLogEntity>> {
        return database.bodyMeasurementLogDao().observeByType(type)
    }

    override suspend fun logBodyMeasurement(type: String, date: java.time.LocalDate, valueCm: Float) {
        database.bodyMeasurementLogDao().insert(com.example.repsgrams.data.db.BodyMeasurementLogEntity(date = date, type = type, valueCm = valueCm))
    }

    override fun observeAchievements(): Flow<List<com.example.repsgrams.data.db.AchievementEntity>> {
        return database.achievementDao().observeAll()
    }"""

text = text.replace(target, replacement)

with open("app/src/main/java/com/example/repsgrams/data/repository/ProgressRepository.kt", "w") as f:
    f.write(text)

