package com.example.repsgrams.data.db

/**
 * Statements Room runs for schema 7 → 8. [com.example.repsgrams.data.db.MigrationTest]
 * executes this same list. A fresh install creates `sessionKind` from the entity
 * and does not run these statements; only an existing database is backfilled.
 */
object SessionKindMigration {
    val STATEMENTS: List<String> = listOf(
        "ALTER TABLE workout_sessions ADD COLUMN sessionKind TEXT NOT NULL DEFAULT 'WORKOUT'",
        "UPDATE workout_sessions SET sessionKind = 'REST' WHERE templateId IS NULL AND notes = 'Rest day'",
        "UPDATE workout_sessions SET notes = NULL WHERE sessionKind = 'REST' AND notes = 'Rest day'",
        "DELETE FROM personal_records WHERE sourceSetLogId IS NULL",
    )
}
