import re

with open("app/src/main/java/com/example/repsgrams/ui/settings/SettingsScreen.kt", "r") as f:
    text = f.read()

# Remove the bad injection
bad_block = """            
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 16.dp)) {
                    Text("ABOUT & CREDITS", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp))
                    IosCard {
                        Column {
                            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                IconBadge(icon = Icons.Outlined.Info, tint = MaterialTheme.colorScheme.primary)
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Open Source Credits", style = MaterialTheme.typography.bodyLarge)
                                    Text("Exercise images provided by free-exercise-db (yuhonas) under the Unlicense / Public Domain.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }"""
text = text.replace(bad_block, "")

# Insert it at the end of the LazyColumn (which ends around line 357 before `ToggleSetting` definitions)
# Let's find the `item { HEALTH & INTEGRATIONS ... }` block
health_block = """            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("HEALTH & INTEGRATIONS", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp))
                    IosCard {
                        Column {
                            SettingToggle(
                                "Health Connect", 
                                "Sync workouts to Health Connect", 
                                Icons.Outlined.HealthAndSafety, 
                                AppColors.workout, 
                                checked = settings.healthConnectEnabled, 
                                enabled = true, 
                                onChecked = { checked ->
                                    onHealthConnectEnabled(checked)
                                }
                            )
                            HorizontalDivider(modifier = Modifier.padding(start = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
                            SettingToggle(
                                "Background Sync", 
                                "Sync even when app is closed", 
                                Icons.Outlined.Sync, 
                                AppColors.progressPurple, 
                                checked = settings.backgroundSyncEnabled, 
                                enabled = settings.healthConnectEnabled,
                                onChecked = { checked ->
                                    onBackgroundSyncEnabled(checked)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}"""

replacement_health_block = health_block.replace("        }\n    }\n}", f"""        }}
{bad_block}
        }}
    }}
}}""")

text = text.replace(health_block, replacement_health_block)

with open("app/src/main/java/com/example/repsgrams/ui/settings/SettingsScreen.kt", "w") as f:
    f.write(text)

