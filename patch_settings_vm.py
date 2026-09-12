import re

with open("app/src/main/java/com/example/repsgrams/ui/settings/SettingsViewModel.kt", "r") as f:
    text = f.read()

funcs = """    fun setTrackedMeasurements(measurements: Set<String>) {
        viewModelScope.launch {
            repository.setTrackedMeasurements(measurements)
        }
    }

    fun setHealthConnectEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setHealthConnectEnabled(enabled)
        }
    }
    
    fun setVoiceCuesEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setVoiceCuesEnabled(enabled)
        }
    }

}"""

text = re.sub(r'\}$', funcs, text)

with open("app/src/main/java/com/example/repsgrams/ui/settings/SettingsViewModel.kt", "w") as f:
    f.write(text)

