import re
with open("app/src/main/java/com/example/repsgrams/ui/today/TodayViewModel.kt", "r") as f:
    text = f.read()

# Fix combine 6 arguments
target = """    val uiState: StateFlow<TodayUiState> = combine(
        scheduleRepository.observeSuggestion(today),
        combine(
            supplementRepository.observeAllSupplements(),
            supplementRepository.observeIntakesForDate(today)
        ) { allSupps, logs ->
            allSupps.filter { it.isActive }.map { supp ->
                TodaySupplement(supp, logs.any { it.supplementId == supp.id && it.taken })
            }
        },
        workoutRepository.observeActiveSession(),
        cycleSettingsRepository.settings.flatMapLatest { settings ->
            progressRepository.observeStreakInfo(today, settings.adherenceGraceDays)
        },
        progressRepository.observeSupplyInventory(),
        supplementRepository.observeAllSupplements()
    ) { suggestion, todaySupps, activeSession, streakInfo, supplies, allSupps ->"""

replacement = """    val uiState: StateFlow<TodayUiState> = combine(
        combine(
            scheduleRepository.observeSuggestion(today),
            combine(
                supplementRepository.observeAllSupplements(),
                supplementRepository.observeIntakesForDate(today)
            ) { allSupps, logs ->
                allSupps.filter { it.isActive }.map { supp ->
                    TodaySupplement(supp, logs.any { it.supplementId == supp.id && it.taken })
                }
            },
            workoutRepository.observeActiveSession()
        ) { suggestion, todaySupps, activeSession ->
            Triple(suggestion, todaySupps, activeSession)
        },
        combine(
            cycleSettingsRepository.settings.flatMapLatest { settings ->
                progressRepository.observeStreakInfo(today, settings.adherenceGraceDays)
            },
            progressRepository.observeSupplyInventory(),
            supplementRepository.observeAllSupplements()
        ) { streakInfo, supplies, allSupps ->
            Triple(streakInfo, supplies, allSupps)
        }
    ) { (suggestion, todaySupps, activeSession), (streakInfo, supplies, allSupps) ->"""

text = text.replace(target, replacement)
text = text.replace("import com.example.repsgrams.domain.schedule.SuggestionStatus\n", "")

with open("app/src/main/java/com/example/repsgrams/ui/today/TodayViewModel.kt", "w") as f:
    f.write(text)

with open("app/src/main/java/com/example/repsgrams/ui/progress/ProgressViewModel.kt", "r") as f:
    text2 = f.read()

# I need to fix ProgressViewModel. It seems my patch didn't apply properly on the UiState constructor.
# I will just write a python script that completely replaces the constructor block for ProgressUiState
text2 = re.sub(r'        ProgressUiState\([\s\S]*?creatineInventory = creatine\n\s*\)', 
"""        ProgressUiState(
            exercises = exData.exercises,
            selectedExerciseId = selectedId,
            exerciseHistory = exData.exerciseHistory,
            isWeightView = exData.isWeightView,
            currentStreak = stats.streakInfo.currentStreak,
            bestStreak = stats.streakInfo.bestStreak,
            bodyweightHistory = stats.bwHistory.map { it.date to it.weightKg },
            activeSupplements = activeSupps,
            selectedSupplementId = chosenSuppId,
            supplementAdherence30d = adherence30,
            supplementAdherence90d = adherence90,
            supplementSupplies = suppSupplies
        )""", text2)
with open("app/src/main/java/com/example/repsgrams/ui/progress/ProgressViewModel.kt", "w") as f:
    f.write(text2)
