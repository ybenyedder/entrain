package fr.webtvmedia.entrain.ui.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import fr.webtvmedia.entrain.EnTrainApp
import fr.webtvmedia.entrain.data.gtfs.ImportProgress
import fr.webtvmedia.entrain.ui.Routes
import fr.webtvmedia.entrain.ui.theme.Violet20
import fr.webtvmedia.entrain.ui.theme.Violet40
import fr.webtvmedia.entrain.ui.vm.VmFactory

@Composable
fun OnboardingScreen(navController: NavController) {
    val context = LocalContext.current
    val app = context.applicationContext as EnTrainApp
    val vm: OnboardingViewModel = viewModel(factory = VmFactory(app))
    val progress by vm.progress.collectAsStateWithLifecycle()
    val busy by vm.busy.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        var navigated = false
        fun go() {
            if (!navigated) {
                navigated = true
                navController.navigate(Routes.HOME) {
                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                }
            }
        }
        vm.progress.collect { p ->
            when {
                // l'import national est terminé : on rentre dans l'app,
                // le pont Île-de-France continue en arrière-plan
                p is ImportProgress.Importing && p.file == "idfm" -> go()
                p is ImportProgress.Done -> go()
                else -> {}
            }
        }
    }
    // reconstruction de l'index en tâche de fond une fois l'import national terminé
    LaunchedEffect(Unit) {
        vm.progress.collect { p ->
            if (p is ImportProgress.Done ||
                (p is ImportProgress.Importing && p.file == "idfm")
            ) {
                app.container.rebuildIndex()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Violet20, Violet40)))
                .padding(horizontal = 28.dp, vertical = 44.dp),
        ) {
            Column {
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Filled.Train,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(30.dp),
                    )
                }
                Spacer(Modifier.height(20.dp))
                Text(
                    "Bienvenue sur EnTrain",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Horaires, temps réel et perturbations des trains SNCF.\nSans compte. Sans traceur.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.92f),
                )
            }
        }

        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            FeatureRow(Icons.Filled.Schedule, "Horaires et temps réel", "TGV INOUI, OUIGO, TER, INTERCITÉS — retards à la minute.")
            FeatureRow(Icons.Filled.Warning, "Perturbations", "Travaux, grèves et infos trafic en direct.")
            FeatureRow(Icons.Filled.Lock, "Zéro traceur", "Aucune publicité, aucun profilage. Les favoris restent sur votre téléphone.")

            Spacer(Modifier.height(28.dp))

            when (val p = progress) {
                null -> {
                    Button(
                        onClick = { vm.startImport() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = MaterialTheme.shapes.large,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    ) {
                        Text("Télécharger les horaires", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Un téléchargement d'environ 4 Mo, une seule fois.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                is ImportProgress.Downloading -> ProgressPane("Téléchargement des horaires…", p.percent)
                ImportProgress.Parsing -> ProgressPane("Lecture des données…", 0)
                is ImportProgress.Importing -> ProgressPane(
                    if (p.file == "idfm") "Préparation du temps réel Île-de-France…" else "Import en base…",
                    p.percent,
                )
                ImportProgress.Finishing -> ProgressPane("Finalisation…", 100)
                is ImportProgress.Done -> ProgressPane("Terminé !", 100)
                is ImportProgress.Failed -> {
                    Text(
                        "Échec : ${p.message}",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(12.dp))
                    Button(
                        onClick = { vm.startImport() },
                        enabled = !busy,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    ) {
                        Text("Réessayer")
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Text(
                "Données ouvertes : SNCF / transport.data.gouv.fr (licence Etalab).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, text: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = title, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(16.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ProgressPane(label: String, percent: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (percent <= 0) {
            CircularProgressIndicator(modifier = Modifier.size(40.dp))
        } else {
            val anim by animateFloatAsState(percent / 100f, tween(300), label = "progress")
            LinearProgressIndicator(
                progress = { anim },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(MaterialTheme.shapes.small),
            )
        }
        Spacer(Modifier.height(12.dp))
        Text(label, style = MaterialTheme.typography.bodyMedium)
    }
}
