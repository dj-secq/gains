import re

with open("app/src/main/java/com/example/repsgrams/ui/settings/ExerciseDictionaryScreen.kt", "r") as f:
    text = f.read()

imports = """import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.outlined.FitnessCenter
import com.example.repsgrams.ui.theme.AppColors
import androidx.compose.ui.graphics.Color
"""
text = text.replace("import androidx.compose.ui.Modifier", imports + "import androidx.compose.ui.Modifier")

list_item_old = """                ListItem(
                    headlineContent = { Text(ex.name) },
                    supportingContent = { Text(if (ex.tracksWeight) "Weighted" else "Bodyweight") },
                    modifier = Modifier.clickable {
                        editingExercise = ex
                        showDialog = true
                    }
                )"""
list_item_new = """                ListItem(
                    headlineContent = { Text(ex.name) },
                    supportingContent = { 
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(if (ex.tracksWeight) "Weighted" else "Bodyweight")
                            Surface(
                                color = AppColors.wheyGreen.copy(alpha = 0.2f),
                                shape = CircleShape,
                            ) {
                                Text(
                                    ex.muscleGroup,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AppColors.wheyGreen,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    },
                    leadingContent = {
                        Box(
                            modifier = Modifier.size(48.dp).clip(MaterialTheme.shapes.small).background(AppColors.workout.copy(alpha=0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.FitnessCenter, contentDescription = null, tint = AppColors.workout)
                        }
                    },
                    modifier = Modifier.clickable {
                        editingExercise = ex
                        showDialog = true
                    }
                )"""
text = text.replace(list_item_old, list_item_new)

with open("app/src/main/java/com/example/repsgrams/ui/settings/ExerciseDictionaryScreen.kt", "w") as f:
    f.write(text)
