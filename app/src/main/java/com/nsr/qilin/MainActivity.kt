package com.nsr.qilin

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import com.nsr.qilin.ui.theme.ColorOSMotion
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
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Person
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import com.nsr.qilin.data.service.AniListService
import com.nsr.qilin.data.service.AniListUser
import com.nsr.qilin.ui.LocalNavAnimatedVisibilityScope
import com.nsr.qilin.ui.LocalSharedTransitionScope
import com.nsr.qilin.ui.SharedElementDestination
import com.nsr.qilin.ui.scopedComposable
import com.nsr.qilin.ui.theme.colorOSPressBounce
import com.nsr.qilin.ui.theme.Haptics
import com.nsr.qilin.ui.theme.hapticOnTap
import com.nsr.qilin.ui.theme.RevealOnEnter
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
import dev.chrisbanes.haze.LocalHazeStyle
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import com.nsr.qilin.ui.DynamicShowBackground
import com.nsr.qilin.ui.GlassBackground
import com.nsr.qilin.ui.GlassHeader
import com.nsr.qilin.ui.SharedPosterKey
import com.nsr.qilin.ui.LiquidGlassNavBar
import com.nsr.qilin.ui.LocalHazeState
import com.nsr.qilin.ui.LocalHeaderBackdrop
import com.nsr.qilin.ui.LocalLayerBackdrop
import com.nsr.qilin.ui.MenuGlassPanel
import com.nsr.qilin.ui.Spacing
import com.nsr.qilin.ui.home.HomeScreen
import com.nsr.qilin.ui.screens.*
import com.nsr.qilin.ui.common.ColorOSDialog
import com.nsr.qilin.ui.common.AppLoadingScreen
import com.nsr.qilin.ui.theme.QilinTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()
    private val showsViewModel: ShowsViewModel by viewModels()
    private val searchViewModel: SearchViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    private val pendingTargetShowId = mutableStateOf<String?>(null)

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra("target_show_id")?.let { targetId ->
            if (targetId.isNotBlank()) {
                pendingTargetShowId.value = targetId
            }
        }
    }

    @OptIn(ExperimentalHazeMaterialsApi::class, ExperimentalSharedTransitionApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        intent?.getStringExtra("target_show_id")?.let { targetId ->
            if (targetId.isNotBlank()) {
                pendingTargetShowId.value = targetId
            }
        }

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
            var drawerNavigating by remember { mutableStateOf(false) }

            LaunchedEffect(currentRoute) {
                if (drawerNavigating) {
                    delay(420)
                    drawerNavigating = false
                }
            }

            LaunchedEffect(Unit) {
                val startTime = System.currentTimeMillis()
                val readyFlow = combine(
                    homeViewModel.uiState,
                    showsViewModel.isLoading
                ) { home, sLoading ->
                    !home.isLoading && !sLoading
                }
                withTimeoutOrNull(8000L) {
                    readyFlow.filter { it }.first()
                }
                val elapsed = System.currentTimeMillis() - startTime
                val minDisplayTime = 5200L
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

            val openDetail: (String, String?) -> Unit = { showId, posterKey ->
                SharedPosterKey.current = posterKey
                navController.navigate("show_detail/$showId")
            }

            val targetShowId by remember { pendingTargetShowId }
            LaunchedEffect(targetShowId, isAppLoading) {
                if (!isAppLoading && !targetShowId.isNullOrBlank()) {
                    openDetail(targetShowId!!, null)
                    pendingTargetShowId.value = null
                }
            }

            val navigateFromDrawer: (String) -> Unit = { route ->
                isDrawerOpen = false
                if (route != currentRoute) {
                    drawerNavigating = true
                    navController.navigate(route)
                }
            }

            BackHandler(
                enabled = !isSearchOpen && !isAppLoading
            ) {
                when {
                    isDrawerOpen -> isDrawerOpen = false
                    navController.previousBackStackEntry != null -> navController.popBackStack()
                    else -> showExitDialog = true
                }
            }

            QilinTheme(themeMode = settingsState.themeMode, accentColor = settingsState.accentColor) {
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
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .hapticOnTap()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .hazeSource(appHazeState)
                        ) {
                            if (settingsState.themeMode.lowercase() != "light") {
                                StarsBackground()
                            }
                        }
                        CompositionLocalProvider(
                            LocalHazeState provides appHazeState,
                            LocalHazeStyle provides HazeMaterials.ultraThin(),
                            LocalLayerBackdrop provides contentBackdrop,
                            LocalHeaderBackdrop provides contentBackdrop
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .layerBackdrop(contentBackdrop)
                            ) {
                                val activeTrendingBg = state.activeTrendingMedia?.bannerImage
                                    ?: state.activeTrendingMedia?.coverImage

                                if (!activeTrendingBg.isNullOrBlank()) {
                                    DynamicShowBackground(
                                        imageUrl = activeTrendingBg,
                                        themeMode = settingsState.themeMode,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    GlassBackground(modifier = Modifier.fillMaxSize())
                                }
                            }
                            Scaffold(
                                containerColor = Color.Transparent,
                                modifier = Modifier.fillMaxSize(),
                                topBar = {
                                    if (!isSearchOpen && showGlobalHeader) {
                                        val headerTitle = when (currentRoute) {
                                            "home" -> "Qilin"
                                            "airing" -> "Airing Schedule"
                                            "complete" -> "Completed"
                                            "stopped" -> "Stopped"
                                            "favorites" -> "Favorites"
                                            else -> ""
                                        }
                                        val isHome = currentRoute == "home"
                                        val countdownShow = if (isHome && settingsState.showBanner) state.nextUpShow else null
                                        GlassHeader(
                                            title = headerTitle,
                                            backdrop = headerBackdrop,
                                            isDark = settingsState.themeMode.lowercase() != "light",
                                            countdownShow = countdownShow,
                                            onCountdownClick = {
                                                if (countdownShow != null) {
                                                    openDetail(countdownShow.id, null)
                                                }
                                            },
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
                            SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
                                CompositionLocalProvider(
                                    LocalSharedTransitionScope provides this
                                ) {
                            NavHost(
                                navController = navController,
                                startDestination = "home",
                                modifier = Modifier.fillMaxSize(),
                                enterTransition = {
                                    val from = initialState.destination.route
                                    val to = targetState.destination.route
                                    val toDetail = to?.startsWith("show_detail/") == true
                                    if (from == to) {
                                        EnterTransition.None
                                    } else if (drawerNavigating || toDetail) {
                                        fadeIn(animationSpec = ColorOSMotion.TabFadeInSpec)
                                    } else if (from in TAB_ROUTES && to in TAB_ROUTES) {
                                        val direction = if (TAB_ROUTES.indexOf(to) >= TAB_ROUTES.indexOf(from)) 1 else -1
                                        fadeIn(animationSpec = ColorOSMotion.TabFadeInSpec) +
                                            slideInHorizontally(
                                                animationSpec = ColorOSMotion.TabSlideInSpec,
                                                initialOffsetX = { direction * it / 18 }
                                            )
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
                                    val toDetail = to?.startsWith("show_detail/") == true
                                    if (from == to) {
                                        ExitTransition.None
                                    } else if (drawerNavigating || toDetail) {
                                        fadeOut(animationSpec = ColorOSMotion.NavFadeOutSpec)
                                    } else if (from in TAB_ROUTES && to in TAB_ROUTES) {
                                        val direction = if (TAB_ROUTES.indexOf(to) >= TAB_ROUTES.indexOf(from)) 1 else -1
                                        fadeOut(animationSpec = ColorOSMotion.TabFadeOutSpec) +
                                            slideOutHorizontally(
                                                animationSpec = ColorOSMotion.TabSlideOutSpec,
                                                targetOffsetX = { -direction * it / 18 }
                                            )
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
                                    val fromDetail = from?.startsWith("show_detail/") == true
                                    if (from == to) {
                                        EnterTransition.None
                                    } else if (fromDetail) {
                                        fadeIn(animationSpec = ColorOSMotion.TabFadeInSpec)
                                    } else if (from in TAB_ROUTES && to in TAB_ROUTES) {
                                        val direction = if (TAB_ROUTES.indexOf(to) >= TAB_ROUTES.indexOf(from)) 1 else -1
                                        fadeIn(animationSpec = ColorOSMotion.TabFadeInSpec) +
                                            slideInHorizontally(
                                                animationSpec = ColorOSMotion.TabSlideInSpec,
                                                initialOffsetX = { direction * it / 18 }
                                            )
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
                                    val fromDetail = from?.startsWith("show_detail/") == true
                                    if (from == to) {
                                        ExitTransition.None
                                    } else if (fromDetail) {
                                        fadeOut(animationSpec = ColorOSMotion.NavFadeOutSpec)
                                    } else if (from in TAB_ROUTES && to in TAB_ROUTES) {
                                        val direction = if (TAB_ROUTES.indexOf(to) >= TAB_ROUTES.indexOf(from)) 1 else -1
                                        fadeOut(animationSpec = ColorOSMotion.TabFadeOutSpec) +
                                            slideOutHorizontally(
                                                animationSpec = ColorOSMotion.TabSlideOutSpec,
                                                targetOffsetX = { -direction * it / 18 }
                                            )
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
                        scopedComposable("home") {
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
                                onShowClick = { showId, posterKey ->
                                    openDetail(showId, posterKey)
                                },
                                onWatchClick = launchWatch
                            )
                        }
                        scopedComposable("airing") {
                                AiringScreen(
                                viewModel = showsViewModel,
                                topPadding = innerPadding.calculateTopPadding(),
                                onWatchClick = launchWatch,
                                onEditClick = { show ->
                                    navController.navigate("add_edit_show?showId=${show.id}")
                                },
                                onCardClick = { show ->
                                    openDetail(show.id, "show-poster-card-${show.id}")
                                }
                            )
                        }
                        scopedComposable("complete") {
                                CompletedScreen(
                                viewModel = showsViewModel,
                                topPadding = innerPadding.calculateTopPadding(),
                                onWatchClick = launchWatch,
                                onEditClick = { show ->
                                    navController.navigate("add_edit_show?showId=${show.id}")
                                },
                                onCardClick = { show ->
                                    openDetail(show.id, "show-poster-card-${show.id}")
                                }
                            )
                        }
                        scopedComposable("stopped") {
                                StoppedScreen(
                                viewModel = showsViewModel,
                                topPadding = innerPadding.calculateTopPadding(),
                                onWatchClick = launchWatch,
                                onEditClick = { show ->
                                    navController.navigate("add_edit_show?showId=${show.id}")
                                },
                                onCardClick = { show ->
                                    openDetail(show.id, "show-poster-card-${show.id}")
                                }
                            )
                        }
                        scopedComposable("favorites") {
                                FavoritesScreen(
                                viewModel = showsViewModel,
                                topPadding = innerPadding.calculateTopPadding(),
                                onWatchClick = launchWatch,
                                onEditClick = { show ->
                                    navController.navigate("add_edit_show?showId=${show.id}")
                                },
                                onCardClick = { show ->
                                    openDetail(show.id, "show-poster-card-${show.id}")
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
                        scopedComposable("show_detail/{showId}") { backStackEntry ->
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
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("anilist") {
                            AniListScreen(
                                viewModel = settingsViewModel,
                                onNavigateBack = { navController.popBackStack() }
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
                                onNavigateBack = { navController.popBackStack() },
                                onShowClick = { show ->
                                    openDetail(show.id, null)
                                }
                            )
                        }
                        composable("history") {
                                HistoryScreen(
                                showsViewModel = showsViewModel,
                                onShowClick = { showId ->
                                    openDetail(showId, null)
                                },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("collections") {
                            CollectionsScreen(
                                showsViewModel = showsViewModel,
                                onShowClick = { showId ->
                                    openDetail(showId, null)
                                },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        composable("downloads") {
                            com.nsr.qilin.ui.screens.DownloadsScreen(
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                    }
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
                                openDetail(showId, null)
                            },
                            onNavigateBack = {
                                isSearchOpen = false
                            }
                        )
                    }
                }

            val menuShape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
            val isAmoledMode = settingsState.themeMode.lowercase() == "amoled"
            val isLightMode = settingsState.themeMode.lowercase() == "light"

            AnimatedVisibility(
                visible = isDrawerOpen,
                modifier = Modifier.zIndex(999f),
                enter = fadeIn(
                    animationSpec = tween(
                        durationMillis = 220,
                        easing = ColorOSMotion.FastOutSlowIn
                    )
                ),
                exit = fadeOut(
                    animationSpec = tween(
                        durationMillis = 180,
                        easing = ColorOSMotion.FastOutSlowIn
                    )
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = if (isLightMode) 0.35f else 0.55f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            isDrawerOpen = false
                        }
                )
            }

            AnimatedVisibility(
                visible = isDrawerOpen,
                modifier = Modifier.zIndex(1000f),
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
                var drawerShown by remember { mutableStateOf(false) }
                LaunchedEffect(Unit) { drawerShown = true }
                val drawerEnter by animateFloatAsState(
                    targetValue = if (drawerShown) 1f else 0f,
                    animationSpec = spring(dampingRatio = 0.60f, stiffness = 300f),
                    label = "drawerEnter"
                )
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    MenuGlassPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = maxHeight * 0.90f)
                            .graphicsLayer {
                                alpha = drawerEnter.coerceIn(0f, 1f)
                                val s = 0.90f + 0.10f * drawerEnter
                                scaleX = s
                                scaleY = s
                                transformOrigin = TransformOrigin(0.5f, 0f)
                            }
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                // Consumes touch events inside menu panel so background touches are disabled
                            },
                        shape = menuShape,
                        isAmoledMode = isAmoledMode,
                        isLightMode = isLightMode
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Header
                            RevealOnEnter(index = 0, distance = 16.dp, scaleFrom = 0.94f) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(Color(0xFF0082FB), Color(0xFF00D2FF))
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(id = R.drawable.ic_dragon_emblem),
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }

                                    Column {
                                        Text(
                                            text = "Navigation",
                                            fontSize = 20.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            letterSpacing = (-0.3).sp
                                        )
                                        Text(
                                            text = "Quick access & library",
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                                        )
                                    }
                                }

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
                                        .size(38.dp)
                                        .graphicsLayer {
                                            scaleX = closeScale
                                            scaleY = closeScale
                                        }
                                        .clip(CircleShape)
                                        .background(
                                            if (isLightMode) Color.Black.copy(alpha = 0.05f)
                                            else Color.White.copy(alpha = 0.08f)
                                        )
                                        .border(
                                            0.75.dp,
                                            if (isLightMode) Color.Black.copy(alpha = 0.08f)
                                            else Color.White.copy(alpha = 0.15f),
                                            CircleShape
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
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            }

                            // AniList drawer card (opens AniList screen)
                            val aniListUser by AniListService.userFlow.collectAsState()
                            RevealOnEnter(index = 1, distance = 18.dp, scaleFrom = 0.94f) {
                            AniListDrawerCard(
                                user = aniListUser,
                                isLight = isLightMode,
                                isSelected = currentRoute == "anilist",
                                onClick = { navigateFromDrawer("anilist") }
                            )
                            }

                        // Quick Tiles (2x2 Grid)
                        RevealOnEnter(index = 2, distance = 20.dp, scaleFrom = 0.94f) {
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
                                    subtitle = "Watch analytics",
                                    iconRes = R.drawable.ic_stats,
                                    gradient = listOf(Color(0xFFFF5722), Color(0xFFFF8A65)),
                                    isSelected = currentRoute == "stats",
                                    isLight = isLightMode,
                                    modifier = Modifier.weight(1f),
                                    onClick = { navigateFromDrawer("stats") }
                                )
                                OxygenDrawerTile(
                                    title = "Schedule",
                                    subtitle = "Weekly releases",
                                    iconRes = R.drawable.ic_calendar,
                                    gradient = listOf(Color(0xFF0082FB), Color(0xFF00D2FF)),
                                    isSelected = currentRoute == "calendar",
                                    isLight = isLightMode,
                                    modifier = Modifier.weight(1f),
                                    onClick = { navigateFromDrawer("calendar") }
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OxygenDrawerTile(
                                    title = "Collections",
                                    subtitle = "Custom lists",
                                    iconRes = R.drawable.ic_collections,
                                    gradient = listOf(Color(0xFF7B1FA2), Color(0xFFBA68C8)),
                                    isSelected = currentRoute == "collections",
                                    isLight = isLightMode,
                                    modifier = Modifier.weight(1f),
                                    onClick = { navigateFromDrawer("collections") }
                                )
                                OxygenDrawerTile(
                                    title = "Downloads",
                                    subtitle = "Offline episodes",
                                    iconRes = R.drawable.ic_download,
                                    gradient = listOf(Color(0xFF00897B), Color(0xFF26A69A)),
                                    isSelected = currentRoute == "downloads",
                                    isLight = isLightMode,
                                    modifier = Modifier.weight(1f),
                                    onClick = { navigateFromDrawer("downloads") }
                                )
                            }
                        }
                        }

                        // Grouped Island List with Specular Border
                        val listCardShape = RoundedCornerShape(22.dp)
                        val listCardBg = if (isLightMode) Color.White.copy(alpha = 0.80f)
                        else Color(0xFF141926).copy(alpha = 0.55f)

                        RevealOnEnter(index = 3, distance = 22.dp, scaleFrom = 0.94f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(listCardShape)
                                .background(listCardBg, listCardShape)
                                .border(
                                    width = 0.85.dp,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            Color.White.copy(alpha = if (isLightMode) 0.60f else 0.16f),
                                            Color.White.copy(alpha = if (isLightMode) 0.10f else 0.03f)
                                        )
                                    ),
                                    shape = listCardShape
                                )
                                .padding(4.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                OxygenDrawerListItem(
                                    title = "Favorites",
                                    subtitle = "Saved donghua collection",
                                    iconRes = R.drawable.ic_heart_filled,
                                    iconGradient = listOf(Color(0xFFE11D48), Color(0xFFFB7185)),
                                    isSelected = currentRoute == "favorites",
                                    isLight = isLightMode,
                                    onClick = { navigateFromDrawer("favorites") }
                                )
                                HorizontalDivider(
                                    color = if (isLightMode) Color.Black.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.06f),
                                    modifier = Modifier.padding(start = 66.dp, end = 16.dp)
                                )
                                OxygenDrawerListItem(
                                    title = "Watch History",
                                    subtitle = "Recently watched episodes",
                                    iconRes = R.drawable.ic_history,
                                    iconGradient = listOf(Color(0xFF334155), Color(0xFF64748B)),
                                    isSelected = currentRoute == "history",
                                    isLight = isLightMode,
                                    onClick = { navigateFromDrawer("history") }
                                )
                                HorizontalDivider(
                                    color = if (isLightMode) Color.Black.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.06f),
                                    modifier = Modifier.padding(start = 66.dp, end = 16.dp)
                                )
                                OxygenDrawerListItem(
                                    title = "Settings",
                                    subtitle = "Theme, streaming & player",
                                    iconRes = R.drawable.ic_settings,
                                    iconGradient = listOf(Color(0xFFE50914), Color(0xFFFF5252)),
                                    isSelected = currentRoute == "settings",
                                    isLight = isLightMode,
                                    onClick = { navigateFromDrawer("settings") }
                                )
                            }
                        }
                        }

                        // Bottom drag handle / dismiss indicator
                        RevealOnEnter(index = 4, distance = 14.dp, scaleFrom = 0.96f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 38.dp, height = 4.5.dp)
                                    .clip(RoundedCornerShape(percent = 50))
                                    .background(
                                        if (isLightMode) Color.Black.copy(alpha = 0.16f)
                                        else Color.White.copy(alpha = 0.22f)
                                    )
                            )
                        }
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
private fun AniListDrawerCard(
    user: AniListUser?,
    isLight: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.965f else 1f,
        animationSpec = ColorOSMotion.TouchBounceSpring,
        label = "aniListCardScale"
    )
    val arrowOffset by androidx.compose.animation.core.animateDpAsState(
        targetValue = if (isPressed) 3.dp else 0.dp,
        animationSpec = ColorOSMotion.SnappyDp,
        label = "aniListArrowOffset"
    )

    val accentStart = Color(0xFF02A9FF)
    val accentEnd = Color(0xFF0066CC)
    val shape = RoundedCornerShape(22.dp)

    val connectedUser = user
    val isConnected = connectedUser != null
    val avatarUrl = connectedUser?.avatarUrl
    val initial = connectedUser?.name?.take(1)?.uppercase() ?: "AL"
    val displayName = connectedUser?.name ?: "AniList"
    val subtitle = connectedUser?.let {
        "${it.episodesWatched} eps • ${it.animeCount} anime • ${it.daysWatched}"
    } ?: "Connect to sync watch progress across devices"

    val cardBrush = when {
        isSelected -> Brush.linearGradient(
            listOf(
                accentStart.copy(alpha = if (isLight) 0.18f else 0.26f),
                accentEnd.copy(alpha = if (isLight) 0.08f else 0.14f)
            )
        )
        isLight -> Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.88f), Color(0xFFEAF3FF).copy(alpha = 0.78f))
        )
        else -> Brush.linearGradient(
            listOf(Color(0xFF18202F).copy(alpha = 0.78f), Color(0xFF0F141E).copy(alpha = 0.70f))
        )
    }

    val borderBrush = if (isSelected) {
        Brush.linearGradient(
            listOf(accentStart.copy(alpha = 0.90f), Color(0xFF00D2FF).copy(alpha = 0.45f))
        )
    } else {
        Brush.linearGradient(
            listOf(
                if (isLight) Color.White.copy(alpha = 0.95f) else Color.White.copy(alpha = 0.16f),
                if (isLight) Color.Black.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.03f)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .background(cardBrush, shape)
            .border(if (isSelected) 1.25.dp else 0.85.dp, borderBrush, shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    Haptics.confirm()
                    onClick()
                }
            )
    ) {
        // Soft top sheen
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = if (isLight) 0.35f else 0.08f),
                            Color.Transparent
                        )
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(accentStart, accentEnd)))
                    .border(1.5.dp, Color.White.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (!avatarUrl.isNullOrEmpty()) {
                    SubcomposeAsyncImage(
                        model = avatarUrl,
                        contentDescription = "AniList avatar",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    ) {
                        if (painter.state is AsyncImagePainter.State.Success) {
                            SubcomposeAsyncImageContent()
                        } else {
                            Text(
                                text = initial,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp
                            )
                        }
                    }
                } else {
                    Text(
                        text = initial,
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = if (isConnected) 19.sp else 15.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = displayName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.5.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(
                                Brush.horizontalGradient(
                                    if (isConnected) listOf(accentStart, accentEnd)
                                    else listOf(
                                        accentStart.copy(alpha = 0.18f),
                                        accentEnd.copy(alpha = 0.14f)
                                    )
                                )
                            )
                            .padding(horizontal = 5.dp, vertical = 1.5.dp)
                    ) {
                        Text(
                            text = if (isConnected) "ANILIST" else "SYNC",
                            color = if (isConnected) Color.White else accentStart,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.4.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                if (isConnected) {
                    Spacer(modifier = Modifier.height(5.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF00C853))
                        )
                        Text(
                            text = "Connected",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF00C853)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .offset(x = arrowOffset)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        if (isLight) Color.Black.copy(alpha = 0.05f)
                        else Color.White.copy(alpha = 0.07f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = "Open AniList",
                    tint = if (isSelected) accentStart
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
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

    val shape = RoundedCornerShape(20.dp)
    val cardBg = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = if (isLight) 0.40f else 0.28f)
    } else {
        if (isLight) Color.White.copy(alpha = 0.85f) else Color(0xFF141926).copy(alpha = 0.55f)
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .height(if (subtitle != null) 104.dp else 92.dp)
            .clip(shape)
            .background(cardBg, shape)
            .border(
                width = 0.85.dp,
                brush = Brush.verticalGradient(
                    colors = if (isSelected) {
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.65f),
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.20f)
                        )
                    } else {
                        listOf(
                            Color.White.copy(alpha = if (isLight) 0.60f else 0.16f),
                            Color.White.copy(alpha = if (isLight) 0.12f else 0.03f)
                        )
                    }
                ),
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    Haptics.confirm()
                    onClick()
                }
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
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
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
        targetValue = if (isPressed) 0.98f else 1f,
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

    val itemShape = RoundedCornerShape(16.dp)
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
                onClick = {
                    Haptics.confirm()
                    onClick()
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = if (subtitle != null) 12.dp else 14.dp),
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
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
