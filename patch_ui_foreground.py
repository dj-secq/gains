import re

with open("app/src/main/java/com/example/repsgrams/ui/session/WorkoutSessionScreen.kt", "r") as f:
    text = f.read()

imports = """import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.example.repsgrams.service.RestTimerService
"""
text = text.replace("import androidx.compose.runtime.LaunchedEffect", imports + "import androidx.compose.runtime.LaunchedEffect")

lifecycle = """    LaunchedEffect(viewModel) { viewModel.summaryDone.collect { onFinished() } }
    
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                RestTimerService.isSessionForeground = true
            } else if (event == Lifecycle.Event.ON_PAUSE) {
                RestTimerService.isSessionForeground = false
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            RestTimerService.isSessionForeground = false
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
"""
text = text.replace("    LaunchedEffect(viewModel) { viewModel.summaryDone.collect { onFinished() } }", lifecycle)

with open("app/src/main/java/com/example/repsgrams/ui/session/WorkoutSessionScreen.kt", "w") as f:
    f.write(text)
