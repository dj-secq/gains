import re

with open("app/src/main/java/com/example/repsgrams/ui/session/WorkoutSessionViewModel.kt", "r") as f:
    text = f.read()

# Strip out all occurrences of these imports
bad_imports = [
    "import android.content.Context\\n",
    "import android.content.Intent\\n",
    "import androidx.core.content.ContextCompat\\n",
    "import com.example.repsgrams.service.RestTimerService\\n",
    "import androidx.lifecycle.ViewModel\\n"
]
for b in bad_imports:
    text = re.sub(b, "", text)

# Add them back once at the top
good_imports = """import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.repsgrams.service.RestTimerService
import androidx.lifecycle.ViewModel
"""
text = text.replace("package com.example.repsgrams.ui.session\n\n", "package com.example.repsgrams.ui.session\n\n" + good_imports)

with open("app/src/main/java/com/example/repsgrams/ui/session/WorkoutSessionViewModel.kt", "w") as f:
    f.write(text)
