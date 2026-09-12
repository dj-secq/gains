import re
with open("app/src/main/java/com/example/repsgrams/ui/session/WorkoutSessionScreen.kt", "r") as f:
    text = f.read()

text = re.sub(r'Text\("Post-workout check-in"[\s\S]*?IosCard \{[\s\S]*?\}', '', text)
text = re.sub(r'Text\("1 whey serving and 5 g creatine.*?\)[\n]*', '', text)
text = re.sub(r'onWheyChanged: \(Boolean\) -> Unit,\n\s*onCreatineChanged: \(Boolean\) -> Unit,', '', text)
text = text.replace("onWheyChanged = viewModel::setWheyTaken,", "")
text = text.replace("onCreatineChanged = viewModel::setCreatineTaken,", "")
text = text.replace("onWheyChanged = {},", "")
text = text.replace("onCreatineChanged = {},", "")
text = text.replace("onWheyChanged = onWheyChanged,", "")
text = text.replace("onCreatineChanged = onCreatineChanged,", "")
with open("app/src/main/java/com/example/repsgrams/ui/session/WorkoutSessionScreen.kt", "w") as f:
    f.write(text)
