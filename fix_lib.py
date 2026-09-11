with open("app/src/main/java/com/example/repsgrams/ui/settings/ExerciseLibraryScreen.kt", "r") as f:
    text = f.read()
    
text = text.replace("androidx.compose.material.icons.Icons.Outlined.ArrowBackIosNew", "androidx.compose.material.icons.outlined.ArrowBackIosNew")
with open("app/src/main/java/com/example/repsgrams/ui/settings/ExerciseLibraryScreen.kt", "w") as f:
    f.write(text)
