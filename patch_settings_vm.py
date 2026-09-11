import re

with open("app/src/main/java/com/example/repsgrams/ui/settings/SettingsViewModel.kt", "r") as f:
    text = f.read()

method = """
    fun setTrackedMeasurements(measurements: Set<String>) {
        viewModelScope.launch { repository.setTrackedMeasurements(measurements) }
    }
"""
text = text.replace("    fun setAutoAdvanceEnabled(enabled: Boolean) {", method + "    fun setAutoAdvanceEnabled(enabled: Boolean) {")

with open("app/src/main/java/com/example/repsgrams/ui/settings/SettingsViewModel.kt", "w") as f:
    f.write(text)
