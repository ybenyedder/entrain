package fr.webtvmedia.entrain.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.TripOrigin
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import fr.webtvmedia.entrain.EnTrainApp
import fr.webtvmedia.entrain.ui.Routes
import fr.webtvmedia.entrain.ui.components.SectionTitle
import fr.webtvmedia.entrain.ui.vm.VmFactory
import fr.webtvmedia.entrain.util.TimeUtils
import kotlinx.coroutines.delay

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    val context = LocalContext.current
    val app = context.applicationContext as EnTrainApp
    val vm: HomeViewModel = viewModel(factory = VmFactory(app))
    val state by vm.state.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val recents by vm.recents.collectAsStateWithLifecycle()
    val liveTrip by vm.liveTrip.collectAsStateWithLifecycle()

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    // retour du sélecteur de gare
    val backEntry = navController.currentBackStackEntry
    LaunchedEffect(backEntry) {
        val saved = backEntry?.savedStateHandle
        if (saved != null) {
            saved.getStateFlow<String?>("picked_id", null).collect { id ->
                if (id != null) {
                    val slot = saved.get<String>("picked_slot") ?: "from"
                    val name = saved.get<String>("picked_name") ?: ""
                    vm.setStation(slot, id, name)
                    saved["picked_id"] = null
                }
            }
        }
    }
    // rafraîchissement périodique du compteur de trafic + rafraîchit le formulaire au retour
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            vm.refreshAlerts()
        }
    }
    LaunchedEffect(Unit) { vm.refreshForm() }

    val today = vm.defaultYmd()

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
    ) {
        Spacer(Modifier.height(18.dp))
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    dateGreeting(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text("Où allez-vous ?", style = MaterialTheme.typography.headlineMedium)
            }
        }
        Spacer(Modifier.height(16.dp))

        // ---- Carte de recherche ----
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            shadowElevation = 4.dp,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp)) {
                StationField(
                    label = "Départ",
                    station = state.fromName,
                    placeholder = "D'où partez-vous ?",
                    onClick = { navController.navigate(Routes.picker("from")) },
                    onSwap = { vm.swap() },
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 52.dp)
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
                StationField(
                    label = "Arrivée",
                    station = state.toName,
                    placeholder = "Où allez-vous ?",
                    onClick = { navController.navigate(Routes.picker("to")) },
                )

                Spacer(Modifier.height(12.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    androidx.compose.material3.FilterChip(
                        selected = state.timeMode == 0,
                        onClick = { vm.setTimeMode(0) },
                        label = { Text("Partir à") },
                        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                    androidx.compose.material3.FilterChip(
                        selected = state.timeMode == 1,
                        onClick = { vm.setTimeMode(1) },
                        label = { Text("Arriver à") },
                        colors = androidx.compose.material3.FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = Color.White,
                        ),
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(10.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    DateChip(
                        ymd = if (state.ymd == 0) today else state.ymd,
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1.4f),
                    )
                    TimeChip(
                        timeSec = state.timeSec,
                        arriveMode = state.timeMode == 1,
                        onClick = { showTimePicker = true },
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(14.dp))

                Button(
                    onClick = {
                        val f = fr.webtvmedia.entrain.ui.vm.SearchForm
                        val ymd = if (state.ymd == 0) today else state.ymd
                        val sec = if (state.timeSec < 0) nowSec() else state.timeSec
                        navController.navigate(
                            Routes.results(f.fromId ?: return@Button, f.toId ?: return@Button, ymd, sec, f.timeMode),
                        )
                    },
                    enabled = state.fromName != null && state.toName != null,
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                ) {
                    Icon(Icons.Filled.Search, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Rechercher", style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        // ---- Voyage en cours ----
        if (liveTrip != null) {
            Spacer(Modifier.height(16.dp))
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate(Routes.LIVE) },
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.Train,
                        contentDescription = null,
                        tint = Color.White,
                    )
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "Voyage en cours",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.8f),
                        )
                        Text(
                            "Suivre votre train",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        "Voir",
                        style = MaterialTheme.typography.labelLarge,
                        color = Color.White,
                    )
                }
            }
        }

        // ---- Bandeau trafic ----
        if (state.activeAlerts > 0) {
            Spacer(Modifier.height(16.dp))
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { navController.navigate(Routes.TRAFFIC) },
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.Notifications,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "Trafic : ${state.activeAlerts} ${state.alertWording} en cours",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        "Voir",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        // ---- Favoris ----
        if (favorites.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            SectionTitle("Vos gares")
            Spacer(Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(favorites, key = { it.id }) { fav ->
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.clickable {
                            navController.navigate(Routes.departures(fav.id))
                        },
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Filled.Place,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                fav.name,
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }

        // ---- Récentes ----
        if (recents.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            SectionTitle("Gares récentes")
            Spacer(Modifier.height(6.dp))
            for (r in recents.take(5)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { navController.navigate(Routes.departures(r.id)) }
                        .padding(horizontal = 4.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Filled.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(r.name, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        Spacer(Modifier.height(28.dp))
    }

    if (showDatePicker) {
        val dp = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    dp.selectedDateMillis?.let { ms ->
                        val d = java.time.Instant.ofEpochMilli(ms).atZone(TimeUtils.PARIS).toLocalDate()
                        vm.setDate(TimeUtils.ymd(d))
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Annuler") } },
        ) {
            DatePicker(state = dp)
        }
    }

    if (showTimePicker) {
        val now = java.time.LocalTime.now(TimeUtils.PARIS)
        val tp = rememberTimePickerState(initialHour = now.hour, initialMinute = now.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    vm.setTime(tp.hour * 3600 + tp.minute * 60)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = {
                    vm.setTime(-1)
                    showTimePicker = false
                }) { Text("Dès que possible") }
            },
            title = { Text(if (state.timeMode == 1) "Heure d'arrivée souhaitée" else "Heure de départ") },
            text = { TimePicker(state = tp) },
        )
    }
}

@Composable
private fun DateChip(ymd: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val date = java.time.LocalDate.of(ymd / 10000, (ymd % 10000) / 100, ymd % 100)
    val today = java.time.LocalDate.now(TimeUtils.PARIS)
    val label = when {
        date == today -> "Aujourd'hui"
        date == today.plusDays(1) -> "Demain"
        else -> date.format(java.time.format.DateTimeFormatter.ofPattern("EEE d MMM", java.util.Locale.FRENCH))
    }
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.CalendarMonth,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun TimeChip(timeSec: Int, arriveMode: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier.clickable(onClick = onClick),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Filled.Schedule,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(6.dp))
            val label = when {
                arriveMode && timeSec >= 0 -> "Avant ${TimeUtils.hhmm(timeSec)}"
                timeSec < 0 -> "Dès que possible"
                else -> TimeUtils.hhmm(timeSec)
            }
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun StationField(
    label: String,
    station: String?,
    placeholder: String,
    onClick: () -> Unit,
    onSwap: (() -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (label == "Départ") Icons.Filled.TripOrigin else Icons.Filled.Place,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            AnimatedContent(
                targetState = station ?: placeholder,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(120)) },
                label = "station",
            ) { value ->
                Text(
                    value,
                    style = if (station == null) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleMedium,
                    color = if (station == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        if (onSwap != null) {
            SwapButton(onSwap)
        }
    }
}

@Composable
private fun SwapButton(onSwap: () -> Unit) {
    var rotated by remember { androidx.compose.runtime.mutableStateOf(false) }
    val angle by animateFloatAsState(if (rotated) 180f else 0f, tween(300), label = "swap")
    IconButton(
        onClick = {
            rotated = !rotated
            onSwap()
        },
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
    ) {
        Icon(
            Icons.Filled.SwapVert,
            contentDescription = "Inverser départ et arrivée",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.rotate(angle),
        )
    }
}

private fun nowSec(): Int {
    val t = java.time.LocalTime.now(TimeUtils.PARIS)
    return t.hour * 3600 + t.minute * 60
}

private fun dateGreeting(): String {
    val d = java.time.LocalDate.now(TimeUtils.PARIS)
    val fmt = java.time.format.DateTimeFormatter.ofPattern("EEEE d MMMM", java.util.Locale.FRENCH)
    return d.format(fmt).replaceFirstChar { it.uppercase(java.util.Locale.FRENCH) }
}
