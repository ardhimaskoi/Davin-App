package com.example.aplikasidavin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.example.aplikasidavin.screens.*
import com.example.aplikasidavin.screens.auth.LoginScreen
import com.example.aplikasidavin.screens.auth.RegisterScreen
import com.example.aplikasidavin.ui.theme.AplikasiDavinTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

// ======================================================================
//  MAIN ACTIVITY
// ======================================================================

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val navController = rememberNavController()

            AplikasiDavinTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    DavinApp(navController)
                }
            }
        }
    }
}

// ======================================================================
//  ROOT APP (GLOBAL SNACKBAR + NAVGRAPH + BOTTOM NAV)
// ======================================================================

@Composable

fun DavinApp(navController: NavHostController) {

    val backStackEntry by navController.currentBackStackEntryAsState()
    var showBottomBar by remember { mutableStateOf(true) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(backStackEntry) {
        val route = backStackEntry?.destination?.route ?: ""
        val hide = listOf("login", "register", "splash", "crypto_detail", "buy", "sell")
        showBottomBar = hide.none { route.contains(it) }
    }

    // 🔥 LAYER PALING ATAS (Overlay)
    Box(modifier = Modifier.fillMaxSize()) {

        // 🔥 NAVIGATION DAN UI
        Scaffold(
            bottomBar = {
                if (showBottomBar) BottomNavigationBar(navController)
            }
        ) { innerPadding ->
            NavGraph(
                navController = navController,
                modifier = Modifier.padding(innerPadding),
                snackbarHostState = snackbarHostState,
                coroutineScope = coroutineScope,
                onHideBottomNav = { hide -> showBottomBar = !hide }
            )
        }

        // 🔥 SNACKBAR DI ATAS SEMUA SCREEN (OVERLAY)
        SnackbarHost(
            hostState = snackbarHostState,
            snackbar = { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Color.White,        // 🔥 Background putih
                    contentColor = Color(0xFF4A148C),    // 🔥 Teks ungu elegan (bisa diubah)
                    actionContentColor = Color(0xFF4A148C),
                    shape = RoundedCornerShape(12.dp),   // Rounded biar modern
                    modifier = Modifier.padding(16.dp)
                )
            }
        )
    }
}



// ======================================================================
//  BOTTOM NAVIGATION
// ======================================================================

@Composable
fun BottomNavigationBar(navController: NavController) {

    val items = listOf(
        BottomNavItem("home", Icons.Default.Home, "Beranda"),
        BottomNavItem("history", Icons.Default.List, "Riwayat"),
        BottomNavItem("blockchain", Icons.Default.Build, "Blockchain"),
        BottomNavItem("profile", Icons.Default.Person, "Profil")
    )

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    NavigationBar(
        containerColor = Color.White,
        tonalElevation = 4.dp
    ) {
        items.forEach { item ->
            val selected = currentRoute?.startsWith(item.route) == true

            NavigationBarItem(
                selected = selected,
                onClick = {
                    val args = navController.currentBackStackEntry?.arguments
                    val userId = args?.getString("userId") ?: "1"
                    navController.navigate("${item.route}/$userId") {
                        popUpTo("home/$userId") { inclusive = false }
                    }
                },
                icon = {
                    Icon(
                        item.icon,
                        contentDescription = item.label,
                        tint = if (selected) Color(0xFF5E35B1) else Color.Gray
                    )
                },
                label = {
                    Text(
                        item.label,
                        color = if (selected) Color(0xFF5E35B1) else Color.Gray,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            )
        }
    }
}

data class BottomNavItem(
    val route: String,
    val icon: ImageVector,
    val label: String
)

// ======================================================================
//  NAVIGATION GRAPH
// ======================================================================

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope,
    onHideBottomNav: (Boolean) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = "splash",
        modifier = modifier
    ) {

        // Splash
        composable("splash") { SplashScreen(navController) }

        // Auth screens
        composable("login") { LoginScreen(navController, snackbarHostState, coroutineScope) }
        composable("register") { RegisterScreen(navController, snackbarHostState, coroutineScope) }

        // Home
        composable("home/{userId}") {
            val userId = it.arguments?.getString("userId")?.toInt() ?: 0
            HomeScreen(userId, navController)
        }

        // Detail Crypto
        composable("crypto_detail/{userId}/{symbol}") {
            val userId = it.arguments?.getString("userId")?.toInt() ?: 0
            val symbol = it.arguments?.getString("symbol") ?: "BTC"

            CryptoDetailScreen(
                userId = userId,
                symbol = symbol,
                navController = navController,
                snackbarHostState = snackbarHostState,
                coroutineScope = coroutineScope
            )
        }

        // BUY
        composable("buy/{userId}/{symbol}") {
            val userId = it.arguments?.getString("userId")?.toInt() ?: 0
            val symbol = it.arguments?.getString("symbol") ?: "BTC"

            BuyScreen(
                userId = userId,
                symbol = symbol,
                snackbarHostState = snackbarHostState,
                coroutineScope = coroutineScope,
                navController = navController,
                onHideBottomNav = onHideBottomNav
            )

        }

        // SELL
        composable("sell/{userId}/{symbol}") {
            val userId = it.arguments?.getString("userId")?.toInt() ?: 0
            val symbol = it.arguments?.getString("symbol") ?: "BTC"

            SellScreen(
                userId,
                symbol,
                snackbarHostState,
                coroutineScope,
                navController
            )
        }

        // History
        composable("history/{userId}") {
            val userId = it.arguments?.getString("userId")?.toInt() ?: 0
            HistoryScreen(userId, snackbarHostState, coroutineScope)
        }

        // Blockchain Log
        composable("blockchain/{userId}") {
            val userId = it.arguments?.getString("userId")?.toInt() ?: 0
            BlockchainHistoryScreen(userId, snackbarHostState, coroutineScope)
        }

        // Profile
        composable("profile/{userId}") {
            val userId = it.arguments?.getString("userId")?.toInt() ?: 0
            ProfileScreen(userId, navController, snackbarHostState, coroutineScope)
        }
    }
}
