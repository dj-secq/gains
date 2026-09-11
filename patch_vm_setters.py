import re

with open("app/src/main/java/com/example/repsgrams/ui/session/WorkoutSessionViewModel.kt", "r") as f:
    text = f.read()

setters = """    fun updateNotes(notes: String) {
        notesInput = notes
        publishActive()
    }
    fun updateRpeTag(tag: String?) {
        rpeTagInput = tag
        publishActive()
    }
"""

text = text.replace("    fun adjustWeight(delta: Float) {", setters + "    fun adjustWeight(delta: Float) {")

with open("app/src/main/java/com/example/repsgrams/ui/session/WorkoutSessionViewModel.kt", "w") as f:
    f.write(text)
