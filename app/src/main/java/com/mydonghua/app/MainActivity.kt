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
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.IntOffset
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlin.math.roundToInt
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
            val drawerHazeState = remember { HazeState() }

            androidx.activity.compose.BackHandler(enabled = !isSearchOpen) {
                if (currentRoute != "home") {
                    navController.popBackStack("home", inclusive = false)
                } else {
                    showExitDialog = true
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
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .haze(drawerHazeState, style = HazeMaterials.ultraThin())
                        ) {
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
                                     if (!isSearchOpen) {
                                         val isDetailRoute = currentRoute.startsWith("show_detail/") || currentRoute.startsWith("detail/")
                                         if (showGlobalHeader || isDetailRoute) {
                                             if (isDetailRoute) {
                                                 val detailShowId = navBackStackEntry?.arguments?.getString("showId")
                                                 val detailShow = remember(detailShowId, allShows) { allShows.find { it.id == detailShowId } }
                                                 GlassHeader(
                                                     title = detailShow?.title ?: "Donghua Details",
                                                     backdrop = contentBackdrop,
                                                     isDark = settingsState.themeMode.lowercase() != "light",
                                                     onBackClick = { navController.popBackStack() },
                                                     onEditClick = detailShow?.let { s -> { navController.navigate("add_edit_show?showId=${s.id}") } }
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
                                     }
                                },
                                bottomBar = {
                                    if (showGlobalHeader && !isSearchOpen) {
                                        LiquidGlassNavBar(
                                            currentRoute = currentRoute,
                                            isDark = settingsState.themeMode.lowercase() != "light",
                                            backdrop = navBackdrop,
                                            isSearchOpen = isSearchOpen,
                                            onSearchClick = { isSearchOpen = !isSearchOpen },
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
                                    fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.96f, animationSpec = tween(220))
                                },
                                exitTransition = {
                                    fadeOut(animationSpec = tween(180)) + scaleOut(targetScale = 0.98f, animationSpec = tween(180))
                                },
                                popEnterTransition = {
                                    fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.98f, animationSpec = tween(220))
                                },
                                popExitTransition = {
                                    fadeOut(animationSpec = tween(180)) + scaleOut(targetScale = 0.96f, animationSpec = tween(180))
                                }
                            ) {
                        composable("home") {
                                HomeScreen(
                                viewModel = homeViewModel,
                                topPadding = innerPadding.calculateTopPadding(),
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
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToStats = { navController.navigate("stats") },
                                onNavigateToCalendar = { navController.navigate("calendar") },
                                onNavigateToHistory = { navController.navigate("history") },
                                onNavigateToCollections = { navController.navigate("collections") }
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
                    }
                }

                AnimatedVisibility(
                    visible = isSearchOpen,
                    enter = slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(280, easing = FastOutSlowInEasing)
                    ) + fadeIn(animationSpec = tween(280)),
                    exit = slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(240, easing = FastOutSlowInEasing)
                    ) + fadeOut(animationSpec = tween(240)),
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
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
                                navController.navigate("show_detail/$showId")
                            },
                            onNavigateBack = { isSearchOpen = false }
                        )
                    }
                }

                }
            }

            AnimatedVisibility(
                visible = isDrawerOpen,
                modifier = Modifier.zIndex(1000f),
                enter = slideInVertically(
                    initialOffsetY = { -it },
                    animationSpec = tween(
                        durationMillis = 350,
                        easing = androidx.compose.animation.core.FastOutSlowInEasing
                    )
                ) + fadeIn(animationSpec = tween(300)),
                exit = slideOutVertically(
                    targetOffsetY = { -it },
                    animationSpec = tween(
                        durationMillis = 300,
                        easing = androidx.compose.animation.core.FastOutSlowInEasing
                    )
                ) + fadeOut(animationSpec = tween(300))
            ) {
                val menuShape = RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp)
                val isAmoledMode = settingsState.themeMode.lowercase() == "amoled"
                val isLightMode = settingsState.themeMode.lowercase() == "light"

                MenuGlassPanel(
                    modifier = Modifier.fillMaxSize(),
                    shape = menuShape,
                    isAmoledMode = isAmoledMode,
                    isLightMode = isLightMode
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                            .padding(horizontal = Spacing.xl, vertical = Spacing.lg),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Navigation",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "My Donghua App",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = { isDrawerOpen = false },
                                colors = IconButtonDefaults.iconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                    contentColor = MaterialTheme.colorScheme.onSurface
                                ),
                                modifier = Modifier.size(44.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Close Menu",
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Spacing.md),
                            verticalArrangement = Arrangement.spacedBy(Spacing.sm)
                        ) {
                            DrawerItem(
                                iconRes = R.drawable.ic_stats,
                                label = "Statistics",
                                isSelected = currentRoute == "stats",
                                onClick = { isDrawerOpen = false; navController.navigate("stats") }
                            )
                            DrawerItem(
                                iconRes = R.drawable.ic_calendar,
                                label = "Weekly Schedule",
                                isSelected = currentRoute == "calendar",
                                onClick = { isDrawerOpen = false; navController.navigate("calendar") }
                            )
                            DrawerItem(
                                iconRes = R.drawable.ic_collections,
                                label = "Collections",
                                isSelected = currentRoute == "collections",
                                onClick = { isDrawerOpen = false; navController.navigate("collections") }
                            )
                            DrawerItem(
                                iconRes = R.drawable.ic_history,
                                label = "Watch History",
                                isSelected = currentRoute == "history",
                                onClick = { isDrawerOpen = false; navController.navigate("history") }
                            )
                            DrawerItem(
                                iconRes = R.drawable.ic_seasonal,
                                label = "Seasonal View",
                                isSelected = currentRoute == "seasonal",
                                onClick = { isDrawerOpen = false }
                            )

                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant,
                                modifier = Modifier.padding(vertical = Spacing.xs)
                            )

                            DrawerItem(
                                iconRes = R.drawable.ic_settings,
                                label = "Settings",
                                isSelected = currentRoute == "settings",
                                onClick = { isDrawerOpen = false; navController.navigate("settings") }
                            )
                        }

                        Text(
                            text = "My Donghua • v1.0.0",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
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
    private fun requestPermissionsOnStart() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return
        requestNotificationPermission()
        requestStoragePermission()
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

    fun requestStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_MEDIA_IMAGES)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.READ_MEDIA_IMAGES),
                    STORAGE_PERMISSION_CODE
                )
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE),
                    STORAGE_PERMISSION_CODE
                )
            }
        }
    }

    companion object {
        const val FILE_CHOOSER_REQUEST_CODE = 1001
        private const val NOTIFICATION_PERMISSION_CODE = 1002
        private const val STORAGE_PERMISSION_CODE = 1003
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
private fun DrawerItem(
    iconRes: Int,
    label: String,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    val pillShape = CircleShape
    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        Color.Transparent
    }
    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val iconTint = if (isSelected) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        onClick = onClick,
        selected = isSelected,
        shape = pillShape,
        color = containerColor,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = label,
                modifier = Modifier.size(24.dp),
                tint = iconTint
            )
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = contentColor
            )
        }
    }
}
