// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.samplejoe

import android.app.Application
import dev.mobilejoe.api.Joe

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
     * Calls [Joe.configure] directly with a fixed sample API key, no external user id, and debug
     * mode mirroring `BuildConfig.DEBUG` (so debug builds bypass server-side throttling/caching,
     * matching local development expectations). The call returns right away: the SDK sends the
     * identity to the backend in the background and logs a failure to Logcat with the tag `Joe`.
     */
    override fun onCreate() {
        super.onCreate()

        Joe.configure(
            applicationContext = this,
            apiKey = "3006e2958a3382f72f084b89ee903d04",
            appUserId = null,
            isDebugMode = BuildConfig.DEBUG,
        )
    }
}
