import re
with open("app/build.gradle.kts", "r") as f:
    text = f.read()

text = text.replace(
    'implementation("br.com.devsrsouza.compose.icons:phosphor-android:1.1.1")',
    'implementation("io.github.dev778g-me:phosphoricon-compose:1.0.4")'
)

with open("app/build.gradle.kts", "w") as f:
    f.write(text)
