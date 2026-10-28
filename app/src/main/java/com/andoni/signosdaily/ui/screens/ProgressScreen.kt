package com.andoni.signosdaily.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andoni.signosdaily.data.UserData
import com.andoni.signosdaily.domain.Achievements
import com.andoni.signosdaily.domain.StatsCalculator
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val spanish = Locale.forLanguageTag("es-ES")

@Composable
fun ProgressScreen(user: UserData) {
    val today = LocalDate.now()
    val stats = remember(user.visits) { StatsCalculator.compute(user.visits, today) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("Tu progreso", style = MaterialTheme.typography.headlineMedium)

        // Streak hero
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        ) {
            Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("🔥", fontSize = 52.sp)
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "${stats.currentStreak} ${if (stats.currentStreak == 1) "día" else "días"} seguidos",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        "Mejor racha: ${stats.bestStreak} días",
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    val next = listOf(3, 7, 14, 30, 60, 100, 200, 365).firstOrNull { it > stats.currentStreak }
                    if (next != null) {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { stats.currentStreak.toFloat() / next },
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.secondary,
                        )
                        Text(
                            "Te faltan ${next - stats.currentStreak} para la racha de $next",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("${stats.daysThisWeek}/7", "días esta semana", Modifier.weight(1f))
            StatTile("${stats.daysThisMonth}/${stats.daysElapsedInMonth}", "días este mes", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile("${stats.totalDays}", "días en total", Modifier.weight(1f))
            StatTile("${user.learned.size}", "aprendidos", Modifier.weight(1f))
            StatTile("${user.quizzesDone}", "repasos", Modifier.weight(1f))
        }

        SectionTitle("Esta semana")
        WeekStrip(stats.weekDays, today.dayOfWeek.value - 1)

        SectionTitle("Calendario")
        MonthCalendar(user.visits, today)

        val unlocked = Achievements.all.count { it.id in user.achievements }
        SectionTitle("Logros · $unlocked de ${Achievements.all.size}")
        Achievements.all.forEach { a ->
            val date = user.achievements[a.id]
            val (cur, target) = a.progress(user, stats)
            AchievementRow(
                emoji = a.emoji,
                title = a.title,
                description = a.description,
                unlockedOn = date,
                progress = if (date != null) 1f else cur.toFloat() / target,
                progressLabel = if (date == null && target > 1) "$cur / $target" else null,
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun StatTile(value: String, label: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MonthCalendar(visits: Set<LocalDate>, today: LocalDate) {
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    val count = visits.count { YearMonth.from(it) == month }

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { month = month.minusMonths(1) }) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Mes anterior")
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        month.format(DateTimeFormatter.ofPattern("MMMM yyyy", spanish))
                            .replaceFirstChar { it.titlecase(spanish) },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text("$count ${if (count == 1) "día" else "días"} de estudio",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(
                    onClick = { month = month.plusMonths(1) },
                    enabled = month < YearMonth.from(today),
                ) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Mes siguiente")
                }
            }
            Row(Modifier.fillMaxWidth()) {
                java.time.DayOfWeek.values().forEach { d ->
                    Text(
                        d.getDisplayName(TextStyle.NARROW, spanish).uppercase(spanish),
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            val first = month.atDay(1)
            val offset = first.dayOfWeek.value - 1
            val days = month.lengthOfMonth()
            val cells = offset + days
            val rows = (cells + 6) / 7
            for (r in 0 until rows) {
                Row(Modifier.fillMaxWidth()) {
                    for (c in 0 until 7) {
                        val dayNum = r * 7 + c - offset + 1
                        Box(Modifier.weight(1f).aspectRatio(1f).padding(3.dp), contentAlignment = Alignment.Center) {
                            if (dayNum in 1..days) {
                                val date = month.atDay(dayNum)
                                val visited = date in visits
                                val isToday = date == today
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                visited -> MaterialTheme.colorScheme.secondary
                                                isToday -> MaterialTheme.colorScheme.secondaryContainer
                                                else -> Color.Transparent
                                            },
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "$dayNum",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                        color = when {
                                            visited -> MaterialTheme.colorScheme.onSecondary
                                            date > today -> MaterialTheme.colorScheme.outline
                                            else -> MaterialTheme.colorScheme.onSurface
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementRow(
    emoji: String,
    title: String,
    description: String,
    unlockedOn: LocalDate?,
    progress: Float,
    progressLabel: String?,
) {
    val unlocked = unlockedOn != null
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (unlocked) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 32.sp, modifier = Modifier.alpha(if (unlocked) 1f else 0.35f))
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(description, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (unlockedOn != null) {
                    Text(
                        "Conseguido el " + unlockedOn.format(DateTimeFormatter.ofPattern("d MMM yyyy", spanish)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LinearProgressIndicator(progress = { progress.coerceIn(0f, 1f) }, modifier = Modifier.weight(1f))
                        if (progressLabel != null) {
                            Spacer(Modifier.width(8.dp))
                            Text(progressLabel, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }
    }
}
