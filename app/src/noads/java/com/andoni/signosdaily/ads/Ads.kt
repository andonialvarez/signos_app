package com.andoni.signosdaily.ads

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Build without ads (adsEnabled=false): same API as the AdMob version, but the
 * AdMob SDK is not even included, so the app collects nothing for advertising.
 */
object Ads {
    private val _privacyOptionsRequired = MutableStateFlow(false)
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    @Suppress("UNUSED_PARAMETER")
    fun gatherConsentAndStart(activity: Activity) = Unit

    @Suppress("UNUSED_PARAMETER")
    fun showPrivacyOptions(activity: Activity) = Unit
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun BannerAd(modifier: Modifier = Modifier) = Unit
