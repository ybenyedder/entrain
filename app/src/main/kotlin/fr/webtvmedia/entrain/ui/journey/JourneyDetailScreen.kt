package fr.webtvmedia.entrain.ui.journey

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Schedule
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import fr.webtvmedia.entrain.domain.model.Journey
import fr.webtvmedia.entrain.domain.model.Leg
import fr.webtvmedia.entrain.ui.components.DelayBadge
import fr.webtvmedia.entrain.ui.components.TrainChip
import fr.webtvmedia.entrain.util.TimeUtils
import kotlinx.coroutines.launch
import java.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JourneyDetailScreen(navController: NavController, index: Int) {
    val context = LocalContext.current
    // les trajets sont conservés par le conteneur applicatif
    val app = context.applicationContext as fr.webtvmedia.entrain.EnTrainApp
    val journeys = app.container.lastJourneys
    val journey = journeys.getOrNull(index)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Détail du trajet") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                ),
            )
        },
    ) { padding ->
        if (journey == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Trajet indisponible", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            // résumé
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row {
                        Column(Modifier.weight(1f)) {
                            Text("Départ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(
                                TimeUtils.hhmm(journey.departureSec) + if (journey.departureDayOffset > 0) " J+1" else "",
                                style = MaterialTheme.typography.headlineMedium,
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Arrivée", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(
                                TimeUtils.hhmm(journey.arrivalSec) + if (journey.arrivalDayOffset > 0) " J+1" else "",
                                style = MaterialTheme.typography.headlineMedium,
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Durée", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text(
                                TimeUtils.duration(journey.durationSec),
                                style = MaterialTheme.typography.headlineMedium,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            journey.legs.forEachIndexed { legIdx, leg ->
                LegPane(leg) {
                    kotlinx.coroutines.MainScope().launch {
                        app.container.startLiveTrip(
                            leg.tripId,
                            leg.boardStopPointId,
                            leg.alightStopPointId,
                        )
                        navController.navigate(fr.webtvmedia.entrain.ui.Routes.LIVE)
                    }
                }
                if (legIdx < journey.legs.lastIndex) {
                    // étape de correspondance
                    val wait = journey.legs[legIdx + 1].let { next ->
                        val nextDep = next.departureSec + next.departureDayOffset * 86400
                        val prevArr = leg.arrivalSec + leg.arrivalDayOffset * 86400
                        nextDep - prevArr
                    }
                    Row(
                        Modifier.padding(start = 30.dp, top = 10.dp, bottom = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Correspondance à ${leg.alightStation} · ${TimeUtils.duration(wait.coerceAtLeast(0))}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    val from = journey.legs.first().boardStation
                    val to = journey.legs.last().alightStation
                    val date = java.time.LocalDate.now(TimeUtils.PARIS).toString()
                    val url = "https://www.sncf-connect.com/billet-train/recherche?from=${
                        Uri.encode(from)
                    }&to=${Uri.encode(to)}&date=${
                        Uri.encode(date)
                    }"
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text("Acheter un billet sur sncf-connect.com")
            }
            Text(
                "L'achat s'ouvre dans votre navigateur : EnTrain ne gère pas les paiements et n'y envoie aucune donnée.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 6.dp),
            )
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun LegPane(leg: Leg, onFollow: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TrainChip(leg.category)
            Spacer(Modifier.width(8.dp))
            Text(
                "${leg.trainNumber} vers ${leg.destination}",
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            androidx.compose.material3.TextButton(onClick = onFollow) {
                Icon(
                    Icons.Filled.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text("Suivre", style = MaterialTheme.typography.labelMedium)
            }
            DelayBadge(delaySec = leg.depDelaySec, cancelled = leg.cancelled)
        }
        Spacer(Modifier.height(10.dp))

        // ligne montée
        StopRow(
            time = leg.departureSec,
            dayOff = leg.departureDayOffset,
            station = leg.boardStation,
            dotColor = leg.category.color,
            filled = true,
            struck = leg.cancelled,
            estEpoch = leg.estDepartureEpoch,
        )
        // arrêts intermédiaires
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(Modifier.padding(start = 30.dp)) {
                Box(
                    Modifier
                        .padding(start = 13.dp)
                        .width(2.dp)
                        .height(6.dp)
                        .background(leg.category.color.copy(alpha = 0.25f)),
                )
                for (s in leg.intermediateStops) {
                    StopRowSmall(s.stationName, s.arrivalSec, s.dayOffset, leg.stopEstimates[s.stopPointId])
                }
            }
        }
        if (leg.intermediateStops.isNotEmpty()) {
            Row(
                Modifier
                    .padding(start = 30.dp, top = 4.dp, bottom = 4.dp)
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .padding(start = 13.dp)
                        .width(2.dp)
                        .height(10.dp)
                        .background(leg.category.color.copy(alpha = 0.25f)),
                )
                Spacer(Modifier.width(10.dp))
                Icon(
                    if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Masquer les arrêts" else "Voir les arrêts",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    if (expanded) "Masquer"
                    else "${leg.intermediateStops.size} arrêt" + if (leg.intermediateStops.size > 1) "s" else "",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        } else {
            Box(
                Modifier
                    .padding(start = 30.dp)
                    .padding(start = 13.dp)
                    .width(2.dp)
                    .height(12.dp)
                    .background(leg.category.color.copy(alpha = 0.25f)),
            )
        }
        StopRow(
            time = leg.arrivalSec,
            dayOff = leg.arrivalDayOffset,
            station = leg.alightStation,
            dotColor = leg.category.color,
            filled = true,
            struck = leg.cancelled,
            estEpoch = leg.estArrivalEpoch,
            isLast = true,
        )
    }
}

@Composable
private fun StopRow(
    time: Int,
    dayOff: Int,
    station: String,
    dotColor: Color,
    filled: Boolean,
    struck: Boolean = false,
    estEpoch: Long? = null,
    isLast: Boolean = false,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .size(14.dp)
                    .background(
                        if (filled) dotColor else MaterialTheme.colorScheme.surface,
                        CircleShape,
                    ),
            )
        }
        Spacer(Modifier.width(16.dp))
        Text(
            TimeUtils.hhmm(time) + if (dayOff > 0) " J+1" else "",
            style = MaterialTheme.typography.titleMedium,
            textDecoration = if (struck) TextDecoration.LineThrough else null,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                station,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (estEpoch != null) {
                val t = Instant.ofEpochSecond(estEpoch).atZone(TimeUtils.PARIS)
                val h = "${"%02d".format(t.hour)}:${"%02d".format(t.minute)}"
                Text(
                    "Estimé $h",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                )
            }
        }
        if (isLast) Spacer(Modifier.size(0.dp))
    }
}

@Composable
private fun StopRowSmall(station: String, time: Int, dayOff: Int, estEpoch: Long?) {
    Row(
        Modifier.padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(8.dp)
                .background(MaterialTheme.colorScheme.outline, CircleShape),
        )
        Spacer(Modifier.width(16.dp))
        val shown = estEpoch?.let {
            TimeUtils.epochToLocalSec(it)
        } ?: time
        Text(
            TimeUtils.hhmm(shown) + if (dayOff > 0) " J+1" else "",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(12.dp))
        Text(
            station,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
