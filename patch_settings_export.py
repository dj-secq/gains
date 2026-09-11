import re

with open("app/src/main/java/com/example/repsgrams/ui/settings/SettingsScreen.kt", "r") as f:
    text = f.read()

imports = """import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import java.io.FileOutputStream
import java.io.FileInputStream
"""
text = text.replace("import androidx.compose.ui.Modifier", imports + "import androidx.compose.ui.Modifier")

call_old = """    onNavigateToExercises: () -> Unit
) {"""
call_new = """    onNavigateToExercises: () -> Unit,
    onExport: (Uri) -> Unit = {},
    onImport: (Uri) -> Unit = {}
) {"""
text = text.replace(call_old, call_new)

btn_old = """                            IosButton(text = "Exercise Dictionary", onClick = onNavigateToExercises, isSecondary = true, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }"""
btn_new = """                            IosButton(text = "Exercise Dictionary", onClick = onNavigateToExercises, isSecondary = true, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }
            
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("DATA", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp))
                    IosCard {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            val context = LocalContext.current
                            val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
                                uri?.let { onExport(it) }
                            }
                            val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
                                uri?.let { onImport(it) }
                            }
                            
                            IosButton(text = "Export Backup (.zip)", onClick = { exportLauncher.launch("repsgrams_backup.zip") }, isSecondary = true, modifier = Modifier.fillMaxWidth())
                            IosButton(text = "Import Backup (.zip)", onClick = { importLauncher.launch(arrayOf("application/zip", "*/*")) }, isSecondary = true, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }"""
text = text.replace(btn_old, btn_new)

with open("app/src/main/java/com/example/repsgrams/ui/settings/SettingsScreen.kt", "w") as f:
    f.write(text)

