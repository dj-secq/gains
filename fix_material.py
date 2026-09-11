import re
with open("app/build.gradle.kts", "r") as f:
    text = f.read()

text = text.replace(
    'implementation("com.github.DevSrSouza.compose-icons:phosphor:1.1.1")',
    'implementation("androidx.compose.material:material-icons-extended")'
)

with open("app/build.gradle.kts", "w") as f:
    f.write(text)
