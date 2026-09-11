import re

with open("app/src/main/java/com/example/repsgrams/ui/session/WorkoutSessionScreen.kt", "r") as f:
    text = f.read()

sig_old = """    onRpeTagChanged: (String?) -> Unit,
) {
    val exercise = state.exercise
    IosCard {"""
sig_new = """    onRpeTagChanged: (String?) -> Unit,
) {
    val exercise = state.exercise
    val haptic = LocalHapticFeedback.current
    IosCard {"""
text = text.replace(sig_old, sig_new)

with open("app/src/main/java/com/example/repsgrams/ui/session/WorkoutSessionScreen.kt", "w") as f:
    f.write(text)
