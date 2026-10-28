package com.andoni.signosdaily.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

data class Stats(
    val currentStreak: Int,
    val bestStreak: Int,
    val totalDays: Int,
    /** Monday..Sunday of the current week: visited or not. */
    val weekDays: List<Boolean>,
    val daysThisWeek: Int,
    val daysThisMonth: Int,
    val daysElapsedInMonth: Int,
    /** True when the user came back after missing 3+ days. */
    val isComeback: Boolean,
)

object StatsCalculator {

    fun compute(visits: Set<LocalDate>, today: LocalDate = LocalDate.now()): Stats {
        val sorted = visits.sorted()

        // Current streak: consecutive days ending today (or yesterday, if today not yet visited).
        var cursor = if (today in visits) today else today.minusDays(1)
        var current = 0
        while (cursor in visits) {
            current++
            cursor = cursor.minusDays(1)
        }

        var best = 0
        var run = 0
        var prev: LocalDate? = null
        for (d in sorted) {
            run = if (prev != null && prev.plusDays(1) == d) run + 1 else 1
            best = maxOf(best, run)
            prev = d
        }

        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val week = List(7) { monday.plusDays(it.toLong()) in visits }
        val monthDays = visits.count { it.year == today.year && it.month == today.month }

        val beforeToday = sorted.lastOrNull { it < today }
        val comeback = today in visits && beforeToday != null && beforeToday.plusDays(3) < today

        return Stats(
            currentStreak = current,
            bestStreak = maxOf(best, current),
            totalDays = visits.size,
            weekDays = week,
            daysThisWeek = week.count { it },
            daysThisMonth = monthDays,
            daysElapsedInMonth = today.dayOfMonth,
            isComeback = comeback,
        )
    }
}
