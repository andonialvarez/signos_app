package com.andoni.signosdaily.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.andoni.signosdaily.data.UserData
import com.andoni.signosdaily.ui.AppViewModel
import com.andoni.signosdaily.ui.QuizKind
import com.andoni.signosdaily.ui.QuizQuestion

private enum class QuizPhase { INTRO, PLAYING, DONE }

@Composable
fun QuizScreen(vm: AppViewModel, user: UserData) {
    var phase by remember { mutableStateOf(QuizPhase.INTRO) }
    var questions by remember { mutableStateOf<List<QuizQuestion>>(emptyList()) }
    var index by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<Int?>(null) }
    var score by remember { mutableIntStateOf(0) }

    fun start() {
        questions = vm.buildQuiz(user)
        index = 0
        selected = null
        score = 0
        phase = QuizPhase.PLAYING
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Repaso", style = MaterialTheme.typography.headlineMedium)

        AnimatedContent(targetState = phase, label = "quiz") { p ->
            when (p) {
                QuizPhase.INTRO -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    val pool = user.seen.size.coerceAtLeast(1)
                    Card(
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    ) {
                        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🎯", fontSize = 56.sp)
                            Text(
                                "Pon a prueba lo que llevas aprendido",
                                style = MaterialTheme.typography.titleLarge,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Preguntas rápidas con los $pool signos y expresiones que has visto: adivina qué " +
                                    "significa un signo, encuentra su foto u ordena una frase como en LSE.",
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                    Text(
                        "Repasos completados: ${user.quizzesDone} · Aciertos totales: ${user.quizCorrectTotal}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(onClick = { start() }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        Text("Empezar repaso")
                    }
                }

                QuizPhase.PLAYING -> {
                    val q = questions.getOrNull(index)
                    if (q == null) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("Aún no hay signos para repasar, o no tienes conexión para cargar sus vídeos. ¡Vuelve más tarde!")
                            OutlinedButton(onClick = { phase = QuizPhase.INTRO }, modifier = Modifier.fillMaxWidth()) {
                                Text("Volver")
                            }
                        }
                    } else {
                        QuestionView(
                            vm = vm,
                            user = user,
                            q = q,
                            number = index + 1,
                            total = questions.size,
                            selected = selected,
                            onSelect = { choice ->
                                if (selected == null) {
                                    selected = choice
                                    if (choice == q.correct) score++
                                }
                            },
                            onNext = {
                                if (index + 1 < questions.size) {
                                    index++
                                    selected = null
                                } else {
                                    vm.finishQuiz(score, questions.size)
                                    phase = QuizPhase.DONE
                                }
                            },
                        )
                    }
                }

                QuizPhase.DONE -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val total = questions.size
                    val ratio = if (total == 0) 0f else score.toFloat() / total
                    Text(
                        when {
                            ratio == 1f -> "🏆"
                            ratio >= 0.7f -> "🎉"
                            ratio >= 0.4f -> "💪"
                            else -> "📖"
                        },
                        fontSize = 72.sp,
                    )
                    Text("$score de $total", style = MaterialTheme.typography.displaySmall)
                    Text(
                        when {
                            ratio == 1f -> "¡Perfecto, ${user.name.ifBlank { "crack" }}! Ni un fallo."
                            ratio >= 0.7f -> "¡Muy bien! Vas por buen camino."
                            ratio >= 0.4f -> "No está mal. Un repaso más y lo bordas."
                            else -> "Cada error es un signo que no volverás a olvidar."
                        },
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { start() }, modifier = Modifier.fillMaxWidth()) { Text("Otro repaso") }
                    OutlinedButton(onClick = { phase = QuizPhase.INTRO }, modifier = Modifier.fillMaxWidth()) {
                        Text("Terminar")
                    }
                }
            }
        }
    }
}

@Composable
private fun QuestionView(
    vm: AppViewModel,
    user: UserData,
    q: QuizQuestion,
    number: Int,
    total: Int,
    selected: Int?,
    onSelect: (Int) -> Unit,
    onNext: () -> Unit,
) {
    val answered = selected != null
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Pregunta $number de $total", style = MaterialTheme.typography.labelLarge)
        LinearProgressIndicator(
            progress = { number.toFloat() / total },
            modifier = Modifier.fillMaxWidth(),
        )
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(q.prompt, style = MaterialTheme.typography.titleLarge)
                Text(q.hint, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (q.kind == QuizKind.SIGN_TO_MEANING) {
                    SignPlayer(
                        signs = q.item.signs,
                        speed = user.videoSpeed,
                        mirror = user.mirror,
                        onSpeedChange = vm::setVideoSpeed,
                        onMirrorChange = vm::setMirror,
                        // The gloss would give the answer away until it is answered.
                        showGloss = answered,
                    )
                }
            }
        }

        if (q.kind == QuizKind.MEANING_TO_SIGN) {
            PhotoOptions(q, selected, user.mirror, onSelect)
        } else {
            q.options.forEachIndexed { i, option ->
                val isCorrect = i == q.correct
                val container = when {
                    !answered -> MaterialTheme.colorScheme.surface
                    isCorrect -> MaterialTheme.colorScheme.primaryContainer
                    i == selected -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.surface
                }
                Surface(
                    onClick = { onSelect(i) },
                    enabled = !answered,
                    shape = RoundedCornerShape(16.dp),
                    color = container,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(option, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        if (answered && isCorrect) Text("✅")
                        if (answered && !isCorrect && i == selected) Text("❌")
                    }
                }
            }
        }

        if (selected != null) {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        if (selected == q.correct) "¡Correcto!"
                        else "Casi… la respuesta era «${if (q.kind == QuizKind.MEANING_TO_SIGN) "la foto marcada" else q.options[q.correct]}»",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text("«${q.item.headword}» → ${q.item.gloss}", color = MaterialTheme.colorScheme.onSecondaryContainer)
                    if (q.item.tip.isNotBlank()) {
                        Text(q.item.tip, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer)
                    }
                }
            }
            Button(onClick = onNext, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Text(if (number < total) "Siguiente" else "Ver resultado")
            }
        }
    }
}

/** Four sign photos in a 2×2 grid; the right one gets a green frame once answered. */
@Composable
private fun PhotoOptions(q: QuizQuestion, selected: Int?, mirror: Boolean, onSelect: (Int) -> Unit) {
    val answered = selected != null
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        q.optionKeys.chunked(2).forEachIndexed { row, keys ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                keys.forEachIndexed { col, key ->
                    val i = row * 2 + col
                    val frame = when {
                        !answered -> Color.Transparent
                        i == q.correct -> MaterialTheme.colorScheme.primary
                        i == selected -> MaterialTheme.colorScheme.error
                        else -> Color.Transparent
                    }
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .border(4.dp, frame, RoundedCornerShape(16.dp))
                            .clickable(enabled = !answered) { onSelect(i) },
                    ) {
                        SignPhoto(key, mirror, Modifier.fillMaxWidth())
                        if (answered && (i == q.correct || i == selected)) {
                            Text(
                                q.options[i],
                                color = Color.White,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(8.dp)
                                    .background(Color.Black.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
