import re
with open("app/build.gradle.kts", "r") as f:
    text = f.read()

text = text.replace(
    'implementation("androidx.core:core-splashscreen:1.0.1")',
    'implementation("androidx.core:core-splashscreen:1.0.1")\n    implementation("com.composables:core:1.1.4")\n    implementation("com.composables:icons-lucide:1.1.4")'
)

with open("app/build.gradle.kts", "w") as f:
    f.write(text)
