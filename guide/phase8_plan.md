# Phase 8 Implementation Plan

## 1. Repositories Updates
- Add `SettingsRepository` or extend `WorkoutRepository` to support CRUD operations on `WorkoutTemplateEntity`, `TemplateBlockEntity`, `TemplateBlockExerciseEntity`, and `ExerciseEntity`.
- Foreign key restrictions: 
  - `WorkoutSession` uses `templateId` with `ON DELETE SET NULL` (this preserves historical sessions).
  - `SetLog` uses `exerciseId` with `ON DELETE RESTRICT` (prevent deleting exercises that have logs).
- Add ordering logic for inserting/swapping exercises within blocks.

## 2. Settings Screen
- Expand `ReminderSettingsScreen.kt` into a full `SettingsScreen.kt`.
- Group settings into logical sections:
  - **Program Editor**: Links to edit Workout A, Workout B, and Manage Exercises.
  - **Supplements**: Inputs for whey serving grams, creatine dose, tub/container sizes.
  - **Reminders**: Keep existing Phase 5 reminder controls.
  - **Cycle Settings**: Edit cycle start date.
  - **Units**: Toggle between kg / lb.

## 3. Program Editor Screens
- `ProgramEditorScreen` (or `TemplateListScreen`): View templates.
- `TemplateEditorScreen`: View/edit blocks within a template.
- `BlockEditorScreen`: View/edit exercises within a block.
- `ExerciseListScreen`: Manage the global dictionary of exercises.

## 4. ViewModels
- Refactor `ReminderSettingsViewModel` to `SettingsViewModel`.
- Create `ProgramEditorViewModel` for the editor screens.

