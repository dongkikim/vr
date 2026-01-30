package com.example.vrapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.vrapp.viewmodel.StockViewModel

@Composable
fun VRApp(viewModel: StockViewModel) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            
            // Custom Bottom Navigation Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant), // 바 배경색
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Helper function for items
                @Composable
                fun CustomNavItem(
                    label: String,
                    icon: @Composable () -> Unit,
                    route: String,
                    onClick: () -> Unit
                ) {
                    val selected = currentDestination?.hierarchy?.any { it.route == route } == true
                    val backgroundColor = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent
                    val contentColor = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(backgroundColor) // 영역 전체 음영
                            .clickable(onClick = onClick),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CompositionLocalProvider(LocalContentColor provides contentColor) {
                            icon()
                            Text(label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // 1. 관리 (Settings)
                CustomNavItem(
                    label = "관리",
                    icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                    route = "settings",
                    onClick = {
                        navController.navigate("settings") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )

                // 2. 홈 (Home)
                CustomNavItem(
                    label = "홈",
                    icon = { Icon(Icons.Default.Home, contentDescription = null) },
                    route = "home",
                    onClick = {
                        viewModel.refreshAllPrices() // 홈 버튼 클릭 시 가격 갱신
                        navController.navigate("home") {
                            popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )

                // 3. 차트 (Chart)
                CustomNavItem(
                    label = "차트",
                    icon = { Icon(Icons.Default.Info, contentDescription = null) },
                    route = "chart",
                    onClick = {
                        navController.navigate("chart") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("home") {
                MainScreen(
                    viewModel = viewModel,
                    onStockClick = { stockId ->
                        navController.navigate("detail/$stockId")
                    },
                    onChartClick = {
                        navController.navigate("chart")
                    }
                )
            }
            composable(
                "detail/{stockId}",
                arguments = listOf(navArgument("stockId") { type = NavType.LongType })
            ) { backStackEntry ->
                val stockId = backStackEntry.arguments?.getLong("stockId") ?: return@composable
                DetailScreen(
                    viewModel = viewModel,
                    stockId = stockId,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("chart") {
                ChartScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("settings") {
                SettingsScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}