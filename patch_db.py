import re

with open("app/src/main/java/com/example/repsgrams/data/db/AppDatabase.kt", "r") as f:
    text = f.read()

text = text.replace("version = 1,", "version = 2,")
text = text.replace("import androidx.room.TypeConverters", "import androidx.room.TypeConverters\nimport androidx.room.migration.Migration\nimport androidx.sqlite.db.SupportSQLiteDatabase")

migration_code = """
    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // ExerciseEntity additions
                db.execSQL("ALTER TABLE exercises ADD COLUMN muscleGroup TEXT NOT NULL DEFAULT 'Uncategorized'")
                db.execSQL("ALTER TABLE exercises ADD COLUMN imageAssetName TEXT")
                db.execSQL("ALTER TABLE exercises ADD COLUMN isCustom INTEGER NOT NULL DEFAULT 0")

                // WorkoutSessionEntity additions
                db.execSQL("ALTER TABLE workout_sessions ADD COLUMN notes TEXT")

                // SetLogEntity additions
                db.execSQL("ALTER TABLE set_logs ADD COLUMN rpeTag TEXT")
                db.execSQL("ALTER TABLE set_logs ADD COLUMN substitutedFrom INTEGER")
                // Foreign key constraint on substitutedFrom (SQLite ALTER TABLE doesn't support adding FKs directly, 
                // but Room doesn't strictly enforce SQLite FK pragmas after the fact unless you recreate the table. 
                // For a simple migration we'll just add the column. To be perfectly compliant with Room schema export, 
                // we technically need to recreate the table, but adding the column often works if FKs aren't strictly checked by SQLite pragma).
                // Actually, Room validation WILL fail if the table schema doesn't perfectly match the generated hash (which includes FKs).
                // It's safest to recreate the set_logs table if we added a FK.
                
                // Let's do the recreate dance for set_logs
                db.execSQL(\"""
                    CREATE TABLE set_logs_new (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        sessionId INTEGER NOT NULL,
                        exerciseId INTEGER NOT NULL,
                        roundNumber INTEGER NOT NULL,
                        reps INTEGER,
                        durationSeconds INTEGER,
                        weightKg REAL,
                        loggedAt INTEGER NOT NULL,
                        rpeTag TEXT,
                        substitutedFrom INTEGER,
                        FOREIGN KEY(sessionId) REFERENCES workout_sessions(id) ON UPDATE NO ACTION ON DELETE CASCADE,
                        FOREIGN KEY(exerciseId) REFERENCES exercises(id) ON UPDATE NO ACTION ON DELETE RESTRICT,
                        FOREIGN KEY(substitutedFrom) REFERENCES exercises(id) ON UPDATE NO ACTION ON DELETE SET NULL
                    )
                \""")
                db.execSQL("INSERT INTO set_logs_new (id, sessionId, exerciseId, roundNumber, reps, durationSeconds, weightKg, loggedAt) SELECT id, sessionId, exerciseId, roundNumber, reps, durationSeconds, weightKg, loggedAt FROM set_logs")
                db.execSQL("DROP TABLE set_logs")
                db.execSQL("ALTER TABLE set_logs_new RENAME TO set_logs")
                db.execSQL("CREATE INDEX index_set_logs_sessionId ON set_logs(sessionId)")
                db.execSQL("CREATE INDEX index_set_logs_exerciseId ON set_logs(exerciseId)")
                db.execSQL("CREATE INDEX index_set_logs_substitutedFrom ON set_logs(substitutedFrom)")
            }
        }
    }
"""

text = text.replace("abstract fun databaseMetadataDao(): DatabaseMetadataDao\n}", "abstract fun databaseMetadataDao(): DatabaseMetadataDao\n" + migration_code + "}")

with open("app/src/main/java/com/example/repsgrams/data/db/AppDatabase.kt", "w") as f:
    f.write(text)
