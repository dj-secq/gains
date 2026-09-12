import re

with open("app/src/main/java/com/example/repsgrams/data/db/AppDatabase.kt", "r") as f:
    text = f.read()

text = text.replace("version = 3,", "version = 4,")

target = "    companion object {"
replacement = """    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE workout_templates ADD COLUMN restDaysAfter INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE workout_templates ADD COLUMN orderIndex INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE workout_templates ADD COLUMN category TEXT NOT NULL DEFAULT 'Custom'")
                
                // Backfill existing templates with order matching their names if possible
                db.execSQL("UPDATE workout_templates SET orderIndex = 0, restDaysAfter = 1 WHERE name LIKE '%A %'")
                db.execSQL("UPDATE workout_templates SET orderIndex = 1, restDaysAfter = 2 WHERE name LIKE '%B %'")
            }
        }"""
text = text.replace(target, replacement)

with open("app/src/main/java/com/example/repsgrams/data/db/AppDatabase.kt", "w") as f:
    f.write(text)
