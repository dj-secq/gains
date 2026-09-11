import re
with open("/home/dj/AndroidStudioProjects/RepsGrams/app/src/main/java/com/example/repsgrams/ui/navigation/RepsGramsApp.kt", "r") as f:
    text = f.read()

text = text.replace("Scaffold(\n        bottomBar", "Scaffold(\n        contentWindowInsets = WindowInsets(0, 0, 0, 0),\n        bottomBar")

if "import androidx.compose.foundation.layout.WindowInsets" not in text:
    text = text.replace("import androidx.compose.foundation.layout.fillMaxWidth", "import androidx.compose.foundation.layout.fillMaxWidth\nimport androidx.compose.foundation.layout.WindowInsets")

with open("/home/dj/AndroidStudioProjects/RepsGrams/app/src/main/java/com/example/repsgrams/ui/navigation/RepsGramsApp.kt", "w") as f:
    f.write(text)
