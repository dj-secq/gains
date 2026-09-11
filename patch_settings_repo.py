import re

with open("app/src/main/java/com/example/repsgrams/data/datastore/CycleSettingsRepository.kt", "r") as f:
    text = f.read()

vars_old = """    val vibrationEnabled: Boolean = true,
    val autoAdvanceEnabled: Boolean = false,
)"""
vars_new = """    val vibrationEnabled: Boolean = true,
    val autoAdvanceEnabled: Boolean = false,
    val trackedMeasurements: Set<String> = emptySet(),
)"""
text = text.replace(vars_old, vars_new)

pref_old = """    private val REST_SOUND_KEY = stringPreferencesKey("rest_sound")
    private val VIBRATION_ENABLED_KEY = booleanPreferencesKey("vibration_enabled")
    private val AUTO_ADVANCE_KEY = booleanPreferencesKey("auto_advance")"""
pref_new = """    private val REST_SOUND_KEY = stringPreferencesKey("rest_sound")
    private val VIBRATION_ENABLED_KEY = booleanPreferencesKey("vibration_enabled")
    private val AUTO_ADVANCE_KEY = booleanPreferencesKey("auto_advance")
    private val TRACKED_MEASUREMENTS_KEY = stringSetPreferencesKey("tracked_measurements")"""
text = text.replace(pref_old, pref_new)

map_old = """                vibrationEnabled = prefs[VIBRATION_ENABLED_KEY] ?: true,
                autoAdvanceEnabled = prefs[AUTO_ADVANCE_KEY] ?: false,
            )
        }"""
map_new = """                vibrationEnabled = prefs[VIBRATION_ENABLED_KEY] ?: true,
                autoAdvanceEnabled = prefs[AUTO_ADVANCE_KEY] ?: false,
                trackedMeasurements = prefs[TRACKED_MEASUREMENTS_KEY] ?: emptySet(),
            )
        }"""
text = text.replace(map_old, map_new)

set_methods = """
    suspend fun setTrackedMeasurements(measurements: Set<String>) {
        dataStore.edit { it[TRACKED_MEASUREMENTS_KEY] = measurements }
    }
"""
text = text.replace("    suspend fun setAutoAdvanceEnabled(enabled: Boolean) {", set_methods + "    suspend fun setAutoAdvanceEnabled(enabled: Boolean) {")

with open("app/src/main/java/com/example/repsgrams/data/datastore/CycleSettingsRepository.kt", "w") as f:
    f.write(text)

