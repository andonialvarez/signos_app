package com.andoni.signosdaily

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andoni.signosdaily.ads.Ads
import com.andoni.signosdaily.data.ThemeMode
import com.andoni.signosdaily.data.UserRepository
import com.andoni.signosdaily.ui.AppViewModel
import com.andoni.signosdaily.ui.MainScaffold
import com.andoni.signosdaily.ui.screens.AchievementUnlockedDialog
import com.andoni.signosdaily.ui.screens.OnboardingScreen
import com.andoni.signosdaily.ui.screens.UpdateDialog
import com.andoni.signosdaily.ui.theme.SignosTheme
import kotlinx.coroutines.runBlocking

class MainActivity : ComponentActivity() {

    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val demoTab = if (BuildConfig.DEBUG) intent.getStringExtra("demo_tab") else null
        if (demoTab != null) {
            DemoMode.active = true
            runBlocking { UserRepository.get(this@MainActivity).seedDemo("Laura") }
        } else {
            Ads.gatherConsentAndStart(this)
        }
        setContent {
            val user by vm.user.collectAsStateWithLifecycle()
            val newAchievements by vm.newAchievements.collectAsStateWithLifecycle()
            val update by vm.update.collectAsStateWithLifecycle()
            val updateProgress by vm.updateProgress.collectAsStateWithLifecycle()
            SignosTheme(user?.theme ?: ThemeMode.SYSTEM) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    val u = user
                    when {
                        u == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("🤟", fontSize = 64.sp)
                        }
                        !u.onboarded -> OnboardingScreen { name, mode, mirror, hour, minute, notifications ->
                            vm.completeOnboarding(name, mode, mirror, hour, minute, notifications)
                        }
                        else -> MainScaffold(vm, u, startTab = demoTab ?: "today")
                    }
                    if (!DemoMode.active) update?.let { info ->
                        UpdateDialog(
                            info = info,
                            progress = updateProgress,
                            onUpdate = { vm.startUpdate() },
                            onDismiss = { vm.dismissUpdate() },
                        )
                    }
                    if (!DemoMode.active && update == null && newAchievements.isNotEmpty()) {
                        AchievementUnlockedDialog(newAchievements) { vm.dismissAchievements() }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        vm.onAppOpened()
    }
}
