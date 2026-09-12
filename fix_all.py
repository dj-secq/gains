import re

def fix_file(filepath, add_imports, remove_com=False):
    with open(filepath, "r") as f:
        text = f.read()
    
    if remove_com:
        text = text.replace(".com.example.repsgrams.ui.components.shimmer()", ".shimmer()")
        
    lines = text.split("\n")
    pkg_line = -1
    for i, line in enumerate(lines):
        if line.startswith("package "):
            pkg_line = i
            break
            
    if pkg_line != -1:
        for imp in add_imports:
            lines.insert(pkg_line + 1, imp)
            
    with open(filepath, "w") as f:
        f.write("\n".join(lines))

fix_file("app/src/main/java/com/example/repsgrams/ui/calendar/CalendarScreen.kt", ["import com.example.repsgrams.ui.components.shimmer"], True)
fix_file("app/src/main/java/com/example/repsgrams/ui/session/WorkoutSessionScreen.kt", ["import com.example.repsgrams.ui.components.shimmer"], True)
fix_file("app/src/main/java/com/example/repsgrams/ui/today/TodayScreen.kt", ["import com.example.repsgrams.ui.components.shimmer"], True)
fix_file("app/src/main/java/com/example/repsgrams/ui/components/IosComponents.kt", ["import androidx.compose.animation.core.animateFloat"], False)

