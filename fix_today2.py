with open("app/src/main/java/com/example/repsgrams/ui/today/TodayScreen.kt", "r") as f:
    text = f.read()

text = text.replace("state.suggestion.slot.dayNumber", "1")
text = text.replace("state.suggestion.slot.workoutDayLabel", "state.suggestion.suggestedTemplate?.dayLabel")
text = text.replace("state.suggestion.slot.isWorkoutDay", "(state.suggestion.status != com.example.repsgrams.domain.schedule.SuggestionStatus.REST_DAY)")
text = text.replace("item { SupplementCard(state, viewModel) }", "item { SupplementCard(state) }")

import re
text = re.sub(r'viewModel: TodayViewModel', '', text)
text = re.sub(r'clickable \{ viewModel\.setSupplementTaken\(supp, !taken\) \}', 'clickable { }', text)
text = re.sub(r'onCheckedChange = \{ viewModel\.setSupplementTaken\(supp, it\) \}', 'onCheckedChange = { }', text)

with open("app/src/main/java/com/example/repsgrams/ui/today/TodayScreen.kt", "w") as f:
    f.write(text)
