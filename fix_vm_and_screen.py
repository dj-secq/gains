import re

with open("app/src/main/java/com/example/repsgrams/ui/session/WorkoutSessionViewModel.kt", "r") as f:
    text = f.read()

service_methods = """    private fun startRestTimerService(endMillis: Long, upNext: String) {
        val intent = Intent(context, RestTimerService::class.java).apply {
            action = RestTimerService.ACTION_START
            putExtra(RestTimerService.EXTRA_END_MILLIS, endMillis)
            putExtra(RestTimerService.EXTRA_UP_NEXT, upNext)
        }
        ContextCompat.startForegroundService(context, intent)
    }

    private fun addTimeToRestTimerService() {
        val intent = Intent(context, RestTimerService::class.java).apply {
            action = RestTimerService.ACTION_ADD_TIME
        }
        ContextCompat.startForegroundService(context, intent)
    }

    private fun stopRestTimerService() {
        val intent = Intent(context, RestTimerService::class.java).apply {
            action = RestTimerService.ACTION_STOP
        }
        context.startService(intent) // stop action is safe for startService
    }

"""
text = text.replace("    private fun currentExercise()", service_methods + "    private fun currentExercise()")

with open("app/src/main/java/com/example/repsgrams/ui/session/WorkoutSessionViewModel.kt", "w") as f:
    f.write(text)

with open("app/src/main/java/com/example/repsgrams/ui/session/WorkoutSessionScreen.kt", "r") as f:
    text = f.read()

imports = """import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Lifecycle
import com.example.repsgrams.service.RestTimerService
"""
text = text.replace("import androidx.compose.ui.Modifier", imports + "import androidx.compose.ui.Modifier")

with open("app/src/main/java/com/example/repsgrams/ui/session/WorkoutSessionScreen.kt", "w") as f:
    f.write(text)

