package com.andoni.signosdaily.domain

import com.andoni.signosdaily.data.UserData
import java.time.LocalTime

data class Achievement(
    val id: String,
    val emoji: String,
    val title: String,
    val description: String,
    /** Progress towards the goal as (current, target), for the progress bar. */
    val progress: (UserData, Stats) -> Pair<Int, Int>,
)

object Achievements {

    private fun streak(target: Int): (UserData, Stats) -> Pair<Int, Int> =
        { _, s -> minOf(s.bestStreak, target) to target }

    private fun learned(target: Int): (UserData, Stats) -> Pair<Int, Int> =
        { u, _ -> minOf(u.learned.size, target) to target }

    private fun flag(done: (UserData, Stats) -> Boolean): (UserData, Stats) -> Pair<Int, Int> =
        { u, s -> (if (done(u, s)) 1 else 0) to 1 }

    val all: List<Achievement> = listOf(
        Achievement("first_day", "🌱", "Primer paso", "Abre la app por primera vez.", streak(1)),
        Achievement("streak_3", "🔥", "Calentando motores", "3 días seguidos aprendiendo.", streak(3)),
        Achievement("streak_7", "📅", "Semana redonda", "7 días seguidos sin fallar.", streak(7)),
        Achievement("streak_14", "⚡", "Imparable", "14 días seguidos.", streak(14)),
        Achievement("streak_30", "🏅", "Mes de hierro", "30 días seguidos.", streak(30)),
        Achievement("streak_60", "💪", "Hábito forjado", "60 días seguidos.", streak(60)),
        Achievement("streak_100", "💯", "Centurión", "100 días seguidos.", streak(100)),
        Achievement("streak_200", "🚀", "Fuera de órbita", "200 días seguidos.", streak(200)),
        Achievement("streak_365", "👑", "Leyenda de los signos", "Un año entero, día a día.", streak(365)),
        Achievement("week_full", "🗓️", "Semana perfecta", "Entra los 7 días de una semana (lunes a domingo).",
            flag { _, s -> s.daysThisWeek == 7 }),
        Achievement("learned_1", "✅", "Mi primer signo", "Marca tu primer signo o expresión como aprendido.", learned(1)),
        Achievement("learned_10", "🙌", "Manos a la obra", "10 signos o expresiones aprendidos.", learned(10)),
        Achievement("learned_50", "🧠", "Biblioteca andante", "50 signos o expresiones aprendidos.", learned(50)),
        Achievement("learned_100", "🎓", "Manos expertas", "100 signos o expresiones aprendidos.", learned(100)),
        Achievement("quiz_1", "🎯", "A prueba", "Completa tu primer repaso.",
            { u, _ -> minOf(u.quizzesDone, 1) to 1 }),
        Achievement("quiz_10", "🔁", "Repaso constante", "Completa 10 repasos.",
            { u, _ -> minOf(u.quizzesDone, 10) to 10 }),
        Achievement("quiz_perfect", "🌟", "Matrícula de honor", "Acierta todas las preguntas de un repaso.",
            { u, _ -> minOf(u.perfectQuizzes, 1) to 1 }),
        Achievement("fav_5", "❤️", "Mis favoritos", "Guarda 5 favoritos.",
            { u, _ -> minOf(u.favorites.size, 5) to 5 }),
        Achievement("comeback", "🦸", "Vuelta a la carga", "Vuelve tras 3 días o más sin entrar. ¡Nunca es tarde!",
            flag { _, s -> s.isComeback }),
        Achievement("early_bird", "🐦", "Madrugador", "Estudia antes de las 8:00.", flag { _, _ -> false }),
        Achievement("night_owl", "🦉", "Búho nocturno", "Estudia después de las 23:00.", flag { _, _ -> false }),
    )

    fun byId(id: String) = all.firstOrNull { it.id == id }

    /** Ids of achievements earned now but not yet stored. */
    fun newlyEarned(user: UserData, stats: Stats, now: LocalTime = LocalTime.now()): List<String> =
        all.filter { it.id !in user.achievements }
            .filter { a ->
                when (a.id) {
                    "early_bird" -> now.hour < 8
                    "night_owl" -> now.hour >= 23
                    else -> a.progress(user, stats).let { (cur, target) -> cur >= target }
                }
            }
            .map { it.id }
}
