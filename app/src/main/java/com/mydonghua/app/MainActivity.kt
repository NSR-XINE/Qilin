package com.mydonghua.app

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import com.mydonghua.app.ui.theme.ColorOSMotion
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import com.mydonghua.app.ui.GlassBackground
import com.mydonghua.app.ui.GlassHeader
import com.mydonghua.app.ui.LiquidGlassNavBar
import com.mydonghua.app.ui.LocalHazeState
import com.mydonghua.app.ui.LocalHeaderBackdrop
import com.mydonghua.app.ui.LocalLayerBackdrop
import com.mydonghua.app.ui.MenuGlassPanel
import com.mydonghua.app.ui.Spacing
import com.mydonghua.app.ui.home.HomeScreen
import com.mydonghua.app.ui.screens.*
import com.mydonghua.app.ui.common.ColorOSDialog
import com.mydonghua.app.ui.common.AppLoadingScreen
import com.mydonghua.app.ui.theme.MyDonghuaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()
    private val showsViewModel: ShowsViewModel by viewModels()
    private val searchViewModel: SearchViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    @OptIn(ExperimentalHazeMaterialsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissionsOnStart()

        setContent {
            val state by homeViewModel.uiState.collectAsState()
            val settingsState by settingsViewModel.uiState.collectAsState()
            val allShows by showsViewModel.allShows.collectAsState()
            val showsLoading by showsViewModel.isLoading.collectAsState()
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route ?: "home"
            var showExitDialog by remember { mutableStateOf(false) }
            var isDrawerOpen by remember { mutableStateOf(false) }
            var isSearchOpen by remember { mutableStateOf(false) }
            var isAppLoading by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                val startTime = System.currentTimeMillis()
                val readyFlow = combine(
                    homeViewModel.uiState,
                    showsViewModel.isLoading
                ) { home, sLoading ->
                    !home.isLoading && !sLoading
                }
                withTimeoutOrNull(4500L) {
                    readyFlow.filter { it }.first()
                }
                val elapsed = System.currentTimeMillis() - startTime
                val minDisplayTime = 2600L
                if (elapsed < minDisplayTime) {
                    delay(minDisplayTime - elapsed)
                }
                isAppLoading = false
            }

            val showGlobalHeader = currentRoute in listOf("home", "airing", "complete", "stopped", "favorites")
            val navBackdrop = rememberLayerBackdrop()
            val contentBackdrop = rememberLayerBackdrop()
            val headerBackdrop = rememberCombinedBackdrop(contentBackdrop, navBackdrop)
            val appHazeState = remember { HazeState() }

            val launchWatch: (String) -> Unit = remember(allShows) {
                { url: String ->
                    val targetShow = allShows.find { it.watchUrl == url }
                    val intent = Intent(this@MainActivity, WatchActivity::class.java).apply {
                        putExtra("watch_url", url)
                        putExtra("show_title", targetShow?.title ?: "Donghua Episode")
                    }
                    startActivity(intent)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        overrideActivityTransition(
                            Activity.OVERRIDE_TRANSITION_OPEN,
                            R.anim.oplus_open_slide_enter,
                            R.anim.oplus_open_slide_exit
                        )
                    } else {
                        @Suppress("DEPRECATION")
                        overridePendingTransition(R.anim.oplus_open_slide_enter, R.anim.oplus_open_slide_exit)
                    }
                }
            }

            androidx.activity.compose.BackHandler(enabled = !isSearchOpen && !isAppLoading) {
                when {
                    isDrawerOpen -> isDrawerOpen = false
                    currentRoute != "home" -> navController.popBackStack("home", inclusive = false)
                    else -> showExitDialog = true
                }
            }

            MyDonghuaTheme(themeMode = settingsState.themeMode, accentColor = settingsState.accentColor) {
                if (showExitDialog) {
                    ColorOSDialog(
                        onDismissRequest = { showExitDialog = false },
                        title = "Exit App",
                        message = "Are you sure you want to exit?",
                        confirmText = "Exit",
                        dismissText = "Cancel",
                        confirmButtonColor = Color(0xFF0082FB),
                        onConfirm = {
                            showExitDialog = false
                            finish()
                        },
                        onDismiss = { showExitDialog = false }
                    )
                }
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .haze(appHazeState, style = HazeMaterials.ultraThin())
                        ) {
                            if (settingsState.themeMode.lowercase() != "light") {
                                StarsBackground()
                            }
                        }
                        CompositionLocalProvider(
                            LocalHazeState provides appHazeState,
                            LocalLayerBackdrop provides contentBackdrop,
                            LocalHeaderBackdrop provides contentBackdrop
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .layerBackdrop(contentBackdrop)
                            ) {
                                GlassBackground(modifier = Modifier.fillMaxSize())
                            }
                            Scaffold(
                                containerColor = Color.Transparent,
                                modifier = Modifier.fillMaxSize(),
                                topBar = {
                                    if (!isSearchOpen && showGlobalHeader) {
                                        val headerTitle = when (currentRoute) {
                                            "home" -> "My Donghua"
                                            "airing" -> "Airing Schedule"
                                            "complete" -> "Completed"
                                            "stopped" -> "Stopped"
                                            "favorites" -> "Favorites"
                                            else -> ""
                                        }
                                        GlassHeader(
                                            title = headerTitle,
                                            backdrop = headerBackdrop,
                                            isDark = settingsState.themeMode.lowercase() != "light",
                                            onDrawerOpen = { isDrawerOpen = true },
                                        )
                                    }
                                },
                                bottomBar = {
                                    if (showGlobalHeader) {
                                        LiquidGlassNavBar(
                                            currentRoute = currentRoute,
                                            isDark = settingsState.themeMode.lowercase() != "light",
                                            backdrop = headerBackdrop,
                                            isSearchOpen = isSearchOpen,
                                            onSearchClick = {
                                                isSearchOpen = !isSearchOpen
                                            },
                                            onNavigate = { route ->
                                                val currentDest = navController.currentBackStackEntry?.destination?.route
                                                if (route == currentDest) {
                                                    // Already on this tab - ignore re-tap
                                                    return@LiquidGlassNavBar
                                                }
                                                when (route) {
                                                    "home" -> {
                                                        if (currentDest != "home") {
                                                            navController.popBackStack("home", inclusive = false)
                                                        }
                                                    }
                                                    "add_edit_show" -> navController.navigate("add_edit_show")
                                                    else -> navController.navigate(route) {
                                                        popUpTo("home") {
                                                            saveState = true
                                                        }
                                                        launchSingleTop = true
                                                        restoreState = true
                                                    }
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = Spacing.sm, start = Spacing.sm, end = Spacing.sm)
                                        )
                                    }
                                }
                            ) { innerPadding ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .layerBackdrop(navBackdrop)
                                ) {
                            NavHost(
                                navController = navController,
                                startDestination = "home",
                                modifier = Modifier.fillMaxSize(),
                                enterTransition = {
                                    val from = initialState.destination.route
                                    val to = targetState.destination.route
                                    if (from == to) {
                                        EnterTransition.None
                                    } else if (from in TAB_ROUTES && to in TAB_ROUTES) {
                                        fadeIn(animationSpec = ColorOSMotion.TabFadeInSpec)
                                    } else {
                                        slideInHorizontally(
                                            animationSpec = ColorOSMotion.NavSlideInSpec,
                                            initialOffsetX = { it }
                                        ) + fadeIn(
                                            animationSpec = ColorOSMotion.NavFadeInSpec
                                        )
                                    }
                                },
                                exitTransition = {
                                    val from = initialState.destination.route
                                    val to = targetState.destination.route
                                    if (from == to) {
                                        ExitTransition.None
                                    } else if (from in TAB_ROUTES && to in TAB_ROUTES) {
                                        fadeOut(animationSpec = ColorOSMotion.TabFadeOutSpec)
                                    } else {
                                        slideOutHorizontally(
                                            animationSpec = ColorOSMotion.NavSlideOutSpec,
                                            targetOffsetX = { -it / 4 }
                                        ) + fadeOut(
                                            animationSpec = ColorOSMotion.NavFadeOutSpec,
                                            targetAlpha = 0.5f
                                        )
                                    }
                                },
                                popEnterTransition = {
                                    val from = initialState.destination.route
                                    val to = targetState.destination.route
                                    if (from == to) {
                                        EnterTransition.None
                                    } else if (from in TAB_ROUTES && to in TAB_ROUTES) {
                                        fadeIn(animationSpec = ColorOSMotion.TabFadeInSpec)
                                    } else {
                                        slideInHorizontally(
                                            animationSpec = ColorOSMotion.NavSlideInSpec,
                                            initialOffsetX = { -it / 4 }
                                        ) + fadeIn(
                                            animationSpec = ColorOSMotion.NavFadeInSpec
                                        )
                                    }
                                },
                                popExitTransition = {
                                    val from = initialState.destination.route
                                    val to = targetState.destination.route
                                    if (from == to) {
                                        ExitTransition.None
                                    } else if (from in TAB_ROUTES && to in TAB_ROUTES) {
                                        fadeOut(animationSpec = ColorOSMotion.TabFadeOutSpec)
                                    } else {
                                        slideOutHorizontally(
                                            animationSpec = ColorOSMotion.NavSlideOutSpec,
                                            targetOffsetX = { it }
                                        ) + fadeOut(
                                            animationSpec = ColorOSMotion.NavFadeOutSpec
                                        )
                                    }
                                }
                            ) {
                        composable("home") {
                                HomeScreen(
                                viewModel = homeViewModel,
                                topPadding = innerPadding.calculateTopPadding(),
                                defaultStreamProvider = settingsState.defaultStreamProvider,
                                onSearchClick = {
                                    isSearchOpen = true
                                },
                                onSettingsClick = {
                                    navController.navigate("settings")
                                },
                                onShowClick = { showId ->
                                    navController.navigate("show_detail/$showId")
                                },
                                onWatchClick = launchWatch
                            )
                        }
                        composable("airing") {
                                AiringScreen(
                                viewModel = showsViewModel,
                                topPadding = innerPadding.calculateTopPadding(),
                                onWatchClick = launchWatch,
                                onEditClick = { show ->
                                    navController.navigate("add_edit_show?showId=${show.id}")
                                },
                                onCardClick = { show ->
                                    navController.navigate("show_detail/${show.id}")
                                }
                            )
                        }
                        composable("complete") {
                                CompletedScreen(
                                viewModel = showsViewModel,
                                topPadding = innerPadding.calculateTopPadding(),
                                onWatchClick = launchWatch,
                                onEditClick = { show ->
                                    navController.navigate("add_edit_show?showId=${show.id}")
                                },
                                onCardClick = { show ->
                                    navController.navigate("show_detail/${show.id}")
                                }
                            )
                        }
                        composable("stopped") {
                                StoppedScreen(
                                viewModel = showsViewModel,
                                topPadding = innerPadding.calculateTopPadding(),
                                onWatchClick = launchWatch,
                                onEditClick = { show ->
                                    navController.navigate("add_edit_show?showId=${show.id}")
                                },
                                onCardClick = { show ->
                                    navController.navigate("show_detail/${show.id}")
                                }
                            )
                        }
                        composable("favorites") {
                                FavoritesScreen(
                                viewModel = showsViewModel,
                                topPadding = innerPadding.calculateTopPadding(),
                                onWatchClick = launchWatch,
                                onEditClick = { show ->
                                    navController.navigate("add_edit_show?showId=${show.id}")
                                },
                                onCardClick = { show ->
                                    navController.navigate("show_detail/${show.id}")
                                }
                            )
                        }
                        composable(
                            "add_edit_show?showId={showId}",
                            arguments = listOf(
                                androidx.navigation.navArgument("showId") {
                                    type = androidx.navigation.NavType.StringType
                                    nullable = true
                                    defaultValue = null
                                }
                            )
                        ) { backStackEntry ->
                            val showIdArg = backStackEntry.arguments?.getString("showId")
                            AddEditShowScreen(
                                showId = showIdArg,
                                viewModel = showsViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("show_detail/{showId}") { backStackEntry ->
                                val showIdArg = backStackEntry.arguments?.getString("showId") ?: ""
                                ShowDetailScreen(
                                showId = showIdArg,
                                viewModel = showsViewModel,
                                defaultStreamProvider = settingsState.defaultStreamProvider,
                                onWatchClick = launchWatch,
                                onEditClick = { show ->
                                    navController.navigate("add_edit_show?showId=${show.id}")
                                },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("settings") {
                            SettingsScreen(
                                viewModel = settingsViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onSoftwareUpdateClick = { navController.navigate("software_update") }
                            )
                        }
                        composable("stats") {
                                StatsScreen(
                                showsViewModel = showsViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("calendar") {
                                CalendarScreen(
                                showsViewModel = showsViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("history") {
                                HistoryScreen(
                                showsViewModel = showsViewModel,
                                onShowClick = { showId ->
                                    navController.navigate("show_detail/$showId")
                                },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("collections") {
                            CollectionsScreen(
                                showsViewModel = showsViewModel,
                                onShowClick = { showId ->
                                    navController.navigate("show_detail/$showId")
                                },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("downloads") {
                            com.mydonghua.app.ui.screens.DownloadsScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("software_update") {
                            ColorOSUpdateScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = isSearchOpen,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = ColorOSMotion.SheetIntOffset
                    ) + fadeIn(animationSpec = ColorOSMotion.NavFadeInSpec),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = ColorOSMotion.SheetExitIntOffset
                    ) + fadeOut(animationSpec = ColorOSMotion.NavFadeOutSpec),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) { }
                            .nestedScroll(
                                remember {
                                    object : NestedScrollConnection {
                                        override fun onPostScroll(
                                            consumed: Offset,
                                            available: Offset,
                                            source: NestedScrollSource
                                        ): Offset = available
                                    }
                                }
                            )
                    ) {
                        SearchScreen(
                            viewModel = searchViewModel,
                            showsViewModel = showsViewModel,
                            onWatchClick = launchWatch,
                            onShowClick = { showId ->
                                isSearchOpen = false
                                navController.navigate("show_detail/$showId")
                            },
                            onNavigateBack = {
                                isSearchOpen = false
                            }
                        )
                    }
                }

            AnimatedVisibility(
                visible = isDrawerOpen,
                modifier = Modifier
                    .zIndex(1000f)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        isDrawerOpen = false
                    },
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = ColorOSMotion.SheetIntOffset
                ) + fadeIn(
                    animationSpec = ColorOSMotion.NavFadeInSpec
                ),
                exit = slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = ColorOSMotion.SheetExitIntOffset
                ) + fadeOut(
                    animationSpec = ColorOSMotion.NavFadeOutSpec
                )
            ) {
                val menuShape = RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp)
                val isAmoledMode = settingsState.themeMode.lowercase() == "amoled"
                val isLightMode = settingsState.themeMode.lowercase() == "light"

                MenuGlassPanel(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            // Consumes touch events on menu panel so background touches are disabled
                        },
                    shape = menuShape,
                    isAmoledMode = isAmoledMode,
                    isLightMode = isLightMode
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Navigation",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            // Frosted Liquid Close Button with ColorOS 17 elastic bounce
                            val closeInteraction = remember { MutableInteractionSource() }
                            val isClosePressed by closeInteraction.collectIsPressedAsState()
                            val closeScale by animateFloatAsState(
                                targetValue = if (isClosePressed) 0.88f else 1f,
                                animationSpec = ColorOSMotion.TouchBounceSpring,
                                label = "closeScale"
                            )

                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .graphicsLayer {
                                        scaleX = closeScale
                                        scaleY = closeScale
                                    }
                                    .clip(CircleShape)
                                    .background(
                                        if (isLightMode) Color.Black.copy(alpha = 0.06f)
                                        else Color.White.copy(alpha = 0.10f)
                                    )
                                    .clickable(
                                        interactionSource = closeInteraction,
                                        indication = null,
                                        onClick = { isDrawerOpen = false }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Close Menu",
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        // Quick Tiles (2x2 Grid)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OxygenDrawerTile(
                                    title = "Statistics",
                                    iconRes = R.drawable.ic_stats,
                                    gradient = listOf(Color(0xFFFF5722), Color(0xFFFF8A65)),
                                    isSelected = currentRoute == "stats",
                                    isLight = isLightMode,
                                    modifier = Modifier.weight(1f),
                                    onClick = { isDrawerOpen = false; navController.navigate("stats") }
                                )
                                OxygenDrawerTile(
                                    title = "Schedule",
                                    iconRes = R.drawable.ic_calendar,
                                    gradient = listOf(Color(0xFF0288D1), Color(0xFF26C6DA)),
                                    isSelected = currentRoute == "calendar",
                                    isLight = isLightMode,
                                    modifier = Modifier.weight(1f),
                                    onClick = { isDrawerOpen = false; navController.navigate("calendar") }
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OxygenDrawerTile(
                                    title = "Collections",
                                    iconRes = R.drawable.ic_collections,
                                    gradient = listOf(Color(0xFF7B1FA2), Color(0xFFBA68C8)),
                                    isSelected = currentRoute == "collections",
                                    isLight = isLightMode,
                                    modifier = Modifier.weight(1f),
                                    onClick = { isDrawerOpen = false; navController.navigate("collections") }
                                )
                                OxygenDrawerTile(
                                    title = "Downloads",
                                    iconRes = R.drawable.ic_history,
                                    gradient = listOf(Color(0xFF00897B), Color(0xFF26A69A)),
                                    isSelected = currentRoute == "downloads",
                                    isLight = isLightMode,
                                    modifier = Modifier.weight(1f),
                                    onClick = { isDrawerOpen = false; navController.navigate("downloads") }
                                )
                            }
                        }

                        // Grouped Island List (Borderless)
                        val listCardShape = RoundedCornerShape(22.dp)
                        val listCardBg = if (isLightMode) Color.White.copy(alpha = 0.70f)
                        else Color.White.copy(alpha = 0.05f)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(listCardShape)
                                .background(listCardBg, listCardShape)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                OxygenDrawerListItem(
                                    title = "Watch History",
                                    iconRes = R.drawable.ic_history,
                                    iconGradient = listOf(Color(0xFF334155), Color(0xFF64748B)),
                                    isSelected = currentRoute == "history",
                                    isLight = isLightMode,
                                    onClick = { isDrawerOpen = false; navController.navigate("history") }
                                )
                                HorizontalDivider(
                                    color = if (isLightMode) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.08f),
                                    modifier = Modifier.padding(start = 68.dp, end = 16.dp)
                                )
                                OxygenDrawerListItem(
                                    title = "Seasonal View",
                                    iconRes = R.drawable.ic_seasonal,
                                    iconGradient = listOf(Color(0xFFD97706), Color(0xFFF59E0B)),
                                    isSelected = currentRoute == "seasonal",
                                    isLight = isLightMode,
                                    onClick = { isDrawerOpen = false }
                                )
                                HorizontalDivider(
                                    color = if (isLightMode) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.08f),
                                    modifier = Modifier.padding(start = 68.dp, end = 16.dp)
                                )
                                OxygenDrawerListItem(
                                    title = "Settings",
                                    iconRes = R.drawable.ic_settings,
                                    iconGradient = listOf(Color(0xFFE50914), Color(0xFFFF5252)),
                                    isSelected = currentRoute == "settings",
                                    isLight = isLightMode,
                                    onClick = { isDrawerOpen = false; navController.navigate("settings") }
                                )
                                HorizontalDivider(
                                    color = if (isLightMode) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.08f),
                                    modifier = Modifier.padding(start = 68.dp, end = 16.dp)
                                )
                                OxygenDrawerListItem(
                                    title = "Software update",
                                    subtitle = "ColorOS 17 • 3.53 GB",
                                    iconRes = R.drawable.ic_settings,
                                    iconGradient = listOf(Color(0xFF0082FB), Color(0xFF52A9FF)),
                                    isSelected = currentRoute == "software_update",
                                    isLight = isLightMode,
                                    onClick = { isDrawerOpen = false; navController.navigate("software_update") }
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = isAppLoading,
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(9999f),
                enter = EnterTransition.None,
                exit = fadeOut(
                    animationSpec = tween(
                        durationMillis = 600,
                        easing = ColorOSMotion.EmphasizedAccelerate
                    )
                )
            ) {
                AppLoadingScreen(
                    isDark = settingsState.themeMode.lowercase() != "light",
                    isAmoled = settingsState.themeMode.lowercase() == "amoled"
                )
            }
        }
    }
}
}
}
}
}

    private fun requestPermissionsOnStart() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        requestNotificationPermission()
        requestStoragePermissionOnce()
    }

    fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
            }
        }
    }

    private fun currentStoragePermission(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            android.Manifest.permission.READ_MEDIA_IMAGES
        } else {
            android.Manifest.permission.READ_EXTERNAL_STORAGE
        }

    fun hasStoragePermission(): Boolean =
        ContextCompat.checkSelfPermission(this, currentStoragePermission()) == PackageManager.PERMISSION_GRANTED

    private fun requestStoragePermissionOnce() {
        lifecycleScope.launch {
            if (settingsViewModel.storagePermissionPrompted()) {
                return@launch
            }
            settingsViewModel.markStoragePermissionPrompted()
            if (hasStoragePermission()) {
                return@launch
            }
            ActivityCompat.requestPermissions(
                this@MainActivity,
                arrayOf(currentStoragePermission()),
                STORAGE_PERMISSION_CODE
            )
        }
    }

    companion object {
        const val FILE_CHOOSER_REQUEST_CODE = 1001
        private const val NOTIFICATION_PERMISSION_CODE = 1002
        private const val STORAGE_PERMISSION_CODE = 1003
        private val TAB_ROUTES = listOf("home", "airing", "complete", "stopped", "favorites")
    }
}

