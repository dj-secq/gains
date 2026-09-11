import re

with open("app/src/main/java/com/example/repsgrams/ui/session/WorkoutSessionViewModel.kt", "r") as f:
    text = f.read()

imports = """import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.example.repsgrams.service.RestTimerService
"""
text = text.replace("import androidx.lifecycle.ViewModel", imports + "import androidx.lifecycle.ViewModel")

vm_class_old = """class WorkoutSessionViewModel(
    private val sessionId: Long,
    private val workoutRepository: WorkoutRepository,
    private val progressStore: SessionProgressStore,
    private val supplementRepository: SupplementRepository,
    private val cycleSettingsRepository: CycleSettingsRepository,
    private val clock: Clock,
) : ViewModel() {"""
vm_class_new = """class WorkoutSessionViewModel(
    private val context: Context,
    private val sessionId: Long,
    private val workoutRepository: WorkoutRepository,
    private val progressStore: SessionProgressStore,
    private val supplementRepository: SupplementRepository,
    private val cycleSettingsRepository: CycleSettingsRepository,
    private val clock: Clock,
) : ViewModel() {"""
text = text.replace(vm_class_old, vm_class_new)

factory_old = """        fun factory(
            sessionId: Long,
            workoutRepository: WorkoutRepository,
            progressStore: SessionProgressStore,
            supplementRepository: SupplementRepository,
            cycleSettingsRepository: CycleSettingsRepository,
            clock: Clock,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(WorkoutSessionViewModel::class.java))
                return WorkoutSessionViewModel(
                    sessionId, workoutRepository, progressStore, supplementRepository,
                    cycleSettingsRepository, clock,
                ) as T
            }
        }"""
factory_new = """        fun factory(
            context: Context,
            sessionId: Long,
            workoutRepository: WorkoutRepository,
            progressStore: SessionProgressStore,
            supplementRepository: SupplementRepository,
            cycleSettingsRepository: CycleSettingsRepository,
            clock: Clock,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(WorkoutSessionViewModel::class.java))
                return WorkoutSessionViewModel(
                    context, sessionId, workoutRepository, progressStore, supplementRepository,
                    cycleSettingsRepository, clock,
                ) as T
            }
        }"""
text = text.replace(factory_old, factory_new)

with open("app/src/main/java/com/example/repsgrams/ui/session/WorkoutSessionViewModel.kt", "w") as f:
    f.write(text)


with open("app/src/main/java/com/example/repsgrams/ui/navigation/RepsGramsApp.kt", "r") as f:
    text = f.read()

import_context = "import androidx.compose.ui.platform.LocalContext\n"
if "import androidx.compose.ui.platform.LocalContext" not in text:
    text = text.replace("import androidx.compose.ui.Modifier", import_context + "import androidx.compose.ui.Modifier")

app_old = """                val sessionViewModel: WorkoutSessionViewModel = viewModel(
                    key = "session-$sessionId",
                    factory = WorkoutSessionViewModel.factory(
                        sessionId = sessionId,"""
app_new = """                val context = LocalContext.current.applicationContext
                val sessionViewModel: WorkoutSessionViewModel = viewModel(
                    key = "session-$sessionId",
                    factory = WorkoutSessionViewModel.factory(
                        context = context,
                        sessionId = sessionId,"""
text = text.replace(app_old, app_new)

with open("app/src/main/java/com/example/repsgrams/ui/navigation/RepsGramsApp.kt", "w") as f:
    f.write(text)
