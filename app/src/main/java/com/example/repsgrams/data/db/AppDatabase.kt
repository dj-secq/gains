package com.example.repsgrams.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutTemplateEntity::class,
        TemplateBlockEntity::class,
        TemplateBlockExerciseEntity::class,
        WorkoutSessionEntity::class,
        SetLogEntity::class,
        SupplementLogEntity::class,
        BodyweightLogEntity::class,
        SupplyInventoryEntity::class,
        DatabaseMetadataEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
@TypeConverters(DatabaseConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun exerciseDao(): ExerciseDao
    abstract fun workoutTemplateDao(): WorkoutTemplateDao
    abstract fun templateBlockDao(): TemplateBlockDao
    abstract fun templateBlockExerciseDao(): TemplateBlockExerciseDao
    abstract fun workoutSessionDao(): WorkoutSessionDao
    abstract fun setLogDao(): SetLogDao
    abstract fun supplementLogDao(): SupplementLogDao
    abstract fun bodyweightLogDao(): BodyweightLogDao
    abstract fun supplyInventoryDao(): SupplyInventoryDao
    abstract fun databaseMetadataDao(): DatabaseMetadataDao
}