@Composable
private fun StarsBackground(modifier: Modifier = Modifier) {
    val starsColor = MaterialTheme.colorScheme.onBackground
    androidx.compose.foundation.Canvas(modifier = modifier.fillMaxSize()) {
        val random = java.util.Random(12345L)
        val width = size.width
        val height = size.height
        if (width > 0 && height > 0) {
            for (i in 0 until 60) {
                val x = random.nextFloat() * width
                val y = random.nextFloat() * height
                val radius = random.nextFloat() * 1.5f + 0.5f
                val alpha = random.nextFloat() * 0.7f + 0.3f
                drawCircle(
                    color = starsColor,
                    radius = radius,
                    center = Offset(x, y),
                    alpha = alpha
                )
            }
        }
    }
}

@Composable
private fun OxygenDrawerTile(
    title: String,
    iconRes: Int,
    gradient: List<Color>,
    isSelected: Boolean,
    isLight: Boolean,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = ColorOSMotion.TouchBounceSpring,
        label = "tileScale"
    )

    val shape = RoundedCornerShape(22.dp)
    val cardBg = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isLight) 0.35f else 0.25f)
    } else {
        if (isLight) Color.White.copy(alpha = 0.75f) else Color.White.copy(alpha = 0.05f)
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .height(if (subtitle != null) 108.dp else 92.dp)
            .clip(shape)
            .background(cardBg, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Continuous Curvature Squircle Icon Badge
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(Brush.linearGradient(gradient)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = title,
                        tint = Color.White,
                        modifier = Modifier.size(19.dp)
                    )
                }

                if (isSelected) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
                        )
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    fontSize = 14.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun OxygenDrawerListItem(
    title: String,
    iconRes: Int,
    iconGradient: List<Color>,
    isSelected: Boolean,
    isLight: Boolean,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val itemScale by animateFloatAsState(
        targetValue = if (isPressed) 0.975f else 1f,
        animationSpec = ColorOSMotion.TouchBounceSpring,
        label = "itemScale"
    )
    val arrowOffset by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isPressed) 4.dp else 0.dp,
        animationSpec = ColorOSMotion.SnappyDp,
        label = "arrowOffset"
    )
    val bgAlpha = if (isPressed) {
        if (isLight) 0.12f else 0.10f
    } else if (isSelected) {
        if (isLight) 0.08f else 0.07f
    } else {
        0f
    }

    val itemShape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = itemScale
                scaleY = itemScale
            }
            .clip(itemShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = bgAlpha), itemShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = if (subtitle != null) 12.dp else 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Brush.linearGradient(iconGradient)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = iconRes),
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        fontSize = 11.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Box(
                modifier = Modifier
                    .offset(x = arrowOffset)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        if (isLight) Color.Black.copy(alpha = 0.04f)
                        else Color.White.copy(alpha = 0.06f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "›",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
