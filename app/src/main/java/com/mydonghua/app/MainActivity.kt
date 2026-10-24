package com.mydonghua.app

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.TransformOrigin
import com.mydonghua.app.ui.theme.ColorOSMotion
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.platform.LocalDensity
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
import kotlin.math.roundToInt
import kotlinx.coroutines.launch
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.refraction
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.HazeMaterials
import com.mydonghua.app.ui.GlassBackground
import com.mydonghua.app.ui.GlassHeader
import com.mydonghua.app.ui.LiquidGlassNavBar
import com.mydonghua.app.ui.LocalHazeState
import com.mydonghua.app.ui.LocalLayerBackdrop
import com.mydonghua.app.ui.MenuGlassPanel
import com.mydonghua.app.ui.Spacing
import com.mydonghua.app.ui.home.HomeScreen
import com.mydonghua.app.ui.screens.*
import com.mydonghua.app.ui.theme.CornerRadius
import com.mydonghua.app.ui.theme.IconSize
import com.mydonghua.app.ui.theme.MyDonghuaTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()
    private val showsViewModel: ShowsViewModel by viewModels()
    private val searchViewModel: SearchViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestPermissionsOnStart()

        setContent {
            val state by homeViewModel.uiState.collectAsState()
            val settingsState by settingsViewModel.uiState.collectAsState()
            val allShows by showsViewModel.allShows.collectAsState()
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route ?: "home"
            var showExitDialog by remember { mutableStateOf(false) }
            var isDrawerOpen by remember { mutableStateOf(false) }
            var isSearchOpen by remember { mutableStateOf(false) }
            val showGlobalHeader = currentRoute in listOf("home", "airing", "complete", "stopped")
            val navBackdrop = rememberLayerBackdrop()
            val contentBackdrop = rememberLayerBackdrop()
            val appHazeState = remember { HazeState() }

            androidx.activity.compose.BackHandler(enabled = !isSearchOpen) {
                when {
                    isDrawerOpen -> isDrawerOpen = false
                    currentRoute != "home" -> navController.popBackStack("home", inclusive = false)
                    else -> showExitDialog = true
                }
            }

            MyDonghuaTheme(themeMode = settingsState.themeMode, accentColor = settingsState.accentColor) {
                if (showExitDialog) {
                    AlertDialog(
                        onDismissRequest = { showExitDialog = false },
                        title = { Text("Exit App") },
                        text = { Text("Are you sure you want to exit?") },
                        confirmButton = {
                            TextButton(
                                onClick = {
                                    showExitDialog = false
                                    finish()
                                }
                            ) {
                                Text("Yes")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showExitDialog = false }
                            ) {
                                Text("No")
                            }
                        }
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
                            LocalLayerBackdrop provides contentBackdrop
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
                                    val isDetailRoute = currentRoute.startsWith("show_detail/") || currentRoute.startsWith("detail/")
                                    if (!isSearchOpen && (showGlobalHeader || isDetailRoute)) {
                                        if (isDetailRoute) {
                                            val detailShowId = navBackStackEntry?.arguments?.getString("showId")
                                            val detailShow = remember(detailShowId, allShows) { allShows.find { it.id == detailShowId } }
                                            GlassHeader(
                                                title = detailShow?.title ?: "Donghua Details",
                                                backdrop = contentBackdrop,
                                                isDark = settingsState.themeMode.lowercase() != "light",
                                                onBackClick = { navController.popBackStack() },
                                                onEditClick = detailShow?.let { s -> { navController.navigate("add_edit_show?showId=${s.id}") } },
                                            )
                                        } else {
                                            val headerTitle = when (currentRoute) {
                                                "home" -> "My Donghua"
                                                "airing" -> "Airing Schedule"
                                                "complete" -> "Completed"
                                                "stopped" -> "Stopped"
                                                else -> ""
                                            }
                                            GlassHeader(
                                                title = headerTitle,
                                                backdrop = contentBackdrop,
                                                isDark = settingsState.themeMode.lowercase() != "light",
                                                onDrawerOpen = { isDrawerOpen = true },
                                            )
                                        }
                                    }
                                },
                                bottomBar = {
                                    if (showGlobalHeader) {
                                        LiquidGlassNavBar(
                                            currentRoute = currentRoute,
                                            isDark = settingsState.themeMode.lowercase() != "light",
                                            backdrop = navBackdrop,
                                            isSearchOpen = isSearchOpen,
                                            onSearchClick = {
                                                isSearchOpen = !isSearchOpen
                                            },
                                            onNavigate = { route ->
                                                when (route) {
                                                    "home" -> {
                                                        val currentDest = navController.currentBackStackEntry?.destination?.route
                                                        if (currentDest != "home") {
                                                            navController.popBackStack("home", inclusive = false)
                                                        }
                                                    }
                                                    "add_edit_show" -> navController.navigate("add_edit_show")
                                                    else -> navController.navigate(route) { popUpTo("home"); launchSingleTop = true; restoreState = true }
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
                                    val dir = tabTransitionDirection(
                                        initialState.destination.route,
                                        targetState.destination.route
                                    )
                                    when {
                                        dir > 0 -> slideInHorizontally(
                                            animationSpec = ColorOSMotion.FluidIntOffset,
                                            initialOffsetX = { it }
                                        ) + scaleIn(
                                            initialScale = 0.94f,
                                            animationSpec = ColorOSMotion.FluidSpring
                                        ) + fadeIn(
                                            animationSpec = tween(ColorOSMotion.DurationNormal, easing = ColorOSMotion.FluidCubic)
                                        )
                                        dir < 0 -> slideInHorizontally(
                                            animationSpec = ColorOSMotion.FluidIntOffset,
                                            initialOffsetX = { -it }
                                        ) + scaleIn(
                                            initialScale = 0.94f,
                                            animationSpec = ColorOSMotion.FluidSpring
                                        ) + fadeIn(
                                            animationSpec = tween(ColorOSMotion.DurationNormal, easing = ColorOSMotion.FluidCubic)
                                        )
                                        else -> slideInHorizontally(
                                            animationSpec = ColorOSMotion.FluidIntOffset,
                                            initialOffsetX = { it }
                                        ) + scaleIn(
                                            initialScale = 0.95f,
                                            animationSpec = ColorOSMotion.FluidSpring
                                        ) + fadeIn(
                                            animationSpec = tween(ColorOSMotion.DurationQuick, easing = ColorOSMotion.FluidCubic)
                                        )
                                    }
                                },
                                exitTransition = {
                                    val dir = tabTransitionDirection(
                                        initialState.destination.route,
                                        targetState.destination.route
                                    )
                                    when {
                                        dir > 0 -> slideOutHorizontally(
                                            animationSpec = ColorOSMotion.FluidIntOffset,
                                            targetOffsetX = { -it }
                                        ) + scaleOut(
                                            targetScale = 0.96f,
                                            animationSpec = ColorOSMotion.FluidSpring
                                        ) + fadeOut(
                                            animationSpec = tween(ColorOSMotion.DurationQuick, easing = ColorOSMotion.FluidCubic)
                                        )
                                        dir < 0 -> slideOutHorizontally(
                                            animationSpec = ColorOSMotion.FluidIntOffset,
                                            targetOffsetX = { it }
                                        ) + scaleOut(
                                            targetScale = 0.96f,
                                            animationSpec = ColorOSMotion.FluidSpring
                                        ) + fadeOut(
                                            animationSpec = tween(ColorOSMotion.DurationQuick, easing = ColorOSMotion.FluidCubic)
                                        )
                                        else -> fadeOut(
                                            animationSpec = tween(ColorOSMotion.DurationQuick, easing = ColorOSMotion.FluidCubic)
                                        ) + scaleOut(
                                            targetScale = 0.96f,
                                            animationSpec = ColorOSMotion.FluidSpring
                                        )
                                    }
                                },
                                popEnterTransition = {
                                    slideInHorizontally(
                                        animationSpec = ColorOSMotion.FluidIntOffset,
                                        initialOffsetX = { -it / 3 }
                                    ) + scaleIn(
                                        initialScale = 0.96f,
                                        animationSpec = ColorOSMotion.FluidSpring
                                    ) + fadeIn(
                                        animationSpec = tween(ColorOSMotion.DurationQuick, easing = ColorOSMotion.FluidCubic)
                                    )
                                },
                                popExitTransition = {
                                    slideOutHorizontally(
                                        animationSpec = ColorOSMotion.FluidIntOffset,
                                        targetOffsetX = { it / 3 }
                                    ) + scaleOut(
                                        targetScale = 0.96f,
                                        animationSpec = ColorOSMotion.FluidSpring
                                    ) + fadeOut(
                                        animationSpec = tween(ColorOSMotion.DurationQuick, easing = ColorOSMotion.FluidCubic)
                                    )
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
                                onWatchClick = { url ->
                                    val intent = Intent(this@MainActivity, WatchActivity::class.java).apply {
                                        putExtra("watch_url", url)
                                    }
                                    startActivity(intent)
                                }
                            )
                        }
                        composable("airing") {
                                AiringScreen(
                                viewModel = showsViewModel,
                                topPadding = innerPadding.calculateTopPadding(),
                                onWatchClick = { url ->
                                    val intent = Intent(this@MainActivity, WatchActivity::class.java).apply {
                                        putExtra("watch_url", url)
                                    }
                                    startActivity(intent)
                                },
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
                                onWatchClick = { url ->
                                    val intent = Intent(this@MainActivity, WatchActivity::class.java).apply {
                                        putExtra("watch_url", url)
                                    }
                                    startActivity(intent)
                                },
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
                                onWatchClick = { url ->
                                    val intent = Intent(this@MainActivity, WatchActivity::class.java).apply {
                                        putExtra("watch_url", url)
                                    }
                                    startActivity(intent)
                                },
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
                                onWatchClick = { url ->
                                    val intent = Intent(this@MainActivity, WatchActivity::class.java).apply {
                                        putExtra("watch_url", url)
                                    }
                                    startActivity(intent)
                                },
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
                                topPadding = innerPadding.calculateTopPadding(),
                                defaultStreamProvider = settingsState.defaultStreamProvider,
                                onWatchClick = { url ->
                                    val intent = Intent(this@MainActivity, WatchActivity::class.java).apply {
                                        putExtra("watch_url", url)
                                    }
                                    startActivity(intent)
                                },
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
                    }
                }

                AnimatedVisibility(
                    visible = isSearchOpen,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioLowBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + fadeIn(animationSpec = tween(260, easing = FastOutSlowInEasing)),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(280, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(240, easing = FastOutSlowInEasing)),
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
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
                            ),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        SearchScreen(
                            viewModel = searchViewModel,
                            showsViewModel = showsViewModel,
                            onWatchClick = { url ->
                                val intent = Intent(this@MainActivity, WatchActivity::class.java).apply {
                                    putExtra("watch_url", url)
                                }
                                startActivity(intent)
                            },
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
                ) + scaleIn(
                    initialScale = 0.92f,
                    transformOrigin = TransformOrigin(0.5f, 0f),
                    animationSpec = ColorOSMotion.FluidSpring
                ) + fadeIn(
                    animationSpec = tween(ColorOSMotion.DurationNormal, easing = ColorOSMotion.FluidCubic)
                ),
                exit = slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = ColorOSMotion.SheetIntOffset
                ) + scaleOut(
                    targetScale = 0.95f,
                    transformOrigin = TransformOrigin(0.5f, 0f),
                    animationSpec = ColorOSMotion.FluidSpring
                ) + fadeOut(
                    animationSpec = tween(ColorOSMotion.DurationQuick, easing = ColorOSMotion.FluidCubic)
                )
            ) {
                val menuShape = RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp)
                val isAmoledMode = settingsState.themeMode.lowercase() == "amoled"
                val isLightMode = settingsState.themeMode.lowercase() == "light"

                val infinitePulse = rememberInfiniteTransition(label = "drawerPulse")
                val pulseAlpha by infinitePulse.animateFloat(
                    initialValue = 0.35f,
                    targetValue = 1.0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1300, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseAlpha"
                )
                val pulseScale by infinitePulse.animateFloat(
                    initialValue = 0.85f,
                    targetValue = 1.35f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1300, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseScale"
                )

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
                        // ColorOS 17 & OxygenOS 17 Fluid Header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(
                                            if (isLightMode) Color(0xFFE50914).copy(alpha = 0.10f)
                                             else Color(0xFFE50914).copy(alpha = 0.18f)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 3.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .graphicsLayer {
                                                    scaleX = pulseScale
                                                    scaleY = pulseScale
                                                    alpha = pulseAlpha * 0.45f
                                                }
                                                .clip(CircleShape)
                                                .background(Color(0xFFE50914))
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFE50914))
                                        )
                                    }
                                    Text(
                                        text = "COLOROS 17 • AURORA FLUID",
                                        fontSize = 9.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.2.sp,
                                        color = if (isLightMode) Color(0xFFC60000) else Color(0xFFFF5252)
                                    )
                                }
                                Text(
                                    text = "Navigation",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Quick access to hub & tools",
                                    fontSize = 12.5.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
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

                        // OxygenOS 17 Liquid Glass Quick Tiles (2x2 Grid)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "ESSENTIAL HUB",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.primary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OxygenDrawerTile(
                                    title = "Statistics",
                                    subtitle = "Hours & streaks",
                                    iconRes = R.drawable.ic_stats,
                                    gradient = listOf(Color(0xFFFF5722), Color(0xFFFF8A65)),
                                    isSelected = currentRoute == "stats",
                                    isLight = isLightMode,
                                    modifier = Modifier.weight(1f),
                                    onClick = { isDrawerOpen = false; navController.navigate("stats") }
                                )
                                OxygenDrawerTile(
                                    title = "Schedule",
                                    subtitle = "Weekly airings",
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
                                    subtitle = "Custom folders",
                                    iconRes = R.drawable.ic_collections,
                                    gradient = listOf(Color(0xFF7B1FA2), Color(0xFFBA68C8)),
                                    isSelected = currentRoute == "collections",
                                    isLight = isLightMode,
                                    modifier = Modifier.weight(1f),
                                    onClick = { isDrawerOpen = false; navController.navigate("collections") }
                                )
                                OxygenDrawerTile(
                                    title = "Downloads",
                                    subtitle = "Offline videos",
                                    iconRes = R.drawable.ic_history,
                                    gradient = listOf(Color(0xFF00897B), Color(0xFF26A69A)),
                                    isSelected = currentRoute == "downloads",
                                    isLight = isLightMode,
                                    modifier = Modifier.weight(1f),
                                    onClick = { isDrawerOpen = false; navController.navigate("downloads") }
                                )
                            }
                        }

                        // OxygenOS 17 Grouped Island List
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "MORE DESTINATIONS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = MaterialTheme.colorScheme.primary
                            )

                            val listCardShape = RoundedCornerShape(22.dp)
                            val listCardBg = if (isLightMode) Color.White.copy(alpha = 0.70f)
                            else Color.White.copy(alpha = 0.05f)
                            val listBorder = if (isLightMode) Color.Black.copy(alpha = 0.08f)
                            else Color.White.copy(alpha = 0.12f)

                            Surface(
                                shape = listCardShape,
                                color = listCardBg,
                                border = androidx.compose.foundation.BorderStroke(0.8.dp, listBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    OxygenDrawerListItem(
                                        title = "Watch History",
                                        subtitle = "Recent episodes & progress",
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
                                        subtitle = "Quarterly broadcast lists",
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
                                        subtitle = "Themes, streaming & backups",
                                        iconRes = R.drawable.ic_settings,
                                        iconGradient = listOf(Color(0xFFE50914), Color(0xFFFF5252)),
                                        isSelected = currentRoute == "settings",
                                        isLight = isLightMode,
                                        onClick = { isDrawerOpen = false; navController.navigate("settings") }
                                    )
                                }
                            }
                        }

                        // Bottom Pill & Footer
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp, bottom = 8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 36.dp, height = 4.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isLightMode) Color.Black.copy(alpha = 0.15f)
                                        else Color.White.copy(alpha = 0.25f)
                                    )
                            )
                            Text(
                                text = "My Donghua • ColorOS 17 Liquid Glass Edition",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f)
                            )
                        }
                    }
                }
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

        private fun tabTransitionDirection(from: String?, to: String?): Int {
            val fromIndex = TAB_ROUTES.indexOf(from)
            val toIndex = TAB_ROUTES.indexOf(to)
            return if (fromIndex != -1 && toIndex != -1) toIndex.compareTo(fromIndex) else 0
        }
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
    subtitle: String,
    iconRes: Int,
    gradient: List<Color>,
    isSelected: Boolean,
    isLight: Boolean,
    modifier: Modifier = Modifier,
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

    // ColorOS 17 Prismatic Aurora Specular Border
    val specularBorder = if (isSelected) {
        Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary,
                Color(0xFF26C6DA),
                MaterialTheme.colorScheme.primary.copy(alpha = 0.40f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                if (isLight) Color.White.copy(alpha = 0.90f) else Color.White.copy(alpha = 0.32f),
                if (isLight) Color.White.copy(alpha = 0.40f) else Color(0xFF26C6DA).copy(alpha = 0.16f),
                if (isLight) Color.Black.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.05f)
            )
        )
    }

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = shape,
        color = cardBg,
        border = androidx.compose.foundation.BorderStroke(if (isSelected) 1.5.dp else 0.8.dp, specularBorder),
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .height(108.dp)
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
                        .background(Brush.linearGradient(gradient))
                        .border(
                            0.5.dp,
                            Color.White.copy(alpha = 0.35f),
                            RoundedCornerShape(13.dp)
                        ),
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

@Composable
private fun OxygenDrawerListItem(
    title: String,
    subtitle: String,
    iconRes: Int,
    iconGradient: List<Color>,
    isSelected: Boolean,
    isLight: Boolean,
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

    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        color = MaterialTheme.colorScheme.primary.copy(alpha = bgAlpha),
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = itemScale
                scaleY = itemScale
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(Brush.linearGradient(iconGradient))
                    .border(
                        0.5.dp,
                        Color.White.copy(alpha = 0.30f),
                        RoundedCornerShape(13.dp)
                    ),
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
                    fontSize = 14.5.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
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
