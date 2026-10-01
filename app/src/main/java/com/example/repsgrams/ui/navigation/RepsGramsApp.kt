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
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.repsgrams.AppContainer
import com.example.repsgrams.ui.today.TodayRoute
import com.example.repsgrams.ui.today.TodayViewModel
import com.example.repsgrams.data.datastore.ThemeMode
import com.example.repsgrams.ui.session.WorkoutSessionRoute
import com.example.repsgrams.ui.session.WorkoutSessionViewModel
import com.example.repsgrams.ui.theme.RepsGramsTheme

import com.example.repsgrams.ui.calendar.CalendarRoute
import com.example.repsgrams.ui.calendar.CalendarViewModel

private enum class TopLevelDestination(val route: String, val label: String, val iconOutlined: ImageVector, val iconFilled: ImageVector) {
    TODAY("today", "Today", Icons.Outlined.Home, Icons.Filled.Home),
    CALENDAR("calendar", "Calendar", Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth),
    PROGRESS("progress", "Progress", Icons.Outlined.TrendingUp, Icons.Filled.TrendingUp),
    SETTINGS("settings", "Settings", Icons.Outlined.Settings, Icons.Filled.Settings),
}

private const val MAIN_ROUTE = "main"

@Composable
fun RepsGramsApp(
    container: AppContainer,
    notificationTarget: String? = null,
    onNotificationHandled: () -> Unit = {},
    openSessionId: Long? = null,
    onOpenSessionHandled: () -> Unit = {},
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    var backupNotice by remember { mutableStateOf<BackupNotice?>(null) }
    val backupSnackbar = remember { SnackbarHostState() }
    LaunchedEffect(backupNotice?.token) {
        val text = backupNotice?.text ?: return@LaunchedEffect
        backupSnackbar.showSnackbar(text)
    }
    var selectedTab by rememberSaveable { mutableIntStateOf(TopLevelDestination.TODAY.ordinal) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val isTopLevel = currentRoute == MAIN_ROUTE

    // Keep top-level ViewModels alive so tab changes do not start database work
    // while both screens are being composed for the transition.
    val todayViewModel: TodayViewModel = viewModel(
        factory = TodayViewModel.factory(
            scheduleRepository = container.scheduleRepository,
            supplementRepository = container.supplementRepository,
            workoutRepository = container.workoutRepository,
            cycleSettingsRepository = container.cycleSettingsRepository,
            progressRepository = container.progressRepository,
            calendarRepository = container.calendarRepository,
            sessionProgressStore = container.sessionProgressStore,
        ),
    )
    val calendarViewModel: CalendarViewModel = viewModel(
        factory = CalendarViewModel.factory(
            container.calendarRepository,
            container.cycleSettingsRepository,
            container.reminderScheduler,
            container.workoutRepository,
            container.clock,
        ),
    )
    val progressViewModel: com.example.repsgrams.ui.progress.ProgressViewModel = viewModel(
        factory = com.example.repsgrams.ui.progress.ProgressViewModel.provideFactory(
            container.progressRepository,
            container.cycleSettingsRepository,
            container.supplementRepository,
            container.healthConnectManager,
        ),
    )
    val settingsViewModel: com.example.repsgrams.ui.settings.SettingsViewModel = viewModel(
        factory = com.example.repsgrams.ui.settings.SettingsViewModel.factory(
            container.cycleSettingsRepository,
            container.reminderScheduler,
        ),
    )

    LaunchedEffect(notificationTarget) {
        if (notificationTarget != null) {
            if (currentRoute != MAIN_ROUTE) {
                navController.popBackStack(MAIN_ROUTE, inclusive = false)
            }
            selectedTab = TopLevelDestination.TODAY.ordinal
        }
    }

    LaunchedEffect(openSessionId) {
        val id = openSessionId ?: return@LaunchedEffect
        navController.navigate("session/$id") {
            launchSingleTop = true
        }
        onOpenSessionHandled()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(backupSnackbar) },
        bottomBar = {
            if (isTopLevel) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .windowInsetsPadding(WindowInsets.navigationBars),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TopLevelDestination.entries.forEachIndexed { index, destination ->
                            val selected = selectedTab == index
                            val contentColor = if (selected) {
                                MaterialTheme.colorScheme.onBackground
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .semantics { this.selected = selected }
                                    .clickable(
                                        enabled = !selected,
                                        role = Role.Tab,
                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                        indication = null,
                                        onClick = { selectedTab = index },
                                    ),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Icon(
                                    imageVector = if (selected) destination.iconFilled else destination.iconOutlined,
                                    contentDescription = null,
                                    tint = contentColor,
                                )
                                Text(
                                    text = destination.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = contentColor,
                                )
                            }
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        val detailSlideSpec = tween<androidx.compose.ui.unit.IntOffset>(
            durationMillis = 220,
            easing = EaseOutCubic,
        )

        NavHost(
            navController = navController,
            startDestination = MAIN_ROUTE,
            modifier = Modifier.padding(bottom = innerPadding.calculateBottomPadding()),
            enterTransition = {
                androidx.compose.animation.slideInHorizontally(
                    initialOffsetX = { width -> width },
                    animationSpec = detailSlideSpec,
                )
            },
            exitTransition = {
                androidx.compose.animation.slideOutHorizontally(
                    targetOffsetX = { width -> -width / 3 },
                    animationSpec = detailSlideSpec,
                )
            },
            popEnterTransition = {
                androidx.compose.animation.slideInHorizontally(
                    initialOffsetX = { width -> -width / 3 },
                    animationSpec = detailSlideSpec,
                )
            },
            popExitTransition = {
                androidx.compose.animation.slideOutHorizontally(
                    targetOffsetX = { width -> width },
                    animationSpec = detailSlideSpec,
                )
            }
        ) {
            composable(MAIN_ROUTE) {
                Crossfade(
                    targetState = selectedTab,
                    animationSpec = tween(durationMillis = 200, easing = EaseOutCubic),
                    label = "tab",
                ) { page ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        when (TopLevelDestination.entries[page]) {
                            TopLevelDestination.TODAY -> TodayRoute(
                                viewModel = todayViewModel,
                                notificationTarget = notificationTarget,
                                onNotificationHandled = onNotificationHandled,
                                onOpenSession = { sessionId -> navController.navigate("session/$sessionId") },
                                onOpenTemplates = { navController.navigate("templates") },
                                onOpenSupplements = { navController.navigate("manage_supplements") },
                            )

                            TopLevelDestination.CALENDAR -> CalendarRoute(
                                viewModel = calendarViewModel,
                                onOpenSession = { sessionId -> navController.navigate("session/$sessionId") },
                            )

                            TopLevelDestination.PROGRESS ->
                                com.example.repsgrams.ui.progress.ProgressRoute(progressViewModel)

                            TopLevelDestination.SETTINGS ->
                                com.example.repsgrams.ui.settings.SettingsRoute(
                                    viewModel = settingsViewModel,
                                    onNavigateToTemplates = { navController.navigate("templates") },
                                    onNavigateToExercises = { navController.navigate("exercises") },
                                    onNavigateToExerciseLibrary = { navController.navigate("exercise_library") },
                                    onNavigateToSupplements = { navController.navigate("manage_supplements") },
                                    onExportData = { uri ->
                                        scope.launch {
                                            backupNotice = BackupNotice(System.nanoTime(), "Exporting...")
                                            val message = container.backupManager.exportDatabaseToZip(uri)
                                            backupNotice = BackupNotice(System.nanoTime(), message)
                                        }
                                    },
                                    onImportData = { uri ->
                                        scope.launch {
                                            backupNotice = BackupNotice(System.nanoTime(), "Importing...")
                                            val message = container.backupManager.importDatabaseFromZip(uri)
                                            backupNotice = BackupNotice(System.nanoTime(), message)
                                        }
                                    },
                                )
                        }
                    }
                }
            }
            composable("templates") {
                val editorViewModel: com.example.repsgrams.ui.settings.ProgramEditorViewModel = viewModel(
                    factory = com.example.repsgrams.ui.settings.ProgramEditorViewModel.factory(container.workoutRepository, container.cycleSettingsRepository)
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
                    factory = com.example.repsgrams.ui.settings.ProgramEditorViewModel.factory(container.workoutRepository, container.cycleSettingsRepository)
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
                    factory = com.example.repsgrams.ui.settings.ProgramEditorViewModel.factory(container.workoutRepository, container.cycleSettingsRepository)
                )
                com.example.repsgrams.ui.settings.BlockEditorRoute(
                    blockId = blockId,
                    viewModel = editorViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
                        composable("manage_supplements") {
                val supplementsViewModel: com.example.repsgrams.ui.settings.ManageSupplementsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = com.example.repsgrams.ui.settings.ManageSupplementsViewModel.provideFactory(container.supplementRepository)
                )
                com.example.repsgrams.ui.settings.ManageSupplementsRoute(
                    viewModel = supplementsViewModel,
                    onNavigateBack = { navController.popBackStack() }
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
            composable("exercise_library") {
                val exViewModel: com.example.repsgrams.ui.settings.ExerciseDictionaryViewModel = viewModel(
                    factory = com.example.repsgrams.ui.settings.ExerciseDictionaryViewModel.factory(container.workoutRepository)
                )
                val exercises by exViewModel.exercises.collectAsStateWithLifecycle()
                com.example.repsgrams.ui.settings.ExerciseLibraryScreen(
                    exercises = exercises,
                    onBack = { navController.popBackStack() },
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
                RepsGramsTheme(themeMode = ThemeMode.DARK) {
                    WorkoutSessionRoute(sessionViewModel) {
                        selectedTab = TopLevelDestination.TODAY.ordinal
                        navController.popBackStack(MAIN_ROUTE, inclusive = false)
                    }
                }
            }
        }
    }
}

private data class BackupNotice(val token: Long, val text: String)
