package fr.webtvmedia.entrain.ui.live

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import fr.webtvmedia.entrain.EnTrainApp
import fr.webtvmedia.entrain.ui.components.DelayBadge
import fr.webtvmedia.entrain.ui.components.TrainChip
import fr.webtvmedia.entrain.ui.vm.VmFactory
import fr.webtvmedia.entrain.util.TimeUtils
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTripScreen(navController: NavController) {
    val context = LocalContext.current
    val app = context.applicationContext as EnTrainApp
    val vm: LiveTripViewModel = viewModel(factory = VmFactory(app))
    val state by vm.state.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            vm.refresh()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voyage en cours") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                actions = {
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
        when {
            state.loading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }

            !state.active -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    "Aucun voyage suivi.\nTouchez « Suivre ce train » sur un trajet ou un départ.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
            }

            else -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                // ---- carte d'état ----
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TrainChip(state.category)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "n° ${state.trainNumber} · vers ${state.destination}",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f),
                            )
                            DelayBadge(delaySec = state.delaySec, cancelled = state.cancelled)
                        }
                        Spacer(Modifier.height(12.dp))
                        if (state.cancelled) {
                            Text(
                                "Train supprimé — reportez-vous sur un autre train.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error,
                            )
                        } else if (state.finished) {
                            Text(
                                "Voyage terminé — arrivée effectuée.",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        } else {
                            Text(
                                state.nextStopName?.let { "Prochain arrêt : $it" } ?: "En route…",
                                style = MaterialTheme.typography.headlineSmall,
                            )
                            state.progress?.let { p ->
                                Spacer(Modifier.height(8.dp))
                                val anim by androidx.compose.animation.core.animateFloatAsState(
                                    p,
                                    androidx.compose.animation.core.tween(900),
                                    label = "progress",
                                )
                                androidx.compose.material3.LinearProgressIndicator(
                                    progress = { anim },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = state.category.color,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                )
                            }
                            if (state.arrivalEtaSec != null) {
                                Spacer(Modifier.height(4.dp))
                                val mineLabel = buildString {
                                    append("Arrivée estimée à votre gare : ")
                                    append(TimeUtils.hhmm(state.arrivalEtaSec!!))
                                    state.arrivalDelaySec?.let {
                                        if (it >= 120) append(" (+${it / 60} min)")
                                        else if (it in 1..119) append(" (+${it / 60} min)")
                                    }
                                }
                                Text(mineLabel, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // perturbations touchant ce train
                if (state.alerts.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.errorContainer,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(Modifier.padding(14.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    androidx.compose.material.icons.Icons.Filled.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Perturbation sur ce train",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                            for (a in state.alerts.take(2)) {
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    a.header,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }

                // ---- timeline des arrêts ----
                state.stops.forEachIndexed { i, s ->
                    LiveStopRow(s, lineColor = state.category.color)
                }

                Spacer(Modifier.height(20.dp))
                OutlinedButton(
                    onClick = { vm.stop() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Terminer le suivi")
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun LiveStopRow(s: LiveStop, lineColor: Color) {
    val shownSec = s.estimatedEpoch?.let { TimeUtils.epochToLocalSec(it) } ?: s.scheduledSec
    val isEstimate = s.estimatedEpoch != null && shownSec != (s.scheduledSec % 86400)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = buildString {
                    append(s.stationName)
                    append(", ")
                    append(if (s.passed) "déjà desservie à " else "à ")
                    append(TimeUtils.hhmm(shownSec))
                    if (isEstimate) append(", horaire estimé")
                    if (s.isNext) append(", prochain arrêt")
                    if (s.isMine) append(", votre gare d'arrivée")
                    if (s.skipped) append(", arrêt supprimé")
                }
            },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(14.dp)
                    .background(
                        when {
                            s.isNext -> lineColor
                            s.passed -> MaterialTheme.colorScheme.outlineVariant
                            else -> lineColor.copy(alpha = 0.4f)
                        },
                        CircleShape,
                    ),
            )
        }
        Spacer(Modifier.width(14.dp))
        Text(
            TimeUtils.hhmm(shownSec),
            style = MaterialTheme.typography.titleMedium,
            textDecoration = if (s.skipped) TextDecoration.LineThrough else null,
            color = if (s.passed && !s.isMine) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    s.stationName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (s.isNext || s.isMine) androidx.compose.ui.text.font.FontWeight(700)
                    else androidx.compose.ui.text.font.FontWeight(400),
                    textDecoration = if (s.skipped) TextDecoration.LineThrough else null,
                )
                if (s.isMine) {
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            AnimatedVisibility(visible = s.isNext) {
                Text(
                    "Prochain arrêt",
                    style = MaterialTheme.typography.labelSmall,
                    color = lineColor,
                )
            }
        }
    }
}
