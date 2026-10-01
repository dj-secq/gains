package com.example.repsgrams.reminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.repsgrams.RepsGramsApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Re-arms a dropped reminder chain. Does not open Room when init never started. */
class ReminderBootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return
        val container = (context.applicationContext as RepsGramsApplication).container
        val ready = container.databaseInitialization ?: return
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                ready.await()
                container.reminderScheduler.syncDailyReminders()
            } catch (error: Exception) {
                Log.e(LOG, "boot sync failed: ${error.javaClass.simpleName}: ${error.message}")
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val LOG = "Reminders"
    }
}
