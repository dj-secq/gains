with open("app/src/main/java/com/example/repsgrams/ui/progress/ProgressScreen.kt", "r") as f:
    text = f.read()

imports = """import androidx.compose.ui.draw.alpha
"""

text = text.replace("import androidx.compose.ui.draw.clip", imports + "import androidx.compose.ui.draw.clip")

with open("app/src/main/java/com/example/repsgrams/ui/progress/ProgressScreen.kt", "w") as f:
    f.write(text)
