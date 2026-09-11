import re
with open("app/build.gradle.kts", "r") as f:
    text = f.read()

text = text.replace(
    'implementation("androidx.core:core-splashscreen:1.0.1")',
    'implementation("androidx.core:core-splashscreen:1.0.1")\n    implementation("io.github.dev778g-me:phosphoricon-compose:1.0.4")'
)

with open("app/build.gradle.kts", "w") as f:
    f.write(text)
