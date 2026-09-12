import re
with open("app/src/main/java/com/example/repsgrams/ui/navigation/RepsGramsApp.kt", "r") as f:
    text = f.read()

text = text.replace("onNavigateToExercises = { navController.navigate(\"exercises\") },", "onNavigateToExercises = { navController.navigate(\"exercises\") },\n                    onNavigateToSupplements = { navController.navigate(\"manage_supplements\") },")

new_route = """            composable("manage_supplements") {
                val supplementsViewModel: com.example.repsgrams.ui.settings.ManageSupplementsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = com.example.repsgrams.ui.settings.ManageSupplementsViewModel.provideFactory(container.supplementRepository)
                )
                com.example.repsgrams.ui.settings.ManageSupplementsRoute(
                    viewModel = supplementsViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
"""
text = text.replace("composable(\"exercises\") {", new_route + "            composable(\"exercises\") {")

with open("app/src/main/java/com/example/repsgrams/ui/navigation/RepsGramsApp.kt", "w") as f:
    f.write(text)
