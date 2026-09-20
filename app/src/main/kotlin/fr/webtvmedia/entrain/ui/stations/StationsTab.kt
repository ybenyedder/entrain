package fr.webtvmedia.entrain.ui.stations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import fr.webtvmedia.entrain.EnTrainApp
import fr.webtvmedia.entrain.ui.Routes
import fr.webtvmedia.entrain.ui.components.SectionTitle
import fr.webtvmedia.entrain.ui.vm.VmFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationsTab(navController: NavController) {
    val context = LocalContext.current
    val app = context.applicationContext as EnTrainApp
    val vm: StationsTabViewModel = viewModel(factory = VmFactory(app))
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val recents by vm.recents.collectAsStateWithLifecycle()
    val popular by vm.popular.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vos gares") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = androidx.compose.ui.graphics.Color.White,
                ),
            )
        },
    ) { padding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            if (favorites.isNotEmpty()) {
                item { Spacer(Modifier.height(12.dp)); SectionTitle("Favoris") }
                items(favorites, key = { "f" + it.id }) { s ->
                    StationLine(s.name) { navController.navigate(Routes.departures(s.id)) }
                }
            }
            if (recents.isNotEmpty()) {
                item { Spacer(Modifier.height(16.dp)); SectionTitle("Récentes") }
                items(recents, key = { "r" + it.id }) { s ->
                    StationLine(s.name, icon = Icons.Filled.History) {
                        navController.navigate(Routes.departures(s.id))
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)); SectionTitle("Gares principales") }
            items(popular, key = { "p" + it.id }) { s ->
                StationLine(s.name) { navController.navigate(Routes.departures(s.id)) }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun StationLine(name: String, icon: androidx.compose.ui.graphics.vector.ImageVector = Icons.Filled.Place, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(34.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(name, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
    }
}
