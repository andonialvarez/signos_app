package com.andoni.signosdaily.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.andoni.signosdaily.data.ContentMode
import com.andoni.signosdaily.data.ContentRepository
import com.andoni.signosdaily.data.UserRepository
import com.andoni.signosdaily.domain.Motivation
import com.andoni.signosdaily.domain.StatsCalculator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                remind(context.applicationContext)
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        /** Posts today's reminder (unless already studied) and schedules tomorrow's. */
        suspend fun remind(context: Context, force: Boolean = false) {
            val users = UserRepository.get(context)
            val user = users.current()
            if (!user.notificationsEnabled && !force) return

            val today = LocalDate.now()
            if (force || today !in user.visits) {
                val content = ContentRepository.get(context)
                val day = user.dayIndex(today)
                val word = when (user.contentMode) {
                    ContentMode.EXPRESSION -> content.expressionForDay(day).headword
                    else -> content.wordForDay(day).headword
                }
                val streak = StatsCalculator.compute(user.visits, today).currentStreak
                val reminder = Motivation.reminder(user.name, streak, word, user.lastPhraseIndex)
                users.setLastPhraseIndex(reminder.index)
                Notifications.show(context, reminder.title, reminder.body)
            }
            if (!force) ReminderScheduler.schedule(context, user.reminderHour, user.reminderMinute)
        }
    }
}
