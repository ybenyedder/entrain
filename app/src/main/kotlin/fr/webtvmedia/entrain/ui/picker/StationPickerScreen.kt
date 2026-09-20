package fr.webtvmedia.entrain.ui.picker

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import fr.webtvmedia.entrain.EnTrainApp
import fr.webtvmedia.entrain.data.db.FavoriteStationEntity
import fr.webtvmedia.entrain.domain.model.Station
import fr.webtvmedia.entrain.ui.components.SectionTitle
import fr.webtvmedia.entrain.ui.vm.VmFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StationPickerScreen(navController: NavController, slot: String) {
    val context = LocalContext.current
    val app = context.applicationContext as EnTrainApp
    val vm: PickerViewModel = viewModel(factory = VmFactory(app))
    val state by vm.state.collectAsStateWithLifecycle()
    val favorites by vm.favorites.collectAsStateWithLifecycle()
    val recents by vm.recents.collectAsStateWithLifecycle()
    val popular by vm.popular.collectAsStateWithLifecycle()

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    fun pick(s: Station) {
        val prev = navController.previousBackStackEntry
        prev?.savedStateHandle?.set("picked_id", s.id)
        prev?.savedStateHandle?.set("picked_name", s.name)
        prev?.savedStateHandle?.set("picked_slot", slot)
        kotlinx.coroutines.MainScope().launch {
            app.container.db.userPrefsDao().upsertRecent(
                fr.webtvmedia.entrain.data.db.RecentStationEntity(s.id, System.currentTimeMillis(), 1),
            )
        }
        navController.popBackStack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (slot == "from") "Gare de départ" else "Gare d'arrivée") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = vm::setQuery,
                placeholder = { Text("Rechercher une gare (Paris, Lyon…)") },
                leadingIcon = { Icon(Icons.Filled.Place, contentDescription = null) },
                trailingIcon = {
                    if (state.query.isNotEmpty()) {
                        IconButton(onClick = { vm.setQuery("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Effacer")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large,
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Search,
                    capitalization = KeyboardCapitalization.Words,
                ),
                keyboardActions = KeyboardActions(onSearch = {
                    state.results.firstOrNull()?.let { pick(it) }
                }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
            Spacer(Modifier.height(12.dp))

            when {
                state.searching -> Row(
                    Modifier.fillMaxWidth().padding(24.dp),
                    horizontalArrangement = Arrangement.Center,
                ) { CircularProgressIndicator() }

                state.query.isNotBlank() -> {
                    if (state.results.isEmpty()) {
                        fr.webtvmedia.entrain.ui.components.EmptyState(
                            "Aucune gare trouvée",
                            "Vérifiez l'orthographe ou essayez le nom d'une grande ville.",
                        )
                    } else {
                        LazyColumn {
                            items(state.results, key = { it.id }) { s ->
                                StationRow(s.name, onClick = { pick(s) })
                            }
                        }
                    }
                }

                else -> LazyColumn {
                    if (favorites.isNotEmpty()) {
                        item { SectionTitle("Favoris"); Spacer(Modifier.height(4.dp)) }
                        items(favorites, key = { "f" + it.id }) { f ->
                            StationRow(f.name, onClick = { pick(Station(f.id, f.name, f.lat, f.lon)) })
                        }
                    }
                    if (recents.isNotEmpty()) {
                        item { Spacer(Modifier.height(16.dp)); SectionTitle("Récentes"); Spacer(Modifier.height(4.dp)) }
                        items(recents, key = { "r" + it.id }) { r ->
                            StationRow(r.name, onClick = { pick(Station(r.id, r.name, r.lat, r.lon)) })
                        }
                    }
                    if (popular.isNotEmpty()) {
                        item { Spacer(Modifier.height(16.dp)); SectionTitle("Gares principales"); Spacer(Modifier.height(4.dp)) }
                        items(popular, key = { "p" + it.id }) { p ->
                            StationRow(p.name, onClick = { pick(p) })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StationRow(name: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp)
            .semantics(mergeDescendants = true) { contentDescription = "Choisir la gare $name" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(34.dp),
        ) {
            androidx.compose.foundation.layout.Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize(),
            ) {
                Icon(
                    Icons.Filled.Place,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(17.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(name, style = MaterialTheme.typography.bodyLarge)
    }
}
