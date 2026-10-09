// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.samplejoe

import android.app.Application
import android.util.Log
import dev.mobilejoe.api.Joe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * [Application] subclass for the sample app, responsible for one-time, process-level setup.
 *
 * Its only job is configuring the MobileJoe SDK ([Joe]) in [onCreate] as soon as the process
 * starts, before [MainActivity] or any Compose screen exists. Every screen reached through
 * [SampleNavHost] — the feature plan list, FAQ list, and alerts banner — is backed by
 * `mobilejoe-ui-compose` components that assume the SDK is already configured with an identity
 * and API key, so this setup has to happen here rather than lazily inside any one screen.
 */
class SampleApp : Application() {

    /**
     * Configures the MobileJoe SDK for the whole process.
     *
     * Launches a fire-and-forget coroutine on `Dispatchers.IO` that calls [Joe.configure] with
     * a fixed sample API key, no external user id, and debug mode mirroring `BuildConfig.DEBUG`
     * (so debug builds bypass server-side throttling/caching, matching local development
     * expectations). The coroutine is not tied to any activity or screen lifecycle — it simply
     * runs once for the process — so a configuration failure is caught and logged rather than
     * being allowed to crash the app or block startup.
     */
    override fun onCreate() {
        super.onCreate()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Joe.configure(
                    applicationContext = this@SampleApp,
                    apiKey = "3006e2958a3382f72f084b89ee903d04",
                    appUserId = null,
                    isDebugMode = BuildConfig.DEBUG,
                )
            } catch (e: Exception) {
                Log.e("SampleApp", "Configure the Mobile Joe SDK failed!", e)
            }
        }
    }
}