package fr.webtvmedia.entrain.ui.results

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import fr.webtvmedia.entrain.EnTrainApp
import fr.webtvmedia.entrain.domain.model.Journey
import fr.webtvmedia.entrain.ui.Routes
import fr.webtvmedia.entrain.ui.components.EmptyState
import fr.webtvmedia.entrain.ui.vm.VmFactory
import fr.webtvmedia.entrain.util.TimeUtils
import kotlinx.coroutines.delay

private enum class SortMode(val label: String) {
    DEPART("Départ"),
    ARRIVEE("Arrivée"),
    DUREE("Durée"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultsScreen(
    navController: NavController,
    fromId: String,
    toId: String,
    ymd: Int,
    timeSec: Int,
    arriveBy: Int = 0,
) {
    val context = LocalContext.current
    val app = context.applicationContext as EnTrainApp
    val vm: ResultsViewModel = viewModel(factory = VmFactory(app))
    val state by vm.state.collectAsStateWithLifecycle()
    var sortMode by remember { mutableStateOf(SortMode.DEPART) }

    // rafraîchissement temps réel périodique
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            vm.refreshRealtime()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val ready = state as? ResultsState.Ready
                    Column {
                        Text(
                            "${ready?.fromName ?: ""} → ${ready?.toName ?: ""}",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (ready != null) {
                            val fmt = java.time.format.DateTimeFormatter.ofPattern("EEE d MMMM", java.util.Locale.FRENCH)
                            Text(
                                ready.date.format(fmt).replaceFirstChar { it.uppercase() } +
                                    if (ready.timeSec >= 0) {
                                        if (arriveBy == 1) " · arrivée avant ${TimeUtils.hhmm(ready.timeSec)}"
                                        else " · ${TimeUtils.hhmm(ready.timeSec)}"
                                    } else "",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.85f),
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.load() }) {
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
            isRefreshing = state is ResultsState.Loading,
            onRefresh = { vm.refreshRealtime() },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (val s = state) {
                ResultsState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                ResultsState.Empty -> EmptyState(
                    "Aucun trajet trouvé",
                    "Aucune correspondance entre ces gares sur cette période.\nEssayez une autre date ou une gare plus proche.",
                )
                is ResultsState.Error -> EmptyState("Erreur", s.message)
                is ResultsState.Ready -> {
                    val journeys = remember(s, sortMode) {
                        when (sortMode) {
                            SortMode.DEPART -> s.journeys.sortedBy { it.totalDeparture }
                            SortMode.ARRIVEE -> s.journeys.sortedBy { it.totalArrival }
                            SortMode.DUREE -> s.journeys.sortedBy { it.durationSec }
                        }
                    }
                    val fastestKey = remember(s) {
                        s.journeys.minByOrNull { it.durationSec }?.let { j ->
                            j.legs.joinToString("|") { it.tripId } + "#" + j.totalDeparture
                        }
                    }
                    Column {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                        ) {
                            SortMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = sortMode == mode,
                                    onClick = { sortMode = mode },
                                    label = { Text(mode.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = Color.White,
                                    ),
                                )
                            }
                        }
                        LazyColumn(
                            contentPadding = PaddingValues(
                                start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp,
                            ),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            itemsIndexed(
                                journeys,
                                key = { _, j -> j.totalDeparture.toLong() * 1000 + j.legs.size },
                            ) { idx, j ->
                                val isFastest = fastestKey ==
                                    j.legs.joinToString("|") { it.tripId } + "#" + j.totalDeparture
                                StaggeredEntrance(index = idx, modifier = Modifier.animateItem()) {
                                    JourneyCard(j, isBest = isFastest) {
                                        navController.navigate(Routes.journey(journeys.indexOf(j)))
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

/** Apparition décalée (fondu + glissement vertical) des cartes de liste. */
@Composable
fun StaggeredEntrance(index: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { shown = true }
    val progress by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(durationMillis = 280, delayMillis = (index * 45).coerceAtMost(360)),
        label = "entrance",
    )
    Box(
        modifier
            .alpha(progress.coerceIn(0f, 1f))
            .graphicsLayer { translationY = (1f - progress) * 40.dp.toPx() },
    ) {
        content()
    }
}

@Composable
fun JourneyCard(j: Journey, isBest: Boolean, onClick: () -> Unit) {
    val firstLeg = j.legs.first()
    val hasRealtime = j.legs.any { it.depDelaySec != null || it.cancelled }
    val maxDelay = j.legs.maxOfOrNull { it.depDelaySec ?: 0 } ?: 0
    val anyCancelled = j.legs.any { it.cancelled }
    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = buildString {
                    append("Trajet, départ ${TimeUtils.hhmm(j.departureSec)}, arrivée ${TimeUtils.hhmm(j.arrivalSec)}")
                    if (j.arrivalDayOffset > 0) append(" le lendemain")
                    append(", durée ${TimeUtils.duration(j.durationSec)}")
                    append(if (j.isDirect) ", direct" else ", ${j.transfers} correspondances")
                    if (isBest) append(", le plus rapide")
                    append(", " + j.legs.joinToString(" puis ") { "${it.category.label} n° ${it.trainNumber}" })
                    when {
                        anyCancelled -> append(", train supprimé")
                        maxDelay >= 120 -> append(", retard de ${maxDelay / 60} minutes")
                        hasRealtime -> append(", à l'heure")
                    }
                }
            }
            .clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                fr.webtvmedia.entrain.ui.results.TimeColumn(
                    dep = j.departureSec,
                    depOff = j.departureDayOffset,
                    arr = j.arrivalSec,
                    arrOff = j.arrivalDayOffset,
                    depStruck = firstLeg.cancelled,
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            TimeUtils.duration(j.durationSec),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        if (isBest) {
                            Spacer(Modifier.width(8.dp))
                            Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                                Text(
                                    "Le plus rapide",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                )
                            }
                        }
                    }
                    Text(
                        if (j.isDirect) "Direct" else "${j.transfers} correspondance" + if (j.transfers > 1) "s" else "",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    val est = firstLeg.estDepartureEpoch
                    if (est != null && !firstLeg.cancelled) {
                        val delay = firstLeg.depDelaySec ?: 0
                        val late = delay >= 120
                        Text(
                            if (late) "Départ estimé ${TimeUtils.hhmm(TimeUtils.epochToLocalSec(est))}" else "À l'heure",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (late) MaterialTheme.colorScheme.error
                            else fr.webtvmedia.entrain.ui.theme.LocalStatusColors.current.onTime,
                        )
                    }
                }
                androidx.compose.animation.AnimatedVisibility(visible = hasRealtime) {
                    fr.webtvmedia.entrain.ui.components.DelayBadge(
                        delaySec = j.legs.maxOfOrNull { it.depDelaySec ?: 0 },
                        cancelled = j.legs.any { it.cancelled },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for ((i, leg) in j.legs.withIndex()) {
                    fr.webtvmedia.entrain.ui.components.TrainChip(leg.category)
                    if (i < j.legs.lastIndex) {
                        Text(
                            "·",
                            color = MaterialTheme.colorScheme.outline,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
internal fun TimeColumn(dep: Int, depOff: Int, arr: Int, arrOff: Int, depStruck: Boolean) {
    Column {
        Text(
            TimeUtils.hhmm(dep),
            style = MaterialTheme.typography.headlineSmall,
            textDecoration = if (depStruck) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            TimeUtils.hhmm(arr) + if (arrOff > 0) " J+1" else "",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
