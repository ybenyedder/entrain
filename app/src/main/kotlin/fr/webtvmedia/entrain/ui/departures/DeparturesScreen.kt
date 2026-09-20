package fr.webtvmedia.entrain.ui.departures

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import fr.webtvmedia.entrain.EnTrainApp
import fr.webtvmedia.entrain.domain.model.Departure
import fr.webtvmedia.entrain.ui.components.EmptyState
import fr.webtvmedia.entrain.ui.components.TrainChip
import fr.webtvmedia.entrain.ui.vm.VmFactory
import fr.webtvmedia.entrain.util.TimeUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeparturesScreen(navController: NavController, stationId: String) {
    val context = LocalContext.current
    val app = context.applicationContext as EnTrainApp
    val vm: DeparturesViewModel = viewModel(factory = VmFactory(app))
    val state by vm.state.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(45_000)
            vm.refresh()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.stationName, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            "Affichage en temps réel",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.85f),
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.toggleFavorite() }) {
                        Icon(
                            if (state.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = if (state.isFavorite) "Retirer des favoris" else "Ajouter aux favoris",
                        )
                    }
                    IconButton(onClick = { vm.refresh() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Actualiser")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White,
                ),
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.loading,
            onRefresh = { vm.refresh() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
        Column(
            Modifier
                .fillMaxSize(),
        ) {
            TabRow(
                selectedTabIndex = tab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
            ) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Départs") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Arrivées") })
            }

            val list = if (tab == 0) state.departures else state.arrivals

            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    androidx.compose.material3.CircularProgressIndicator()
                }
                list.isEmpty() -> EmptyState(
                    if (tab == 0) "Aucun départ prévu" else "Aucune arrivée prévue",
                    "Dans les prochaines heures.",
                )
                else -> LazyColumn(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp, vertical = 8.dp,
                    ),
                ) {
                    itemsIndexed(list, key = { i, d -> d.tripId + d.stopPointId + i }) { i, d ->
                        fr.webtvmedia.entrain.ui.results.StaggeredEntrance(index = i) {
                            DepartureRow(d, onFollow = {
                                kotlinx.coroutines.MainScope().launch {
                                    app.container.followTripToEnd(d.tripId, d.stopPointId)
                                    navController.navigate(fr.webtvmedia.entrain.ui.Routes.LIVE)
                                }
                            })
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun DepartureRow(d: Departure, onFollow: (() -> Unit)? = null) {
    val statusColor = when {
        d.cancelled -> MaterialTheme.colorScheme.error
        d.delayedMin != null && d.delayedMin > 0 -> MaterialTheme.colorScheme.error
        d.delayedMin != null -> fr.webtvmedia.entrain.ui.theme.LocalStatusColors.current.onTime
        else -> MaterialTheme.colorScheme.onSurface
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = buildString {
                    append("${d.category.label} vers ${d.destination}")
                    d.lineLabel?.let { append(", ligne $it") }
                    append(", départ à ${TimeUtils.hhmm(d.scheduledSec)}")
                    if (d.cancelled) append(", supprimé")
                    d.delayedMin?.takeIf { it > 0 }?.let { append(", retard de $it minutes") }
                    if (onFollow != null) append(". Bouton suivre le train en fin de ligne")
                }
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val estSec = d.estimatedEpoch?.let { TimeUtils.epochToLocalSec(it) }
        Column {
            if (estSec != null && !d.cancelled) {
                Text(
                    TimeUtils.hhmm(estSec),
                    style = MaterialTheme.typography.titleLarge,
                    color = statusColor,
                )
                Text(
                    TimeUtils.hhmm(d.scheduledSec),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textDecoration = TextDecoration.LineThrough,
                )
            } else {
                Text(
                    TimeUtils.hhmm(d.scheduledSec),
                    style = MaterialTheme.typography.titleLarge,
                    textDecoration = if (d.cancelled) TextDecoration.LineThrough else null,
                    color = if (d.cancelled) statusColor else MaterialTheme.colorScheme.onSurface,
                )
                if (d.cancelled) {
                    Text(
                        "Supprimé",
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor,
                    )
                }
            }
        }
        Spacer(Modifier.width(16.dp))
        TrainChip(d.category)
        if (d.lineLabel != null) {
            Spacer(Modifier.width(6.dp))
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Text(
                    d.lineLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = androidx.compose.ui.text.font.FontWeight(700),
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 3.dp),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                d.destination,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = androidx.compose.ui.text.font.FontWeight(600),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (d.trainNumber.isNotBlank()) {
                Text(
                    "n° ${d.trainNumber}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (!d.cancelled && d.delayedMin != null && d.delayedMin > 0) {
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                color = MaterialTheme.colorScheme.errorContainer,
            ) {
                Text(
                    "+${d.delayedMin} min",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                )
            }
        }
        if (onFollow != null) {
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onFollow, modifier = Modifier.size(40.dp)) {
                Icon(
                    Icons.Filled.LocationOn,
                    contentDescription = "Suivre le train n° ${d.trainNumber} vers ${d.destination}",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}
