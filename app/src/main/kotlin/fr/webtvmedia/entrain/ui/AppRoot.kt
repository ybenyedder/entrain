package fr.webtvmedia.entrain.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import fr.webtvmedia.entrain.EnTrainApp
import fr.webtvmedia.entrain.ui.alerts.TrafficScreen
import fr.webtvmedia.entrain.ui.departures.DeparturesScreen
import fr.webtvmedia.entrain.ui.home.HomeScreen
import fr.webtvmedia.entrain.ui.journey.JourneyDetailScreen
import fr.webtvmedia.entrain.ui.onboarding.OnboardingScreen
import fr.webtvmedia.entrain.ui.picker.StationPickerScreen
import fr.webtvmedia.entrain.ui.results.ResultsScreen
import fr.webtvmedia.entrain.ui.settings.SettingsScreen
import fr.webtvmedia.entrain.ui.stations.StationsTab
import fr.webtvmedia.entrain.ui.vm.AppViewModel
import fr.webtvmedia.entrain.ui.vm.VmFactory

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val STATIONS = "stations"
    const val TRAFFIC = "traffic"
    const val SETTINGS = "settings"
    const val RESULTS = "results/{from}/{to}/{ymd}/{sec}/{ab}"
    const val JOURNEY = "journey/{idx}"
    const val PICKER = "picker/{slot}"
    const val DEPARTURES = "departures/{stationId}"
    const val LIVE = "live"

    fun results(from: String, to: String, ymd: Int, sec: Int, arriveBy: Int = 0) =
        "results/$from/$to/$ymd/$sec/$arriveBy"

    fun journey(idx: Int) = "journey/$idx"
    fun picker(slot: String) = "picker/$slot"
    fun departures(stationId: String) = "departures/$stationId"
}

private data class Tab(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val iconSelected: ImageVector,
)

@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val context = LocalContext.current
    val app = context.applicationContext as EnTrainApp
    val appVm: AppViewModel = viewModel(factory = VmFactory(app))

    val tabs = listOf(
        Tab(Routes.HOME, "Accueil", Icons.Outlined.Home, Icons.Filled.Home),
        Tab(Routes.STATIONS, "Gares", Icons.Outlined.StarBorder, Icons.Filled.Star),
        Tab(Routes.TRAFFIC, "Trafic", Icons.Outlined.Notifications, Icons.Filled.Notifications),
        Tab(Routes.SETTINGS, "Réglages", Icons.Outlined.Settings, Icons.Filled.Settings),
    )

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    LaunchedEffect(Unit) {
        val ready = appVm.checkData()
        navController.navigate(if (ready) Routes.HOME else Routes.ONBOARDING) {
            popUpTo(Routes.SPLASH) { inclusive = true }
        }
    }

    Scaffold(
        bottomBar = {
            if (currentRoute in tabs.map { it.route }) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp,
                ) {
                    tabs.forEach { tab ->
                        val selected = currentRoute == tab.route
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    if (selected) tab.iconSelected else tab.icon,
                                    contentDescription = tab.label,
                                )
                            },
                            label = { Text(tab.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            enterTransition = {
                fadeIn(tween(220)) + slideInHorizontally(tween(260)) { it / 8 }
            },
            exitTransition = { fadeOut(tween(160)) },
            popEnterTransition = { fadeIn(tween(220)) },
            popExitTransition = {
                fadeOut(tween(160)) + slideOutHorizontally(tween(260)) { it / 8 }
            },
        ) {
            composable(Routes.SPLASH) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "EnTrain",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            composable(Routes.ONBOARDING) {
                OnboardingScreen(navController)
            }
            composable(Routes.HOME) {
                HomeScreen(navController)
            }
            composable(Routes.STATIONS) {
                StationsTab(navController)
            }
            composable(Routes.TRAFFIC) {
                TrafficScreen(navController)
            }
            composable(Routes.SETTINGS) {
                SettingsScreen()
            }
            composable(Routes.RESULTS) { entry ->
                val from = entry.arguments?.getString("from") ?: ""
                val to = entry.arguments?.getString("to") ?: ""
                val ymd = entry.arguments?.getString("ymd")?.toIntOrNull() ?: 0
                val sec = entry.arguments?.getString("sec")?.toIntOrNull() ?: 0
                val ab = entry.arguments?.getString("ab")?.toIntOrNull() ?: 0
                ResultsScreen(navController, from, to, ymd, sec, ab)
            }
            composable(Routes.JOURNEY) { entry ->
                val idx = entry.arguments?.getString("idx")?.toIntOrNull() ?: 0
                JourneyDetailScreen(navController, idx)
            }
            composable(Routes.PICKER) { entry ->
                val slot = entry.arguments?.getString("slot") ?: "from"
                StationPickerScreen(navController, slot)
            }
            composable(Routes.DEPARTURES) { entry ->
                val stationId = entry.arguments?.getString("stationId") ?: ""
                DeparturesScreen(navController, stationId)
            }
            composable(Routes.LIVE) {
                fr.webtvmedia.entrain.ui.live.LiveTripScreen(navController)
            }
        }
    }
}
