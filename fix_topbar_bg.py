import os
import glob

files = [
    "app/src/main/java/com/example/repsgrams/ui/progress/ProgressScreen.kt",
    "app/src/main/java/com/example/repsgrams/ui/today/TodayScreen.kt",
    "app/src/main/java/com/example/repsgrams/ui/settings/SettingsScreen.kt",
    "app/src/main/java/com/example/repsgrams/ui/calendar/CalendarScreen.kt",
]

for file in files:
    with open(file, "r") as f:
        text = f.read()
    
    text = text.replace("LargeTopAppBar(", "TopAppBar(")
    text = text.replace("largeTopAppBarColors(", "topAppBarColors(")
    
    with open(file, "w") as f:
        f.write(text)

# Also fix Color.kt
with open("app/src/main/java/com/example/repsgrams/ui/theme/Color.kt", "r") as f:
    color_text = f.read()

# Change DarkBackground and DarkSurface to a nice deep slate
color_text = color_text.replace("val DarkBackground = Color(0xFF000000)", "val DarkBackground = Color(0xFF0E1117)") # Deep dark slate
color_text = color_text.replace("val DarkSurface = Color(0xFF1C1C1E)", "val DarkSurface = Color(0xFF1A1D24)")

with open("app/src/main/java/com/example/repsgrams/ui/theme/Color.kt", "w") as f:
    f.write(color_text)

