package com.example.repsgrams.ui.gate

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.repsgrams.data.DatabaseGateDecision
import com.example.repsgrams.ui.components.IosButton
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
fun DatabaseGateScreen(
    gate: DatabaseGateDecision,
    unreadable: Boolean,
    onExport: suspend (Uri) -> String,
    onImport: suspend (Uri) -> String,
    onContinue: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val launchers = LocalGainsLaunchers.current
    var busy by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf<String?>(null) }
    SideEffect {
        launchers.onExport = { uri ->
            picking = false
            if (uri != null) {
                busy = true
                scope.launch {
                    status = "Exporting..."
                    status = onExport(uri)
                    busy = false
                }
            }
        }
        launchers.onImport = { uri ->
            picking = false
            if (uri != null) {
                busy = true
                scope.launch {
                    status = "Importing..."
                    status = onImport(uri)
                    busy = false
                }
            }
        }
    }
    val blocked = busy || picking
    val title = when {
        unreadable -> "This workout log could not be read."
        gate == DatabaseGateDecision.IMPORT_ONLY -> "This file is newer than this build."
        else -> "Update this workout log?"
    }
    val body = when {
        unreadable -> "Gains did not open it. Import a backup from this version or an older one."
        gate == DatabaseGateDecision.IMPORT_ONLY ->
            "Gains will not open it. Import a backup from this version or an older one."
        else ->
            "This log was saved by an older version of Gains. Export a backup before it is updated. " +
                "Anyone who can open the file can read the log."
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(12.dp))
        Text(body, style = MaterialTheme.typography.bodyLarge)
        status?.let { message ->
            Spacer(modifier = Modifier.height(12.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(modifier = Modifier.height(24.dp))
        if (gate == DatabaseGateDecision.EXPORT_THEN_CONTINUE && !unreadable) {
            IosButton(
                text = "Export backup",
                onClick = {
                    if (blocked) return@IosButton
                    picking = true
                    launchers.export.launch("gains-backup-${LocalDate.now()}.zip")
                },
                enabled = !blocked,
            )
            Spacer(modifier = Modifier.height(12.dp))
            IosButton(
                text = "Continue",
                onClick = {
                    if (blocked) return@IosButton
                    busy = true
                    onContinue()
                },
                enabled = !blocked,
                isSecondary = true,
            )
        } else if (gate == DatabaseGateDecision.IMPORT_ONLY || unreadable) {
            IosButton(
                text = "Import backup",
                onClick = {
                    if (blocked) return@IosButton
                    picking = true
                    launchers.import.launch(
                        arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"),
                    )
                },
                enabled = !blocked,
            )
        }
    }
}
