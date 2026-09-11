with open("app/src/main/java/com/example/repsgrams/ui/session/WorkoutSessionScreen.kt", "r") as f:
    text = f.read()

text = text.replace(
    'CheckRow("Whey protein taken", state.wheyTaken, onWheyChanged)',
    'CheckRow("Whey protein taken", Icons.Outlined.FlashlightOn, AppColors.wheyGreen, state.wheyTaken, onWheyChanged)'
)
text = text.replace(
    'CheckRow("Creatine taken", state.creatineTaken, onCreatineChanged)',
    'CheckRow("Creatine taken", Icons.Outlined.Science, AppColors.creatineTeal, state.creatineTaken, onCreatineChanged)'
)

with open("app/src/main/java/com/example/repsgrams/ui/session/WorkoutSessionScreen.kt", "w") as f:
    f.write(text)
