import re
with open("app/build.gradle.kts", "r") as f:
    text = f.read()

text = text.replace(
    'implementation("com.composables:core:1.1.4")\n    implementation("com.composables:icons-lucide:1.1.4")',
    'implementation("io.github.thelacspace:lucide-compose:1.16.0")'
)

with open("app/build.gradle.kts", "w") as f:
    f.write(text)
