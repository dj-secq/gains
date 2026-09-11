package com.example.repsgrams.data.db

import androidx.room.Embedded
import androidx.room.Relation

data class BlockExerciseWithExercise(
    @Embedded val assignment: TemplateBlockExerciseEntity,
    @Relation(parentColumn = "exerciseId", entityColumn = "id")
    val exercise: ExerciseEntity,
)

data class BlockWithExercises(
    @Embedded val block: TemplateBlockEntity,
    @Relation(
        entity = TemplateBlockExerciseEntity::class,
        parentColumn = "id",
        entityColumn = "blockId",
    )
    val exercises: List<BlockExerciseWithExercise>,
)

data class WorkoutTemplateWithBlocks(
    @Embedded val template: WorkoutTemplateEntity,
    @Relation(
        entity = TemplateBlockEntity::class,
        parentColumn = "id",
        entityColumn = "templateId",
    )
    val blocks: List<BlockWithExercises>,
)
