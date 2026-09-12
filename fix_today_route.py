with open("app/src/main/java/com/example/repsgrams/ui/today/TodayScreen.kt", "r") as f:
    text = f.read()

text = text.replace("fun TodayRoute(\n    ,", "fun TodayRoute(\n    viewModel: TodayViewModel,")
with open("app/src/main/java/com/example/repsgrams/ui/today/TodayScreen.kt", "w") as f:
    f.write(text)
