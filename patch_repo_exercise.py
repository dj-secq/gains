import re

with open("app/src/main/java/com/example/repsgrams/data/repository/WorkoutRepository.kt", "r") as f:
    text = f.read()

create_old = """    suspend fun createCustomExercise(name: String, tracksWeight: Boolean, notes: String? = null) {
        database.exerciseDao().insert(
            ExerciseEntity(
                name = name,
                tracksWeight = tracksWeight,
                notes = notes,
                isCustom = true
            )
        )
    }"""
create_new = """    suspend fun createCustomExercise(name: String, tracksWeight: Boolean, notes: String? = null, muscleGroup: String = "Uncategorized") {
        database.exerciseDao().insert(
            ExerciseEntity(
                name = name,
                tracksWeight = tracksWeight,
                notes = notes,
                muscleGroup = muscleGroup,
                isCustom = true
            )
        )
    }"""
text = text.replace(create_old, create_new)

with open("app/src/main/java/com/example/repsgrams/data/repository/WorkoutRepository.kt", "w") as f:
    f.write(text)
