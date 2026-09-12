import re
with open("app/src/main/java/com/example/repsgrams/data/repository/ProgressRepository.kt", "r") as f:
    text = f.read()

text = text.replace("SupplementLogEntity", "SupplementIntakeLogEntity")
text = text.replace("supplementLogDao", "supplementIntakeLogDao")
with open("app/src/main/java/com/example/repsgrams/data/repository/ProgressRepository.kt", "w") as f:
    f.write(text)
