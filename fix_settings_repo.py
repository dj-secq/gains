import re

with open("app/src/main/java/com/example/repsgrams/data/datastore/CycleSettingsRepository.kt", "r") as f:
    text = f.read()

settings_old = """    val restTimerSound: String,
    val restTimerVibrationEnabled: Boolean,
    val restTimerAutoAdvance: Boolean,
)"""
settings_new = """    val restTimerSound: String,
    val restTimerVibrationEnabled: Boolean,
    val restTimerAutoAdvance: Boolean,
    val trackedMeasurements: Set<String> = emptySet(),
)"""
text = text.replace(settings_old, settings_new)

pref_old = """    private val REST_TIMER_AUTO_ADVANCE = booleanPreferencesKey("rest_timer_auto_advance")
}"""
pref_new = """    private val REST_TIMER_AUTO_ADVANCE = booleanPreferencesKey("rest_timer_auto_advance")
    private val TRACKED_MEASUREMENTS_KEY = stringSetPreferencesKey("tracked_measurements")
}"""
text = text.replace(pref_old, pref_new)

map_old = """                restTimerVibrationEnabled = prefs[PreferencesKeys.REST_TIMER_VIBRATION] ?: true,
                restTimerAutoAdvance = prefs[PreferencesKeys.REST_TIMER_AUTO_ADVANCE] ?: false,
            )
        }"""
map_new = """                restTimerVibrationEnabled = prefs[PreferencesKeys.REST_TIMER_VIBRATION] ?: true,
                restTimerAutoAdvance = prefs[PreferencesKeys.REST_TIMER_AUTO_ADVANCE] ?: false,
                trackedMeasurements = prefs[PreferencesKeys.TRACKED_MEASUREMENTS_KEY] ?: emptySet(),
            )
        }"""
text = text.replace(map_old, map_new)

set_methods = """
    override suspend fun setTrackedMeasurements(measurements: Set<String>) {
        dataStore.edit { it[PreferencesKeys.TRACKED_MEASUREMENTS_KEY] = measurements }
    }
"""
text = text.replace("    override suspend fun setRestTimerAutoAdvance(enabled: Boolean) {", set_methods + "    override suspend fun setRestTimerAutoAdvance(enabled: Boolean) {")

interface_methods = """
    suspend fun setTrackedMeasurements(measurements: Set<String>)
"""
text = text.replace("    suspend fun setRestTimerAutoAdvance(enabled: Boolean)", interface_methods + "    suspend fun setRestTimerAutoAdvance(enabled: Boolean)")


with open("app/src/main/java/com/example/repsgrams/data/datastore/CycleSettingsRepository.kt", "w") as f:
    f.write(text)

