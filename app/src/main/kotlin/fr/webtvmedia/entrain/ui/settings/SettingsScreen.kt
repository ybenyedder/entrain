package fr.webtvmedia.entrain.ui.settings

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Button
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.webtvmedia.entrain.EnTrainApp
import fr.webtvmedia.entrain.ui.components.SectionTitle
import fr.webtvmedia.entrain.ui.vm.VmFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val app = context.applicationContext as EnTrainApp
    val vm: SettingsViewModel = viewModel(factory = VmFactory(app))
    val state by vm.state.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Réglages") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                ),
            )
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(16.dp))
            SectionTitle("Données")
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    SettingLine("Version des horaires", state.gtfsVersion ?: "—")
                    SettingLine("Mise à jour", state.importDate ?: "—")
                    if (state.updateStatus != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            state.updateStatus ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { vm.updateTimetable() },
                        enabled = !state.updating,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (state.updating) "Mise à jour…" else "Mettre à jour les horaires")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            SectionTitle("Transilien & RER — temps réel")
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Pour les prochains passages des trains Transilien et RER en Île-de-France, " +
                            "EnTrain interroge la plateforme open data PRIM d'Île-de-France Mobilités, " +
                            "qui demande une clé gratuite liée à votre compte personnel.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(10.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = state.primKey,
                        onValueChange = { vm.setPrimKey(it) },
                        placeholder = { Text("Clé API PRIM (optionnelle)") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    androidx.compose.material3.TextButton(onClick = { vm.savePrimKey() }) {
                        Text(if (state.primKeySaved) "Clé enregistrée ✓" else "Enregistrer la clé")
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Obtenir une clé : prim.iledefrance-mobilites.fr → compte gratuit → section « clé API ». " +
                            "La clé reste dans votre téléphone ; les autres données (TGV, TER, Intercités) " +
                            "fonctionnent sans clé et sans compte.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            SectionTitle("Confidentialité")
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text("EnTrain ne collecte rien", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Aucun traceur, aucune statistique, aucune publicité. " +
                            "L'application ne parle qu'aux flux open data SNCF et Île-de-France Mobilités, " +
                            "sans compte ni identifiant. Favoris et recherches restent dans le stockage local du téléphone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
            SectionTitle("À propos")
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 1.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp)) {
                    SettingLine("Application", "EnTrain 0.1.0")
                    SettingLine("Sources", "SNCF open data · transport.data.gouv.fr")
                    SettingLine("Licence données", "Etalab 2.0")
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "EnTrain est une application indépendante, non affiliée à la SNCF. " +
                            "Les horaires et informations temps réel proviennent des données ouvertes publiées par SNCF Voyageurs.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingLine(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}
