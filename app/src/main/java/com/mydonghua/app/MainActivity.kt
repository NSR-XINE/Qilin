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
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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
            val navController = rememberNavController()
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = navBackStackEntry?.destination?.route ?: "home"
            var showExitDialog by remember { mutableStateOf(false) }
            var isDrawerOpen by remember { mutableStateOf(false) }
            var isSearchOpen by remember { mutableStateOf(false) }
            val showGlobalHeader = currentRoute in listOf("home", "airing", "complete", "stopped")
            val backdrop = rememberLayerBackdrop()

            androidx.activity.compose.BackHandler(enabled = true) {
                if (currentRoute != "home") {
                    navController.navigate("home") {
                        popUpTo("home") { inclusive = false }
                        launchSingleTop = true
                    }
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
                        val hazeState = remember { HazeState() }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .haze(hazeState, style = HazeMaterials.ultraThin())
                        ) {
                            if (settingsState.themeMode.lowercase() != "light") {
                                StarsBackground()
                            }
                        }
                        CompositionLocalProvider(LocalHazeState provides hazeState) {
                        Scaffold(
                            containerColor = Color.Transparent,
                            modifier = Modifier.fillMaxSize(),
                            topBar = {
                                if (showGlobalHeader && !isSearchOpen) {
                                    GlassHeader(
                                        isDark = settingsState.themeMode.lowercase() != "light",
                                        onDrawerOpen = { isDrawerOpen = true },
                                    )
                                }
                            }
                        ) { innerPadding ->
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = innerPadding.calculateTopPadding())
                                    .layerBackdrop(backdrop)
                            ) {
                            NavHost(
                                navController = navController,
                                startDestination = "home",
                                modifier = Modifier.fillMaxSize(),
                                enterTransition = {
                                    fadeIn() + slideInHorizontally { -it }
                                },
                                exitTransition = {
                                    fadeOut() + slideOutHorizontally { it }
                                },
                                popEnterTransition = {
                                    fadeIn() + slideInHorizontally { -it }
                                },
                                popExitTransition = {
                                    fadeOut() + slideOutHorizontally { it }
                                },
                            ) {
                        composable("home") {
                            GlassBackground {
                                HomeScreen(
                                viewModel = homeViewModel,
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
                        }
                        composable("airing") {
                            GlassBackground {
                                AiringScreen(
                                viewModel = showsViewModel,
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
                        }
                        composable("complete") {
                            GlassBackground {
                                CompletedScreen(
                                viewModel = showsViewModel,
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
                        }
                        composable("stopped") {
                            GlassBackground {
                                StoppedScreen(
                                viewModel = showsViewModel,
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
                        }
                        composable("favorites") {
                            GlassBackground {
                                FavoritesScreen(
                                viewModel = showsViewModel,
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
                            GlassBackground {
                                val showIdArg = backStackEntry.arguments?.getString("showId")
                                AddEditShowScreen(
                                showId = showIdArg,
                                viewModel = showsViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        }
                        composable("show_detail/{showId}") { backStackEntry ->
                            GlassBackground {
                                val showIdArg = backStackEntry.arguments?.getString("showId") ?: ""
                                ShowDetailScreen(
                                showId = showIdArg,
                                viewModel = showsViewModel,
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
                        }
                        composable("settings") {
                            GlassBackground {
                                SettingsScreen(
                                viewModel = settingsViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToStats = { navController.navigate("stats") },
                                onNavigateToCalendar = { navController.navigate("calendar") },
                                onNavigateToHistory = { navController.navigate("history") },
                                onNavigateToCollections = { navController.navigate("collections") }
                            )
                        }
                        }
                        composable("stats") {
                            GlassBackground {
                                StatsScreen(
                                showsViewModel = showsViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        }
                        composable("calendar") {
                            GlassBackground {
                                CalendarScreen(
                                showsViewModel = showsViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        }
                        composable("history") {
                            GlassBackground {
                                HistoryScreen(
                                showsViewModel = showsViewModel,
                                onShowClick = { showId ->
                                    navController.navigate("show_detail/$showId")
                                },
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }
                        }
                        composable("collections") {
                            GlassBackground {
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

            }
            }

            AnimatedVisibility(
                visible = isSearchOpen,
                enter = slideInHorizontally { it } + fadeIn(),
                exit = slideOutHorizontally { it } + fadeOut(),
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

                if (showGlobalHeader) {
                    LiquidGlassNavBar(
                        currentRoute = currentRoute,
                        isDark = settingsState.themeMode.lowercase() != "light",
                        backdrop = backdrop,
                        isSearchOpen = isSearchOpen,
                        onSearchClick = { isSearchOpen = !isSearchOpen },
                        onNavigate = { route ->
                            when (route) {
                                "home" -> navController.navigate("home") { popUpTo("home") { inclusive = true }; launchSingleTop = true }
                                "add_edit_show" -> navController.navigate("add_edit_show")
                                else -> navController.navigate(route) { popUpTo("home"); launchSingleTop = true; restoreState = true }
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .padding(bottom = Spacing.sm, start = Spacing.sm, end = Spacing.sm),
                    )
                }

                if (isDrawerOpen) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f))
                            .clickable(onClick = { isDrawerOpen = false })
                    ) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .width(280.dp)
                                .hazeChild(state = hazeState)
                                .background(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)
                                )
                                .clickable(enabled = false) {}
                                .statusBarsPadding()
                                .navigationBarsPadding()
                                .padding(horizontal = Spacing.xl, vertical = Spacing.xl),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Menu",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    IconButton(onClick = { isDrawerOpen = false }) {
                                        Text("\u2715", fontSize = IconSize.xs.value.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Spacer(modifier = Modifier.height(Spacing.sm))

                                DrawerItem(
                                    iconRes = R.drawable.ic_stats,
                                    label = "Statistics",
                                    onClick = { isDrawerOpen = false; navController.navigate("stats") }
                                )
                                DrawerItem(
                                    iconRes = R.drawable.ic_calendar,
                                    label = "Calendar",
                                    onClick = { isDrawerOpen = false; navController.navigate("calendar") }
                                )
                                DrawerItem(
                                    iconRes = R.drawable.ic_collections,
                                    label = "Collections",
                                    onClick = { isDrawerOpen = false; navController.navigate("collections") }
                                )
                                DrawerItem(
                                    iconRes = R.drawable.ic_history,
                                    label = "Watch History",
                                    onClick = { isDrawerOpen = false; navController.navigate("history") }
                                )
                                DrawerItem(
                                    iconRes = R.drawable.ic_seasonal,
                                    label = "Seasonal View",
                                    onClick = { isDrawerOpen = false }
                                )

                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.padding(vertical = Spacing.sm)
                                )

                                DrawerItem(
                                    iconRes = R.drawable.ic_settings,
                                    label = "Settings",
                                    onClick = { isDrawerOpen = false; navController.navigate("settings") }
                                )
                            }

                            Text(
                                text = "My Donghua",
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
                val alpha = random.nextFloat() * 0.4f + 0.1f
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
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.md)
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            modifier = Modifier.size(IconSize.md),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}