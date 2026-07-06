package com.mydonghua.app

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.work.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.blur
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import com.mydonghua.app.ui.home.HomeScreen
import com.mydonghua.app.ui.screens.*
import com.mydonghua.app.ui.theme.MyDonghuaTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.concurrent.TimeUnit

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()
    private val showsViewModel: ShowsViewModel by viewModels()
    private val searchViewModel: SearchViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupBackgroundSync()
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
                        if (settingsState.themeMode.lowercase() != "light") {
                            StarsBackground()
                        }
                        Scaffold(
                            containerColor = Color.Transparent,
                            modifier = Modifier.fillMaxSize(),
                            topBar = {
                                if (showGlobalHeader) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .statusBarsPadding()
                                            .padding(horizontal = 16.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Image(
                                                painter = painterResource(id = R.mipmap.ic_launcher),
                                                contentDescription = "App Icon",
                                                modifier = Modifier
                                                    .size(42.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                            )

                                            Spacer(modifier = Modifier.width(12.dp))

                                            Row {
                                                Text(
                                                    text = "东画 ",
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = MaterialTheme.colorScheme.onBackground
                                                )
                                                Text(
                                                    text = "My Donghua",
                                                    fontSize = 20.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    style = androidx.compose.ui.text.TextStyle(
                                                        brush = Brush.linearGradient(
                                                            colors = listOf(
                                                                Color(0xFF00F2FE),
                                                                Color(0xFF9D4EDD)
                                                            )
                                                        )
                                                    )
                                                )
                                            }
                                        }

                                        IconButton(
                                            onClick = { isDrawerOpen = true },
                                            modifier = Modifier
                                                .background(
                                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .border(
                                                    width = 1.dp,
                                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                                                    shape = RoundedCornerShape(8.dp)
                                                )
                                                .size(40.dp)
                                        ) {
                                            Text(
                                                text = "☰",
                                                fontSize = 20.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                    }
                                }
                            }
                        ) { innerPadding ->
                            NavHost(
                                navController = navController,
                                startDestination = "home",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(top = innerPadding.calculateTopPadding())
                            ) {
                        composable("home") {
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
                        composable("airing") {
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
                        composable("complete") {
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
                        composable("stopped") {
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
                        composable("favorites") {
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
                        composable("search") {
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

                if (showGlobalHeader) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(bottom = 16.dp, start = 16.dp, end = 16.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Row(
                            modifier = Modifier
                                .widthIn(max = 500.dp)
                                .fillMaxWidth()
                                .height(65.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.75f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceAround
                        ) {
                            BottomNavItem(
                                iconRes = R.drawable.ic_home,
                                label = "Home",
                                selected = currentRoute == "home",
                                onClick = { navController.navigate("home") { popUpTo("home") { saveState = true }; launchSingleTop = true; restoreState = true } }
                            )
                            BottomNavItem(
                                iconRes = R.drawable.ic_airing,
                                label = "Airing",
                                selected = currentRoute == "airing",
                                onClick = { navController.navigate("airing") { popUpTo("home"); launchSingleTop = true; restoreState = true } }
                            )
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(Color(0xFF00F2FE), Color(0xFF9D4EDD))
                                        )
                                    )
                                    .clickable {
                                        navController.navigate("add_edit_show")
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("+", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            }
                            BottomNavItem(
                                iconRes = R.drawable.ic_complete,
                                label = "Complete",
                                selected = currentRoute == "complete",
                                onClick = { navController.navigate("complete") { popUpTo("home"); launchSingleTop = true; restoreState = true } }
                            )
                            BottomNavItem(
                                iconRes = R.drawable.ic_stopped,
                                label = "Stopped",
                                selected = currentRoute == "stopped",
                                onClick = { navController.navigate("stopped") { popUpTo("home"); launchSingleTop = true; restoreState = true } }
                            )
                        }
                    }
                }

                // Right Drawer Menu
                if (isDrawerOpen) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f))
                            .clickable { isDrawerOpen = false }
                    ) {
                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .width(280.dp)
                                .background(MaterialTheme.colorScheme.surface)
                                .clickable(enabled = false) {}
                                .statusBarsPadding()
                                .navigationBarsPadding()
                                .padding(20.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "🐉",
                                            fontSize = 20.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Menu",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    IconButton(onClick = { isDrawerOpen = false }) {
                                        Text("✕", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                DrawerItem(
                                    iconRes = R.drawable.ic_stats,
                                    label = "Statistics",
                                    iconColor = MaterialTheme.colorScheme.primary,
                                    onClick = {
                                        isDrawerOpen = false
                                        navController.navigate("stats")
                                    }
                                )
                                DrawerItem(
                                    iconRes = R.drawable.ic_calendar,
                                    label = "Calendar",
                                    iconColor = Color(0xFF9D4EDD),
                                    onClick = {
                                        isDrawerOpen = false
                                        navController.navigate("calendar")
                                    }
                                )
                                DrawerItem(
                                    iconRes = R.drawable.ic_collections,
                                    label = "Collections",
                                    iconColor = MaterialTheme.colorScheme.primary,
                                    onClick = {
                                        isDrawerOpen = false
                                        navController.navigate("collections")
                                    }
                                )
                                DrawerItem(
                                    iconRes = R.drawable.ic_history,
                                    label = "Watch History",
                                    iconColor = Color(0xFF9D4EDD),
                                    onClick = {
                                        isDrawerOpen = false
                                        navController.navigate("history")
                                    }
                                )
                                DrawerItem(
                                    iconRes = R.drawable.ic_seasonal,
                                    label = "Seasonal View",
                                    iconColor = MaterialTheme.colorScheme.primary,
                                    onClick = {
                                        isDrawerOpen = false
                                    }
                                )

                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))

                                DrawerItem(
                                    iconRes = R.drawable.ic_settings,
                                    label = "Settings",
                                    iconColor = MaterialTheme.colorScheme.primary,
                                    onClick = {
                                        isDrawerOpen = false
                                        navController.navigate("settings")
                                    }
                                )
                            }

                            Text(
                                text = "Last sync: just now",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
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

    private fun setupBackgroundSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val syncRequest = PeriodicWorkRequest.Builder(
            SyncWorker::class.java, 4, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .addTag("donghua_sync")
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "donghua_background_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
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
private fun RowScope.BottomNavItem(
    iconRes: Int,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            modifier = Modifier
                .size(24.dp)
                .align(Alignment.CenterHorizontally),
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

@Composable
private fun DrawerItem(
    iconRes: Int,
    label: String,
    iconColor: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = iconColor
        )
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
