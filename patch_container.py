import re

with open("app/src/main/java/com/example/repsgrams/AppContainer.kt", "r") as f:
    text = f.read()

text = text.replace(".addMigrations(AppDatabase.MIGRATION_1_2).build()", ".addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4).build()")

with open("app/src/main/java/com/example/repsgrams/AppContainer.kt", "w") as f:
    f.write(text)
