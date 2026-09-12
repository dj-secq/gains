import re
with open("app/src/main/java/com/example/repsgrams/data/db/Daos.kt", "r") as f:
    text = f.read()

target = """@Dao
interface SupplementLogDao {
    @Query("SELECT * FROM supplement_logs WHERE date = :date")
    fun observeForDate(date: LocalDate): Flow<SupplementLogEntity?>

    @Query("SELECT * FROM supplement_logs WHERE date = :date")
    suspend fun getForDate(date: LocalDate): SupplementLogEntity?

    @Query("SELECT * FROM supplement_logs")
    fun observeAll(): Flow<List<SupplementLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: SupplementLogEntity): Long
}"""

replacement = """@Dao
interface SupplementDao {
    @Query("SELECT * FROM supplements")
    fun observeAll(): Flow<List<SupplementEntity>>

    @Query("SELECT * FROM supplements WHERE id = :id")
    suspend fun getById(id: Long): SupplementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(supplement: SupplementEntity): Long

    @Update
    suspend fun update(supplement: SupplementEntity)

    @Delete
    suspend fun delete(supplement: SupplementEntity)
}

@Dao
interface SupplementIntakeLogDao {
    @Query("SELECT * FROM supplement_intake_logs WHERE date = :date")
    fun observeForDate(date: LocalDate): Flow<List<SupplementIntakeLogEntity>>

    @Query("SELECT * FROM supplement_intake_logs WHERE date = :date AND supplementId = :supplementId")
    suspend fun getForDateAndSupplement(date: LocalDate, supplementId: Long): SupplementIntakeLogEntity?

    @Query("SELECT * FROM supplement_intake_logs")
    fun observeAll(): Flow<List<SupplementIntakeLogEntity>>
    
    @Query("SELECT COUNT(*) > 0 FROM supplement_intake_logs WHERE supplementId = :supplementId")
    suspend fun hasHistory(supplementId: Long): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: SupplementIntakeLogEntity): Long
    
    @Update
    suspend fun update(log: SupplementIntakeLogEntity)
    
    @Delete
    suspend fun delete(log: SupplementIntakeLogEntity)
}"""

text = text.replace(target, replacement)
with open("app/src/main/java/com/example/repsgrams/data/db/Daos.kt", "w") as f:
    f.write(text)

with open("app/src/main/java/com/example/repsgrams/data/db/AppDatabase.kt", "r") as f:
    text = f.read()

text = text.replace("abstract fun supplementLogDao(): SupplementLogDao", "abstract fun supplementDao(): SupplementDao\n    abstract fun supplementIntakeLogDao(): SupplementIntakeLogDao")
with open("app/src/main/java/com/example/repsgrams/data/db/AppDatabase.kt", "w") as f:
    f.write(text)

