import re

with open("app/src/main/java/com/example/repsgrams/ui/session/WorkoutSessionScreen.kt", "r") as f:
    text = f.read()

sig_old = """@Composable
fun WorkoutSessionScreen(
    state: WorkoutSessionUiState,"""
sig_new = """@Composable
fun WorkoutSessionScreen(
    state: WorkoutSessionUiState,
    prAchieved: kotlinx.coroutines.flow.SharedFlow<List<com.example.repsgrams.data.db.PersonalRecordEntity>>,"""
text = text.replace(sig_old, sig_new)

call_old = """    WorkoutSessionScreen(
        state = state,"""
call_new = """    WorkoutSessionScreen(
        state = state,
        prAchieved = viewModel.prAchieved,"""
text = text.replace(call_old, call_new)

effect_old = """    Scaffold(
        topBar = {"""
effect_new = """    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(prAchieved) {
        prAchieved.collect { prs ->
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            snackbarHostState.showSnackbar("🎉 New Personal Record!")
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {"""
text = text.replace(effect_old, effect_new)

with open("app/src/main/java/com/example/repsgrams/ui/session/WorkoutSessionScreen.kt", "w") as f:
    f.write(text)
