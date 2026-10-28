package com.andoni.signosdaily.ads

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.andoni.signosdaily.BuildConfig
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/** AdMob setup: asks for EU consent (Play build) and starts the Mobile Ads SDK once allowed and enabled. */
object Ads {
    private val started = AtomicBoolean(false)

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)
    /** True when the user must be able to change their ad consent from Settings. */
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    /** Call on every launch, as Google recommends; shows the consent form only when needed. */
    fun gatherConsentAndStart(activity: Activity) {
        // Play build: no ads until the Fundación CNSE allows them alongside its videos.
        if (!BuildConfig.ADS_ENABLED) return
        if (!BuildConfig.ADS_REQUIRE_CONSENT) {
            start(activity)
            return
        }
        val info = UserMessagingPlatform.getConsentInformation(activity)
        info.requestConsentInfoUpdate(
            activity,
            ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { _ ->
                    _privacyOptionsRequired.value = info.privacyOptionsRequirementStatus ==
                        ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
                    if (info.canRequestAds()) start(activity)
                }
            },
            { _ -> if (info.canRequestAds()) start(activity) },
        )
        // Returning users who already answered can see ads straight away.
        if (info.canRequestAds()) start(activity)
    }

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { _ -> }
    }

    private fun start(context: Context) {
        if (!started.compareAndSet(false, true)) return
        val app = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(app) { _ready.value = true }
        }
    }
}

/** Anchored adaptive banner; renders nothing until ads are allowed and initialised. */
@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    val ready by Ads.ready.collectAsStateWithLifecycle()
    if (!ready) return
    BoxWithConstraints(modifier.fillMaxWidth()) {
        val widthDp = maxWidth.value.toInt()
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { context ->
                AdView(context).apply {
                    adUnitId = BuildConfig.ADMOB_BANNER_ID
                    setAdSize(AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, widthDp))
                    loadAd(AdRequest.Builder().build())
                }
            },
            onRelease = { it.destroy() },
        )
    }
}
