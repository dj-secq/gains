import re
with open("app/build.gradle.kts", "r") as f:
    text = f.read()

text = text.replace(
    'implementation("io.github.dev778g-me:phosphoricon-compose:1.0.4")',
    'implementation("com.github.DevSrSouza.compose-icons:phosphor:1.1.1")'
)

with open("app/build.gradle.kts", "w") as f:
    f.write(text)
