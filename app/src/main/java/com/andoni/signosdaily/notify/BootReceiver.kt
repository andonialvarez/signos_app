package com.andoni.signosdaily.notify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.andoni.signosdaily.data.UserRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Alarms are cleared on reboot, update or clock change, so re-arm the daily reminder. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val user = UserRepository.get(context).current()
                if (user.onboarded && user.notificationsEnabled) {
                    ReminderScheduler.schedule(context, user.reminderHour, user.reminderMinute)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
