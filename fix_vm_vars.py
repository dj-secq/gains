import re

with open("app/src/main/java/com/example/repsgrams/ui/session/WorkoutSessionViewModel.kt", "r") as f:
    text = f.read()

vars_old = """    private var restEndEpochMillis: Long? = null
    private var valueInput = ""
    private var weightInput = ""
    private var unitSystem = UnitSystem.KG
    private var isSaving = false
    private var progressionSuggestion: ProgressionSuggestion? = null"""
vars_new = """    private var restEndEpochMillis: Long? = null
    private var valueInput = ""
    private var weightInput = ""
    private var unitSystem = UnitSystem.KG
    private var isSaving = false
    private var progressionSuggestion: ProgressionSuggestion? = null
    private var rpeTagInput: String? = null
    private var notesInput: String = ""
    private var lastTimeRound: PriorRound? = null"""
text = text.replace(vars_old, vars_new)

with open("app/src/main/java/com/example/repsgrams/ui/session/WorkoutSessionViewModel.kt", "w") as f:
    f.write(text)
