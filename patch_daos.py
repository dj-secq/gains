import re

with open("app/src/main/java/com/example/repsgrams/data/db/Daos.kt", "r") as f:
    text = f.read()

new_daos = """
@Dao
interface PersonalRecordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: PersonalRecordEntity): Long

    @Query("SELECT * FROM personal_records WHERE exerciseId = :exerciseId AND type = :type ORDER BY value DESC, achievedDate DESC LIMIT 1")
    suspend fun getLatestRecord(exerciseId: Long, type: String): PersonalRecordEntity?

    @Query("SELECT * FROM personal_records ORDER BY achievedDate DESC")
    fun observeAll(): Flow<List<PersonalRecordEntity>>
    
    @Query("SELECT * FROM personal_records WHERE type = :type ORDER BY achievedDate ASC")
    fun observeByType(type: String): Flow<List<PersonalRecordEntity>>
}

@Dao
interface BodyMeasurementLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: BodyMeasurementLogEntity): Long

    @Query("SELECT * FROM body_measurements WHERE type = :type ORDER BY date ASC")
    fun observeByType(type: String): Flow<List<BodyMeasurementLogEntity>>

    @Query("SELECT DISTINCT type FROM body_measurements ORDER BY type ASC")
    fun observeTypes(): Flow<List<String>>
}

@Dao
interface AchievementDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(achievement: AchievementEntity): Long

    @Query("SELECT * FROM achievements ORDER BY unlockedDate DESC")
    fun observeAll(): Flow<List<AchievementEntity>>
}
"""

text = text.replace("@Dao\ninterface DatabaseMetadataDao {", new_daos + "\n@Dao\ninterface DatabaseMetadataDao {")

with open("app/src/main/java/com/example/repsgrams/data/db/Daos.kt", "w") as f:
    f.write(text)

