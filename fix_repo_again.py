import re

with open("app/src/main/java/com/example/repsgrams/data/datastore/CycleSettingsRepository.kt", "r") as f:
    text = f.read()

# Add TRACKED_MEASUREMENTS_KEY
if "val TRACKED_MEASUREMENTS = stringSetPreferencesKey" not in text:
    text = text.replace('val REST_TIMER_AUTO_ADVANCE = booleanPreferencesKey("rest_timer_auto_advance")', 'val REST_TIMER_AUTO_ADVANCE = booleanPreferencesKey("rest_timer_auto_advance")\n        val TRACKED_MEASUREMENTS = androidx.datastore.preferences.core.stringSetPreferencesKey("tracked_measurements")')

# Update method
meth_old = """    override suspend fun setTrackedMeasurements(measurements: Set<String>) {
        dataStore.edit { it[PreferencesKeys.TRACKED_MEASUREMENTS_KEY] = measurements }
    }"""
meth_new = """    override suspend fun setTrackedMeasurements(measurements: Set<String>) {
        context.cycleSettingsDataStore.edit { it[TRACKED_MEASUREMENTS] = measurements }
    }"""
text = text.replace(meth_old, meth_new)

# Update toSettings
map_old = "trackedMeasurements = prefs[PreferencesKeys.TRACKED_MEASUREMENTS_KEY] ?: emptySet(),"
map_new = "trackedMeasurements = prefs[TRACKED_MEASUREMENTS] ?: emptySet(),"
text = text.replace(map_old, map_new)

with open("app/src/main/java/com/example/repsgrams/data/datastore/CycleSettingsRepository.kt", "w") as f:
    f.write(text)

