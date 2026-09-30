package com.example.repsgrams.ui.gate

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.health.connect.client.PermissionController

/**
 * Registered from the first composition, which runs before the activity is started.
 * The pre-open gate composes the rest of the UI later, and a launcher registered then crashes.
 */
class GainsLaunchers {
    var onExport: (Uri?) -> Unit = {}
    var onImport: (Uri?) -> Unit = {}
    var onHealth: (Set<String>) -> Unit = {}
    lateinit var export: ActivityResultLauncher<String>
    lateinit var import: ActivityResultLauncher<Array<String>>
    lateinit var notifications: ActivityResultLauncher<Array<String>>
    lateinit var health: ActivityResultLauncher<Set<String>>
}

val LocalGainsLaunchers = staticCompositionLocalOf<GainsLaunchers> {
    error("Activity result launchers are not registered")
}

@Composable
fun rememberGainsLaunchers(): GainsLaunchers {
    val holders = remember { GainsLaunchers() }
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        holders.onExport(uri)
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        holders.onImport(uri)
    }
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
    val health = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { granted ->
        holders.onHealth(granted)
    }
    holders.export = export
    holders.import = import
    holders.notifications = notifications
    holders.health = health
    return holders
}
