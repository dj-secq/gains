import re

with open("app/src/main/java/com/example/repsgrams/ui/settings/SettingsViewModel.kt", "r") as f:
    text = f.read()

text = text.replace("fun updateRestTimerAutoAdvance(enabled: Boolean) {", "fun updateDefaultRestSeconds(seconds: Int) {\n        viewModelScope.launch { repository.setDefaultRestSeconds(seconds) }\n    }\n\n    fun updateRestTimerAutoAdvance(enabled: Boolean) {")

with open("app/src/main/java/com/example/repsgrams/ui/settings/SettingsViewModel.kt", "w") as f:
    f.write(text)
