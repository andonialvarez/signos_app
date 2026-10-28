package com.andoni.signosdaily.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andoni.signosdaily.data.ContentMode
import com.andoni.signosdaily.data.Expression
import com.andoni.signosdaily.data.StudyItem
import com.andoni.signosdaily.data.Word
import com.andoni.signosdaily.data.UserData
import com.andoni.signosdaily.domain.Motivation
import com.andoni.signosdaily.domain.StatsCalculator
import com.andoni.signosdaily.notify.Notifications
import com.andoni.signosdaily.ui.AppViewModel
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val spanish = Locale.forLanguageTag("es-ES")

@Composable
fun TodayScreen(vm: AppViewModel, user: UserData, onStartQuiz: () -> Unit) {
    val context = LocalContext.current
    val today = LocalDate.now()
    val stats = remember(user.visits) { StatsCalculator.compute(user.visits, today) }
    val items = vm.todayItems(user, today)
    val dayIndex = user.dayIndex(today)

    var permissionAsked by remember { mutableStateOf(false) }
    var canNotify by remember { mutableStateOf(Notifications.canPost(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        canNotify = it
        permissionAsked = true
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Greeting + streak
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    today.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", spanish))
                        .replaceFirstChar { it.titlecase(spanish) },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    Motivation.greeting(user.name, LocalTime.now().hour, dayIndex),
                    style = MaterialTheme.typography.headlineMedium,
                )
            }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
            ) {
                Column(
                    Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("🔥 ${stats.currentStreak}", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer)
                    Text(if (stats.currentStreak == 1) "día" else "días",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }

        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
            Column(Modifier.padding(16.dp)) {
                Row {
                    Text("Esta semana", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    Text("${stats.daysThisWeek}/7 días", style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary)
                }
                Spacer(Modifier.height(10.dp))
                WeekStrip(stats.weekDays, today.dayOfWeek.value - 1)
            }
        }

        if (user.notificationsEnabled && !canNotify && Build.VERSION.SDK_INT >= 33) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.NotificationsOff, null, tint = MaterialTheme.colorScheme.onErrorContainer)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Los recordatorios están bloqueados",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer)
                        Text(
                            if (permissionAsked) "Actívalos en los ajustes de Android para no perder tu racha."
                            else "Permite las notificaciones para que te recuerde tu signo diario.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                    if (!permissionAsked) {
                        TextButton(onClick = { permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }) {
                            Text("Permitir")
                        }
                    }
                }
            }
        }

        items.forEach { item ->
            SectionTitle(if (item is Word) "Tu signo de hoy" else "Tu expresión de hoy")
            StudyCard(
                item = item,
                isFavorite = item.id in user.favorites,
                isLearned = item.id in user.learned,
                speed = user.videoSpeed,
                mirror = user.mirror,
                onSpeedChange = vm::setVideoSpeed,
                onMirrorChange = vm::setMirror,
                onToggleFavorite = { vm.setFavorite(item.id, item.id !in user.favorites) },
                onToggleLearned = { vm.setLearned(item.id, item.id !in user.learned) },
                onShare = { shareItem(context, item) },
                usedIn = usedInSeen(vm, user, item),
            )
        }

        if (user.contentMode != ContentMode.BOTH) {
            OutlinedButton(onClick = { vm.swapToday() }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.SwapHoriz, null)
                Spacer(Modifier.width(8.dp))
                Text(if (items.first() is Expression) "Cambiar al signo de hoy" else "Cambiar a la expresión de hoy")
            }
        }

        Button(onClick = onStartQuiz, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Text("Repasar lo aprendido")
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.ArrowForward, null)
        }

        Text(
            "“${Motivation.quoteFor(dayIndex)}”",
            style = MaterialTheme.typography.bodyLarge,
            fontStyle = FontStyle.Italic,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Text(
            "Día ${dayIndex + 1} de tu aventura · mañana, un signo nuevo",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.fillMaxWidth(),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
    }
}

/** For a word, the expressions already unlocked that use its sign. */
fun usedInSeen(vm: AppViewModel, user: UserData, item: StudyItem): List<Expression> {
    val key = (item as? Word)?.sign?.key ?: return emptyList()
    return vm.content.expressionsUsing(key).filter { it.id in user.seen }
}
