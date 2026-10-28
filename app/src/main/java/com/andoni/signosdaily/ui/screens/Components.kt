package com.andoni.signosdaily.ui.screens

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andoni.signosdaily.data.Expression
import com.andoni.signosdaily.data.StudyItem
import com.andoni.signosdaily.data.Word
import com.andoni.signosdaily.domain.Achievements
import com.andoni.signosdaily.update.UpdateInfo
import java.net.URLEncoder

/** The main study card shown on the home screen and in the library. */
@Composable
fun StudyCard(
    item: StudyItem,
    isFavorite: Boolean,
    isLearned: Boolean,
    speed: Float,
    mirror: Boolean,
    onSpeedChange: (Float) -> Unit,
    onMirrorChange: (Boolean) -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleLearned: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
    usedIn: List<Expression> = emptyList(),
    onOpenItem: (StudyItem) -> Unit = {},
) {
    val isWord = item is Word
    val accent = if (isWord) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary
    val accentContainer =
        if (isWord) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.tertiaryContainer
    val onAccentContainer =
        if (isWord) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onTertiaryContainer

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        // Header band
        Column(
            Modifier
                .fillMaxWidth()
                .background(accentContainer)
                .padding(horizontal = 20.dp, vertical = 18.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (isWord) "SIGNO" else "EXPRESIÓN",
                    style = MaterialTheme.typography.labelMedium,
                    color = onAccentContainer,
                    letterSpacing = 1.5.sp,
                )
                Spacer(Modifier.weight(1f))
                Surface(shape = RoundedCornerShape(50), color = accent) {
                    Text(
                        item.category,
                        Modifier.padding(horizontal = 10.dp, vertical = 2.dp),
                        color = if (isWord) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onTertiary,
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                item.headword,
                style = if (isWord) MaterialTheme.typography.displaySmall else MaterialTheme.typography.headlineMedium,
                color = onAccentContainer,
            )
        }

        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SignPlayer(
                signs = item.signs,
                speed = speed,
                mirror = mirror,
                onSpeedChange = onSpeedChange,
                onMirrorChange = onMirrorChange,
            )

            if (!isWord) {
                Column(Modifier.padding(horizontal = 4.dp)) {
                    Text("En LSE, signo a signo", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(item.gloss, style = MaterialTheme.typography.titleLarge, color = accent)
                }
            }

            if (item.tip.isNotBlank()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .padding(12.dp),
                ) {
                    Icon(Icons.Outlined.Lightbulb, null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                    Spacer(Modifier.width(8.dp))
                    Text(item.tip, style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }

            if (usedIn.isNotEmpty()) {
                Column(Modifier.padding(horizontal = 4.dp)) {
                    Text("Lo usarás en", style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    usedIn.take(3).forEach { e ->
                        Text(
                            "«${e.headword}»  ·  ${e.gloss}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onOpenItem(e) }
                                .padding(vertical = 4.dp),
                        )
                    }
                }
            }

            Text(
                if (isWord) "Repítelo frente al espejo: fíjate en la forma de la mano, dónde se coloca, cómo se mueve y la expresión de la cara."
                else "El vídeo enseña cada signo por separado. Al signar seguido, los signos se enlazan y la cara marca si es pregunta, negación o exclamación.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )

            HorizontalDivider()

            Row(verticalAlignment = Alignment.CenterVertically) {
                AssistChip(
                    onClick = onToggleLearned,
                    label = { Text(if (isLearned) "Aprendido" else "Marcar aprendido") },
                    leadingIcon = {
                        Icon(
                            if (isLearned) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                            null,
                            Modifier.size(AssistChipDefaults.IconSize),
                        )
                    },
                    colors = if (isLearned) AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        leadingIconContentColor = MaterialTheme.colorScheme.primary,
                    ) else AssistChipDefaults.assistChipColors(),
                )
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        if (isFavorite) "Quitar de favoritos" else "Añadir a favoritos",
                        tint = if (isFavorite) Color(0xFFE11D48) else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onShare) {
                    Icon(Icons.Filled.Share, "Compartir", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(top = 8.dp, bottom = 4.dp),
    )
}

private val weekLetters = listOf("L", "M", "X", "J", "V", "S", "D")

/** Seven circles, Monday to Sunday, filled for visited days. */
@Composable
fun WeekStrip(days: List<Boolean>, todayIndex: Int, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        days.forEachIndexed { i, visited ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                visited -> MaterialTheme.colorScheme.secondary
                                i == todayIndex -> MaterialTheme.colorScheme.secondaryContainer
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (visited) "🔥" else weekLetters[i],
                        color = if (visited) MaterialTheme.colorScheme.onSecondary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = if (visited) 16.sp else 13.sp,
                        fontWeight = if (i == todayIndex) FontWeight.Bold else FontWeight.Normal,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(weekLetters[i], style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AchievementUnlockedDialog(ids: List<String>, onDismiss: () -> Unit) {
    val items = ids.mapNotNull(Achievements::byId)
    if (items.isEmpty()) return
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text("¡Genial!") } },
        title = {
            Text(
                if (items.size == 1) "¡Nuevo logro desbloqueado!" else "¡${items.size} logros desbloqueados!",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        text = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                items.forEach { a ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(120.dp),
                    ) {
                        Text(a.emoji, fontSize = 44.sp)
                        Text(a.title, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
                        Text(
                            a.description,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        },
    )
}

fun shareItem(context: Context, item: StudyItem) {
    val term = item.signs.firstOrNull { it.key != null }?.searchTerm ?: item.headword
    val text = buildString {
        when (item) {
            is Word -> append("🤟 Hoy he aprendido a signar «${item.headword}» en lengua de signos española.\n")
            is Expression -> append("🤟 Hoy he aprendido a decir «${item.headword}» en lengua de signos española:\n${item.gloss}\n")
        }
        append("\nMira el signo: ${DILSE_URL}?buscar=${URLEncoder.encode(term, "UTF-8")}\n\nAprendido con Signos Daily")
    }
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(Intent.createChooser(intent, "Compartir"))
}

@Composable
fun UpdateDialog(
    info: UpdateInfo,
    progress: Float?,
    onUpdate: () -> Unit,
    onDismiss: () -> Unit,
) {
    val downloading = progress != null
    AlertDialog(
        onDismissRequest = { if (!downloading) onDismiss() },
        title = { Text("¡Nueva versión disponible!") },
        text = {
            Column {
                Text("Signos Daily ${info.versionName} ya está lista.")
                if (info.notes.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Novedades: ${info.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (progress != null) {
                    Spacer(Modifier.height(16.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text("Descargando… ${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                } else {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Tu progreso se mantiene. La primera vez, Android te pedirá permitir que Signos Daily instale apps.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onUpdate, enabled = !downloading) { Text("Actualizar") }
        },
        dismissButton = {
            if (!downloading) TextButton(onClick = onDismiss) { Text("Más tarde") }
        },
    )
}
