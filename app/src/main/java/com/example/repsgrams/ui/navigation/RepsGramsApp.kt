package com.example.repsgrams.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.repsgrams.AppContainer
import com.example.repsgrams.ui.today.TodayRoute
import com.example.repsgrams.ui.today.TodayViewModel
import com.example.repsgrams.ui.session.WorkoutSessionRoute
import com.example.repsgrams.ui.session.WorkoutSessionViewModel

import com.example.repsgrams.ui.calendar.CalendarRoute
import com.example.repsgrams.ui.calendar.CalendarViewModel

private enum class TopLevelDestination(val route: String, val label: String, val iconOutlined: ImageVector, val iconFilled: ImageVector) {
    TODAY("today", "Today", Icons.Outlined.Home, Icons.Filled.Home),
    CALENDAR("calendar", "Calendar", Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
    PROGRESS("progress", "Progress", Icons.Outlined.TrendingUp, Icons.Filled.TrendingUp),
    SETTINGS("settings", "Settings", Icons.Outlined.Settings, Icons.Filled.Settings),
}

@Composable
fun RepsGramsApp(
    container: AppContainer,
    notificationTarget: String? = null,
    onNotificationHandled: () -> Unit = {},
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTopLevel = TopLevelDestination.entries.any { it.route == currentRoute }
    LaunchedEffect(notificationTarget) {
        if (notificationTarget != null) {
            navController.navigate(TopLevelDestination.TODAY.route) { launchSingleTop = true }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (isTopLevel) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
                            .windowInsetsPadding(WindowInsets.navigationBars)
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TopLevelDestination.entries.forEach { destination ->
                            val selected = currentRoute == destination.route
                            val contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable(
                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                        indication = null
                                    ) {
                                        navController.navigate(destination.route) {
                                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = if (selected) destination.iconFilled else destination.iconOutlined,
                                    contentDescription = destination.label,
                                    tint = contentColor
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = destination.label,
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                                    color = contentColor
                                )
                            }
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = TopLevelDestination.TODAY.route,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
        ) {
            composable(TopLevelDestination.TODAY.route) {
                val todayViewModel: TodayViewModel = viewModel(
                    factory = TodayViewModel.factory(
                        scheduleRepository = container.scheduleRepository,
                        supplementRepository = container.supplementRepository,
                        workoutRepository = container.workoutRepository,
                        cycleSettingsRepository = container.cycleSettingsRepository,
                        progressRepository = container.progressRepository,
                    ),
                )
                TodayRoute(
                    viewModel = todayViewModel,
                    notificationTarget = notificationTarget,
                    onNotificationHandled = onNotificationHandled,
                    onOpenSession = { sessionId -> navController.navigate("session/$sessionId") },
                )
            }
            composable(TopLevelDestination.CALENDAR.route) {
                val calendarViewModel: CalendarViewModel = viewModel(
                    factory = CalendarViewModel.factory(
                        container.calendarRepository,
                        container.cycleSettingsRepository,
                        container.reminderScheduler,
                        container.clock,
                    ),
                )
                CalendarRoute(calendarViewModel)
            }
            composable(TopLevelDestination.PROGRESS.route) {
                val progressViewModel: com.example.repsgrams.ui.progress.ProgressViewModel = viewModel(
                    factory = com.example.repsgrams.ui.progress.ProgressViewModel.factory(
                        container.progressRepository,
                        container.cycleSettingsRepository,
                    ),
                )
                com.example.repsgrams.ui.progress.ProgressRoute(progressViewModel)
            }
            composable(TopLevelDestination.SETTINGS.route) {
                val settingsViewModel: com.example.repsgrams.ui.settings.SettingsViewModel = viewModel(
                    factory = com.example.repsgrams.ui.settings.SettingsViewModel.factory(
                        container.cycleSettingsRepository, container.reminderScheduler,
                    ),
                )
                com.example.repsgrams.ui.settings.SettingsRoute(
                    viewModel = settingsViewModel,
                    onNavigateToTemplates = { navController.navigate("templates") },
                    onNavigateToExercises = { navController.navigate("exercises") }
                )
            }
            composable("templates") {
                val editorViewModel: com.example.repsgrams.ui.settings.ProgramEditorViewModel = viewModel(
                    factory = com.example.repsgrams.ui.settings.ProgramEditorViewModel.factory(container.workoutRepository)
                )
                com.example.repsgrams.ui.settings.TemplateListRoute(
                    viewModel = editorViewModel,
                    onNavigateToTemplate = { id -> navController.navigate("template/$id") },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "template/{templateId}",
                arguments = listOf(navArgument("templateId") { type = NavType.LongType })
            ) { entry ->
                val templateId = requireNotNull(entry.arguments?.getLong("templateId"))
                val editorViewModel: com.example.repsgrams.ui.settings.ProgramEditorViewModel = viewModel(
                    factory = com.example.repsgrams.ui.settings.ProgramEditorViewModel.factory(container.workoutRepository)
                )
                com.example.repsgrams.ui.settings.TemplateEditorRoute(
                    templateId = templateId,
                    viewModel = editorViewModel,
                    onNavigateToBlock = { id -> navController.navigate("block/$id") },
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "block/{blockId}",
                arguments = listOf(navArgument("blockId") { type = NavType.LongType })
            ) { entry ->
                val blockId = requireNotNull(entry.arguments?.getLong("blockId"))
                val editorViewModel: com.example.repsgrams.ui.settings.ProgramEditorViewModel = viewModel(
                    factory = com.example.repsgrams.ui.settings.ProgramEditorViewModel.factory(container.workoutRepository)
                )
                com.example.repsgrams.ui.settings.BlockEditorRoute(
                    blockId = blockId,
                    viewModel = editorViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("exercises") {
                val exViewModel: com.example.repsgrams.ui.settings.ExerciseDictionaryViewModel = viewModel(
                    factory = com.example.repsgrams.ui.settings.ExerciseDictionaryViewModel.factory(
                        container.workoutRepository
                    )
                )
                com.example.repsgrams.ui.settings.ExerciseDictionaryRoute(
                    viewModel = exViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "session/{sessionId}",
                arguments = listOf(navArgument("sessionId") { type = NavType.LongType }),
            ) { entry ->
                val sessionId = requireNotNull(entry.arguments?.getLong("sessionId"))
                val context = LocalContext.current.applicationContext
                val sessionViewModel: WorkoutSessionViewModel = viewModel(
                    key = "session-$sessionId",
                    factory = WorkoutSessionViewModel.factory(
                        context = context,
                        sessionId = sessionId,
                        workoutRepository = container.workoutRepository,
                        progressStore = container.sessionProgressStore,
                        supplementRepository = container.supplementRepository,
                        cycleSettingsRepository = container.cycleSettingsRepository,
                        clock = container.clock,
                    ),
                )
                WorkoutSessionRoute(sessionViewModel) {
                    navController.popBackStack(TopLevelDestination.TODAY.route, inclusive = false)
                }
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(name: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = "$name coming in a later phase", style = MaterialTheme.typography.titleMedium)
    }
}
