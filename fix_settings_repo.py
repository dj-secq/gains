import re

with open("app/src/main/java/com/example/repsgrams/data/datastore/CycleSettingsRepository.kt", "r") as f:
    text = f.read()

text = text.replace("context.dataStore", "context.cycleSettingsDataStore")
text = text.replace("PreferencesKeys.", "")

target = """        restTimerAutoAdvance = preferences[REST_TIMER_AUTO_ADVANCE] ?: true,
    )"""

replacement = """        restTimerAutoAdvance = preferences[REST_TIMER_AUTO_ADVANCE] ?: true,
        trackedMeasurements = preferences[TRACKED_MEASUREMENTS] ?: emptySet(),
        healthConnectEnabled = preferences[HEALTH_CONNECT_ENABLED] ?: false,
        voiceCuesEnabled = preferences[VOICE_CUES_ENABLED] ?: false,
    )"""

text = text.replace(target, replacement)

with open("app/src/main/java/com/example/repsgrams/data/datastore/CycleSettingsRepository.kt", "w") as f:
    f.write(text)

