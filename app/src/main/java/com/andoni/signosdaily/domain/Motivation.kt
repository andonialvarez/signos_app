package com.andoni.signosdaily.domain

import kotlin.random.Random

/**
 * Reminder texts. Placeholders: {name}, {streak}, {word}.
 * Phrases with {streak} are only used when the streak is at least 2.
 */
object Motivation {

    private val titles = listOf(
        "¡Tu signo del día te espera!",
        "Un minuto de lengua de signos 🙌",
        "Hora de mover las manos",
        "¿Listo para el de hoy?",
        "Tu racha te necesita 🔥",
        "Manos a la obra ✋",
        "Nuevo día, nuevo signo",
        "Pssst… {name}",
        "Tus manos quieren gimnasio",
        "Signo exprés ☕",
    )

    private val bodies = listOf(
        "{name}, hoy toca «{word}». ¿Te imaginas cómo se signa?",
        "Un signo al día y la lengua de signos deja de parecer difícil. Hoy: «{word}».",
        "Llevas {streak} días seguidos, {name}. No dejes que la racha se apague 🔥",
        "{streak} días de racha. Tu yo del futuro te lo agradecerá.",
        "Dos minutos ahora valen más que dos horas el mes que viene.",
        "Los grandes cambios empiezan con gestos pequeños. Hoy, «{word}».",
        "{name}, la lengua de signos no se aprende sola… pero casi: solo tienes que abrir la app.",
        "Hoy aprenderás un signo que mañana podrás usar. Palabra de app.",
        "Cada día cuenta. Literalmente: aquí llevamos la cuenta 😉",
        "¡Sigue así, {name}! Tu racha de {streak} días está en juego.",
        "Cada signo que aprendes es una conversación más que puedes tener.",
        "Tu racha es como una planta 🌱: un poquito de agua cada día.",
        "Un signo hoy, una conversación mañana.",
        "Prometido: «{word}» es más fácil de lo que parece.",
        "{name}, tus manos han pedido algo nuevo que aprender 🙌",
        "La constancia vence al talento. Y tú tienes las dos.",
        "Racha de {streak} días. ¿A por el siguiente nivel?",
        "Un minuto. Un signo. Un paso más cerca de entenderte con más gente.",
        "Spoiler: hoy vas a aprender a signar «{word}».",
        "¡No me dejes en visto, {name}! Tu signo de hoy te espera 👀",
        "Aprender es como ir al gimnasio: lo difícil es entrar. Entra.",
        "{streak} días seguidos… ¡esto ya es un hábito de verdad!",
        "¿Un café y un signo? La combinación perfecta ☕",
        "Lo bueno de aprender poco cada día es que nunca es mucho.",
        "Hoy es un buen día para practicar frente al espejo 🪞",
        "Pequeños pasos, grandes resultados. Paso de hoy: «{word}».",
        "Ni excusas ni pereza: hoy toca lengua de signos, {name}.",
        "Si no lo haces hoy, ¿quién va a cuidar de tu racha de {streak} días?",
        "Abre, mira, repite. Así de fácil.",
        "En lengua de signos la cara también habla. Hoy lo verás con «{word}».",
        "Cinco segundos para abrir la app, toda una vida para usar lo aprendido.",
        "«{word}» quiere conocerte. ¿Le das una oportunidad?",
        "Tu racha no se va a alargar sola, {name} 😏",
        "Ayer dijiste «mañana». Hoy es el día.",
        "La práctica hace al maestro. Tu práctica de hoy está lista.",
        "Repite el signo de hoy tres veces y ya no se te olvida.",
        "Cuanto más signas, más natural te sale. Hoy: «{word}».",
        "La lengua de signos española está reconocida por ley desde 2007. Hoy la haces un poco más tuya.",
    )

    private val streakDangerBodies = listOf(
        "⚠️ {name}, tu racha de {streak} días termina hoy si no entras.",
        "¡Alerta de racha! {streak} días en juego. Solo necesitas un minuto.",
        "No dejes que {streak} días de esfuerzo se esfumen. Hoy: «{word}».",
    )

    private val greetingsMorning = listOf("¡Buenos días, {name}!", "¡Hola, {name}!", "¡Arriba, {name}!")
    private val greetingsAfternoon = listOf("¡Buenas tardes, {name}!", "¡Hola, {name}!", "¿Qué tal, {name}?")
    private val greetingsNight = listOf("¡Buenas noches, {name}!", "¡Hola, {name}!", "¿Qué tal, {name}?")

    /** Short quotes shown on the home screen. */
    val dailyQuotes = listOf(
        "Poco a poco se va lejos.",
        "Lo que haces cada día importa más que lo que haces de vez en cuando.",
        "Una lengua nueva es una ventana nueva al mundo.",
        "No tienes que ser perfecto, solo constante.",
        "Hoy un signo, mañana una conversación.",
        "Todo experto empezó siendo principiante.",
        "El mejor momento para empezar fue ayer. El segundo mejor, hoy.",
        "Equivocarse también es aprender.",
        "En lengua de signos, la cara también habla.",
        "Tu esfuerzo de hoy es tu fluidez de mañana.",
        "Las manos dicen lo que las palabras no alcanzan.",
        "Aprender a signar es aprender a mirar.",
    )

    data class Reminder(val title: String, val body: String, val index: Int)

    fun reminder(name: String, streak: Int, word: String, lastIndex: Int, random: Random = Random.Default): Reminder {
        val displayName = name.ifBlank { "crack" }
        val candidates = if (streak >= 3 && random.nextInt(4) == 0) {
            streakDangerBodies.indices.map { it + 1000 }
        } else {
            bodies.indices.filter { streak >= 2 || "{streak}" !in bodies[it] }
        }.filter { it != lastIndex }
        val idx = candidates.random(random)
        val raw = if (idx >= 1000) streakDangerBodies[idx - 1000] else bodies[idx]
        fun fill(s: String) = s.replace("{name}", displayName)
            .replace("{streak}", streak.toString())
            .replace("{word}", word)
        return Reminder(title = fill(titles.random(random)), body = fill(raw), index = idx)
    }

    fun greeting(name: String, hour: Int, dayIndex: Long): String {
        val list = when (hour) {
            in 5..13 -> greetingsMorning
            in 14..20 -> greetingsAfternoon
            else -> greetingsNight
        }
        return list[Math.floorMod(dayIndex, list.size.toLong()).toInt()].replace("{name}", name.ifBlank { "crack" })
    }

    fun quoteFor(dayIndex: Long): String = dailyQuotes[Math.floorMod(dayIndex, dailyQuotes.size.toLong()).toInt()]
}
