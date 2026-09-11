import re
with open("app/src/main/java/com/example/repsgrams/data/datastore/CycleSettingsRepository.kt", "r") as f:
    text = f.read()

# CycleSettings data class
text = text.replace("val postWorkoutWheyDelayMinutes: Int,\n)", "val postWorkoutWheyDelayMinutes: Int,\n    val trackedMeasurements: Set<String> = emptySet(),\n)")
text = text.replace("suspend fun setPostWorkoutWheyDelayMinutes(minutes: Int)", "suspend fun setPostWorkoutWheyDelayMinutes(minutes: Int)\n    suspend fun setTrackedMeasurements(measurements: Set<String>)")
text = text.replace("private val POST_WORKOUT_WHEY_DELAY_MINUTES = intPreferencesKey(\"post_workout_whey_delay_minutes\")", "private val POST_WORKOUT_WHEY_DELAY_MINUTES = intPreferencesKey(\"post_workout_whey_delay_minutes\")\n        val TRACKED_MEASUREMENTS = androidx.datastore.preferences.core.stringSetPreferencesKey(\"tracked_measurements\")")
text = text.replace("postWorkoutWheyDelayMinutes = prefs[POST_WORKOUT_WHEY_DELAY_MINUTES] ?: 25,\n            )", "postWorkoutWheyDelayMinutes = prefs[POST_WORKOUT_WHEY_DELAY_MINUTES] ?: 25,\n                trackedMeasurements = prefs[PreferencesKeys.TRACKED_MEASUREMENTS] ?: emptySet(),\n            )")
text = text.replace("override suspend fun setPostWorkoutWheyDelayMinutes(minutes: Int) = update(POST_WORKOUT_WHEY_DELAY_MINUTES, minutes)", "override suspend fun setPostWorkoutWheyDelayMinutes(minutes: Int) = update(POST_WORKOUT_WHEY_DELAY_MINUTES, minutes)\n    override suspend fun setTrackedMeasurements(measurements: Set<String>) { context.cycleSettingsDataStore.edit { it[PreferencesKeys.TRACKED_MEASUREMENTS] = measurements } }")

with open("app/src/main/java/com/example/repsgrams/data/datastore/CycleSettingsRepository.kt", "w") as f:
    f.write(text)
