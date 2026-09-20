package fr.webtvmedia.entrain.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Search
import fr.webtvmedia.entrain.domain.model.TrainCategory
import fr.webtvmedia.entrain.ui.theme.LocalStatusColors

/** Pastille catégorie de train (TGV INOUI, OUIGO, TER…), couleurs produit. */
@Composable
fun TrainChip(category: TrainCategory, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(7.dp),
        color = category.color,
        shadowElevation = 1.dp,
    ) {
        Text(
            text = category.label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 11.sp,
                letterSpacing = 0.2.sp,
                fontWeight = FontWeight(700),
            ),
            color = Color.White,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Badge temps réel : "à l'heure" (vert plein), "retard X min" (rouge plein), "supprimé". */
@Composable
fun DelayBadge(delaySec: Int?, cancelled: Boolean, modifier: Modifier = Modifier) {
    val status = LocalStatusColors.current
    if (cancelled) {
        BadgePill(color = status.cancelled, text = "Supprimé", icon = Icons.Filled.Warning, modifier = modifier)
    } else if (delaySec != null) {
        val min = delaySec / 60
        when {
            min <= 0 -> BadgePill(color = status.onTime, text = "À l'heure", icon = Icons.Filled.Check, modifier = modifier)
            min < 60 -> BadgePill(color = status.late, text = "+$min min", modifier = modifier)
            else -> BadgePill(color = status.late, text = "+${min / 60} h ${"%02d".format(min % 60)}", modifier = modifier)
        }
    }
}

@Composable
private fun BadgePill(color: Color, text: String, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(7.dp),
        color = color,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(11.dp))
            }
            Text(
                text,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight(700),
                ),
                color = Color.White,
            )
        }
    }
}

/** Trait vertical d'une timeline d'étape. */
@Composable
fun TimelineLine(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier
            .width(2.dp)
            .background(color.copy(alpha = 0.25f), RoundedCornerShape(1.dp)),
    )
}

/** Titre de section à la SNCF Connect : petit gras violet + trait. */
@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth(),
    ) {
        Box(
            Modifier
                .size(width = 3.dp, height = 14.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp)),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
    }
}

/** État vide illustré par un simple pictogramme + texte. */
@Composable
fun EmptyState(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 48.dp),
    ) {
        Box(
            Modifier
                .size(64.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Outlined.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(30.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
        if (subtitle != null) {
            Spacer(Modifier.height(6.dp))
            Text(
                subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}
