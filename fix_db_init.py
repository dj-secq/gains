import re
with open("app/src/main/java/com/example/repsgrams/data/db/DatabaseInitializer.kt", "r") as f:
    text = f.read()

# Replace the supply seeding logic
new_seed_logic = """val wheyId = database.supplementDao().insert(
                com.example.repsgrams.data.db.SupplementEntity(
                    name = "Whey Protein",
                    doseAmount = 1f,
                    unit = "serving",
                    scheduleType = "workoutDayOnly",
                    containerSize = 65,
                    lowSupplyThreshold = 10,
                    colorToken = "green",
                    iconName = "water_drop"
                )
            )
            val creatineId = database.supplementDao().insert(
                com.example.repsgrams.data.db.SupplementEntity(
                    name = "Creatine",
                    doseAmount = 5f,
                    unit = "g",
                    scheduleType = "daily",
                    containerSize = 30,
                    lowSupplyThreshold = 5,
                    colorToken = "teal",
                    iconName = "science"
                )
            )
            database.supplyInventoryDao().upsert(
                com.example.repsgrams.data.db.SupplyInventoryEntity(
                    supplementId = wheyId,
                    totalServings = 65,
                    servingsRemaining = 65f,
                    startDate = today,
                )
            )
            database.supplyInventoryDao().upsert(
                com.example.repsgrams.data.db.SupplyInventoryEntity(
                    supplementId = creatineId,
                    totalServings = 30,
                    servingsRemaining = 30f,
                    startDate = today,
                )
            )"""

text = re.sub(r'database\.supplyInventoryDao\(\)\.upsert\([\s\S]*?\}', new_seed_logic + '\n            metadataDao.put(DatabaseMetadataEntity(SEED_VERSION_KEY, CURRENT_SEED_VERSION))\n        }\n', text)
with open("app/src/main/java/com/example/repsgrams/data/db/DatabaseInitializer.kt", "w") as f:
    f.write(text)

