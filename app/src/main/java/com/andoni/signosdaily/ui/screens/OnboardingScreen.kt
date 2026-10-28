package com.andoni.signosdaily.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andoni.signosdaily.data.ContentMode
import com.andoni.signosdaily.notify.Notifications

private const val LAST_STEP = 4

@Composable
fun OnboardingScreen(
    onFinish: (
        name: String,
        mode: ContentMode,
        mirror: Boolean,
        hour: Int,
        minute: Int,
        notifications: Boolean,
    ) -> Unit,
) {
    val context = LocalContext.current
    var step by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var mode by rememberSaveable { mutableStateOf(ContentMode.BOTH) }
    var mirror by rememberSaveable { mutableStateOf(false) }
    var hour by rememberSaveable { mutableIntStateOf(19) }
    var minute by rememberSaveable { mutableIntStateOf(0) }
    var notifications by rememberSaveable { mutableStateOf(true) }
    var pickingTime by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        onFinish(name, mode, mirror, hour, minute, granted)
    }

    fun finish() {
        if (notifications && Build.VERSION.SDK_INT >= 33 && !Notifications.canPost(context)) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            onFinish(name, mode, mirror, hour, minute, notifications)
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .imePadding()
            .padding(24.dp),
    ) {
        // Progress dots
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            repeat(LAST_STEP + 1) { i ->
                Box(
                    Modifier
                        .height(6.dp)
                        .width(if (i == step) 28.dp else 12.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (i <= step) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant,
                        ),
                )
            }
        }

        AnimatedContent(targetState = step, label = "onboarding", modifier = Modifier.weight(1f)) { s ->
            Column(
                Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(top = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                when (s) {
                    0 -> {
                        Box(
                            Modifier.size(96.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center,
                        ) { Text("🤟", fontSize = 48.sp) }
                        Text("Bienvenido a Signos Daily", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "Cada día aprenderás un signo nuevo y una expresión de la lengua de signos española " +
                                "(LSE), con vídeos del diccionario oficial de la Fundación CNSE. Poco a poco podrás " +
                                "presentarte, saludar, preguntar y contar cosas de tu día a día.",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Text(
                            "🎬 Vídeos en bucle, a cámara lenta y en espejo\n🔥 Rachas y logros por entrar cada día\n" +
                                "📊 Estadísticas semanales y mensuales\n🔔 Recordatorios con frases motivadoras\n" +
                                "🎯 Repasos para que no se te olvide",
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 28.sp,
                        )
                    }
                    1 -> {
                        Text("¿Cómo te llamas?", style = MaterialTheme.typography.headlineMedium)
                        Text("Así podré saludarte cada día.", style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it.take(30) },
                            label = { Text("Tu nombre") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next,
                            ),
                            keyboardActions = KeyboardActions(onNext = { if (name.isNotBlank()) step = 2 }),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("¿Con qué mano escribes?", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Los vídeos están signados con la mano derecha. Si eres zurdo, te los enseño en espejo " +
                                "para que los copies con tu mano dominante.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        HandPicker(mirror) { mirror = it }
                    }
                    2 -> {
                        Text(
                            "¿Qué quieres aprender cada día, ${name.trim()}?",
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Text("Lo puedes cambiar luego en Ajustes.", style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        ContentModePicker(mode) { mode = it }
                    }
                    3 -> {
                        Text("Cómo mirar un signo", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "Cada signo se forma con cinco piezas. Fíjate en ellas en cada vídeo y repítelo " +
                                "frente al espejo:",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "✋ Forma de la mano: qué dedos están abiertos o cerrados.\n" +
                                "📍 Lugar: dónde se hace (cara, pecho, delante del cuerpo…).\n" +
                                "↗️ Movimiento: hacia dónde va la mano y cuántas veces.\n" +
                                "🔄 Orientación: hacia dónde mira la palma.\n" +
                                "🙂 Expresión de la cara: en LSE también es gramática (preguntas, negaciones, intensidad).",
                            style = MaterialTheme.typography.bodyLarge,
                            lineHeight = 28.sp,
                        )
                        Text(
                            "Truco: pon los vídeos a cámara lenta la primera vez y en velocidad normal después.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    else -> {
                        Text("Tu recordatorio diario", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            "Te mandaré una notificación con una frase motivadora para que no pierdas la racha. " +
                                "¿A qué hora te viene mejor?",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Activar recordatorios", modifier = Modifier.weight(1f),
                                style = MaterialTheme.typography.titleMedium)
                            Switch(checked = notifications, onCheckedChange = { notifications = it })
                        }
                        if (notifications) {
                            TextButton(onClick = { pickingTime = true }) {
                                Text(formatTime(hour, minute), fontSize = 48.sp)
                            }
                            Text("Toca la hora para cambiarla", style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            if (step > 0) {
                TextButton(onClick = { step-- }) { Text("Atrás") }
            }
            Spacer(Modifier.weight(1f))
            Button(
                onClick = { if (step < LAST_STEP) step++ else finish() },
                enabled = step != 1 || name.isNotBlank(),
                modifier = Modifier.height(52.dp),
            ) {
                Text(
                    when (step) {
                        0 -> "Empezar"
                        LAST_STEP -> "¡Vamos allá!"
                        else -> "Siguiente"
                    },
                    textAlign = TextAlign.Center,
                )
            }
        }
    }

    if (pickingTime) {
        TimePickerDialog(
            hour = hour,
            minute = minute,
            onDismiss = { pickingTime = false },
            onConfirm = { h, m ->
                hour = h
                minute = m
                pickingTime = false
            },
        )
    }
}
