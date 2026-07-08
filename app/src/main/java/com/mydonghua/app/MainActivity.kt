package com.mydonghua.app

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
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
import com.mydonghua.app.ui.home.HomeScreen
import com.mydonghua.app.ui.screens.*
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
                                if (showGlobalHeader) {
                                    GlassHeader(
                                        isDark = settingsState.themeMode.lowercase() != "light",
                                        onSearchClick = { navController.navigate("search") },
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
                                modifier = Modifier.fillMaxSize()
                            ) {
                        composable("home") {
                            GlassBackground {
                                HomeScreen(
                                viewModel = homeViewModel,
                                onSearchClick = {
                                    navController.navigate("search")
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
                        composable("search") {
                            GlassBackground {
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
                                onNavigateBack = { navController.popBackStack() },
                                onAddClick = { navController.navigate("add_edit_show") }
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

                if (showGlobalHeader) {
                    LiquidGlassNavBar(
                        currentRoute = currentRoute,
                        isDark = settingsState.themeMode.lowercase() != "light",
                        backdrop = backdrop,
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
                            .navigationBarsPadding()
                            .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
                    )
                }

                // Right Drawer Menu
                if (isDrawerOpen) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f))
                            .clickable { isDrawerOpen = false }
                    ) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .width(280.dp)
                                .hazeChild(state = hazeState)
                                .background(
                                    color = if (settingsState.themeMode.lowercase() != "light") Color.White.copy(alpha = 0.04f)
                                    else Color.Black.copy(alpha = 0.02f)
                                )
                                .clickable(enabled = false) {}
                                .statusBarsPadding()
                                .navigationBarsPadding()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                                        Text("✕", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

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
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                    modifier = Modifier.padding(vertical = 8.dp)
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
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
            for (i in 0 until 120) {
                val x = random.nextFloat() * width
                val y = random.nextFloat() * height
                val radius = random.nextFloat() * 1.5f + 0.5f
                val alpha = random.nextFloat() * 0.4f + 0.1f
                drawCircle(
                    color = starsColor,
                    radius = radius,
                    center = androidx.compose.ui.geometry.Offset(x, y),
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
                interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                indication = null
            ) { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            modifier = Modifier.size(20.dp),
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
