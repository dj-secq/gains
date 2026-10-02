package com.example.repsgrams.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.launch
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import com.example.repsgrams.ui.theme.CanvasDark
import com.example.repsgrams.ui.theme.LocalDarkTheme
import com.example.repsgrams.ui.theme.SystemBars
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
    val sessionOnTop = currentRoute?.startsWith("session/") == true
    // The session covers the board with an opaque black root on the first frame.
    SystemBars(dark = sessionOnTop || LocalDarkTheme.current)

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
    ) {
        val detailSlideSpec = tween<androidx.compose.ui.unit.IntOffset>(
            durationMillis = 220,
            easing = EaseOutCubic,
        )

        NavHost(
            navController = navController,
            startDestination = MAIN_ROUTE,
            modifier = Modifier.fillMaxSize().clipToBounds(),
            enterTransition = {
                if (isSessionMove(initialState.destination.route, targetState.destination.route)) {
                    EnterTransition.None
                } else {
                    slideInHorizontally(
                        initialOffsetX = { width -> width },
                        animationSpec = detailSlideSpec,
                    )
                }
            },
            exitTransition = {
                if (isSessionMove(initialState.destination.route, targetState.destination.route)) {
                    ExitTransition.None
                } else {
                    slideOutHorizontally(
                        targetOffsetX = { width -> -width / 3 },
                        animationSpec = detailSlideSpec,
                    )
                }
            },
            popEnterTransition = {
                if (isSessionMove(initialState.destination.route, targetState.destination.route)) {
                    EnterTransition.None
                } else {
                    slideInHorizontally(
                        initialOffsetX = { width -> -width / 3 },
                        animationSpec = detailSlideSpec,
                    )
                }
            },
            popExitTransition = {
                if (isSessionMove(initialState.destination.route, targetState.destination.route)) {
                    ExitTransition.None
                } else {
                    slideOutHorizontally(
                        targetOffsetX = { width -> width },
                        animationSpec = detailSlideSpec,
                    )
                }
            },
            predictivePopEnterTransition = { _ ->
                if (isSessionMove(initialState.destination.route, targetState.destination.route)) {
                    EnterTransition.None
                } else {
                    slideInHorizontally(
                        initialOffsetX = { width -> -width / 3 },
                        animationSpec = detailSlideSpec,
                    )
                }
            },
            predictivePopExitTransition = { _ ->
                if (isSessionMove(initialState.destination.route, targetState.destination.route)) {
                    ExitTransition.None
                } else {
                    slideOutHorizontally(
                        targetOffsetX = { width -> width },
                        animationSpec = detailSlideSpec,
                    )
                }
            },
        ) {
            composable(MAIN_ROUTE) {
                // Crossfade composes the incoming screen in the click, then starts
                // the fade on a later frame, and drops the screen when the fade ends.
                // Kept tabs are already built, so this 200ms ease-out starts immediately.
                val retainedTabs = remember {
                    mutableStateListOf(TopLevelDestination.entries[selectedTab])
                }
                LaunchedEffect(selectedTab) {
                    val selected = TopLevelDestination.entries[selectedTab]
                    if (selected !in retainedTabs) retainedTabs.add(selected)
                }
                LaunchedEffect(Unit) {
                    withFrameNanos { }
                    TopLevelDestination.entries.forEach { destination ->
                        if (destination !in retainedTabs) retainedTabs.add(destination)
                    }
                }
                val tabSpec = remember { tween<Float>(durationMillis = 200, easing = EaseOutCubic) }
                val fade = remember { Animatable(1f) }
                var baseTab by remember { mutableIntStateOf(selectedTab) }
                var topTab by remember { mutableIntStateOf(selectedTab) }
                LaunchedEffect(selectedTab) {
                    if (selectedTab == topTab && (selectedTab == baseTab || fade.value >= 0.99f)) {
                        return@LaunchedEffect
                    }
                    val under = if (fade.value >= 0.99f || baseTab == topTab) topTab else baseTab
                    fade.snapTo(0f)
                    baseTab = under
                    topTab = selectedTab
                    if (baseTab == topTab) {
                        fade.snapTo(1f)
                        return@LaunchedEffect
                    }
                    fade.animateTo(1f, tabSpec)
                    if (topTab == selectedTab) baseTab = selectedTab
                }
                Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    retainedTabs.sortedBy { it.ordinal }.forEach { destination ->
                        key(destination) {
                            val covering = topTab != baseTab && destination.ordinal == topTab
                            val alpha = when {
                                covering -> fade.value
                                destination.ordinal == baseTab || destination.ordinal == topTab -> 1f
                                else -> 0f
                            }
                            val selected = destination.ordinal == topTab
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .zIndex(if (destination.ordinal == topTab) 1f else 0f)
                                    .graphicsLayer { this.alpha = alpha }
                                    .then(if (selected) Modifier else Modifier.clearAndSetSemantics {}),
                            ) {
                                when (destination) {
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
                                if (!selected) {
                                    Box(
                                        Modifier
                                            .matchParentSize()
                                            .clickable(
                                                interactionSource = remember { MutableInteractionSource() },
                                                indication = null,
                                                onClick = {},
                                            ),
                                    )
                                }
                            }
                        }
                    }
                }
                    MainTabBar(selectedTab = selectedTab, onSelect = { selectedTab = it })
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
                com.example.repsgrams.ui.settings.ExerciseDictionaryRoute(
                    viewModel = exViewModel,
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
                RepsGramsTheme(themeMode = ThemeMode.DARK, manageSystemBars = false) {
                    val appear = remember { Animatable(0f) }
                    LaunchedEffect(Unit) {
                        appear.animateTo(1f, tween(durationMillis = 200, easing = EaseOutCubic))
                    }
                    Box(Modifier.fillMaxSize().background(CanvasDark)) {
                        Box(
                            Modifier.fillMaxSize().graphicsLayer {
                                alpha = appear.value
                                translationY = (1f - appear.value) * 8.dp.toPx()
                            },
                        ) {
                            WorkoutSessionRoute(sessionViewModel) {
                                selectedTab = TopLevelDestination.TODAY.ordinal
                                navController.popBackStack(MAIN_ROUTE, inclusive = false)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MainTabBar(selectedTab: Int, onSelect: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface)) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TopLevelDestination.entries.forEachIndexed { index, destination ->
                val selected = selectedTab == index
                val contentColor = if (selected) {
                    MaterialTheme.colorScheme.onSurface
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
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { onSelect(index) },
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = if (selected) destination.iconFilled else destination.iconOutlined,
                        contentDescription = destination.label,
                        tint = contentColor,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = destination.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            lineHeight = 12.sp,
                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                        ),
                        color = contentColor,
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

private fun isSessionMove(fromRoute: String?, toRoute: String?): Boolean =
    fromRoute?.startsWith("session/") == true || toRoute?.startsWith("session/") == true

private data class BackupNotice(val token: Long, val text: String)
