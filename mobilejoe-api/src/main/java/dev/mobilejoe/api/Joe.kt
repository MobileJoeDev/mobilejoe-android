// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api

import android.content.Context
import dev.mobilejoe.api.identity.Identity
import dev.mobilejoe.api.network.JoeNetworkClient

/**
 * Top-level entry point for configuring and interacting with the MobileJoe SDK.
 *
 * A host app calls [configure] exactly once, directly in [android.app.Application.onCreate],
 * before touching any other part of the SDK — every other member of [Joe], and every SDK type
 * that talks to the network (e.g. `Alerts`, `Faqs`, `FeaturePlans`), assumes a configured
 * [networkClient] and will throw if used beforehand. Once configured, [identify] and [logout]
 * let the host app associate the SDK's identity with its own signed-in user and clear it again,
 * respectively.
 *
 * [configure], [identify], and [logout] return right away and are safe to call from any thread.
 * They switch to the new identity locally and send it to the backend in the background.
 */
object Joe {

    /** The shared network client used for every SDK operation. */
    private val networkClient = JoeNetworkClient

    /**
     * The current identity, or `null` when no local identity is available.
     *
     * @throws IllegalStateException if the SDK is not configured before accessing this property.
     */
    val identity: Identity?
        get() = networkClient.currentIdentity

    /**
     * Configures the SDK: stores [apiKey] and [isDebugMode], and switches to the identity for
     * [appUserId]. SDK components can load content as soon as this returns.
     *
     * Call it directly in [android.app.Application.onCreate], without a coroutine. It reads the
     * small persisted identity from disk on the calling thread, and writes it there on first
     * launch or when [appUserId] changes it. `StrictMode` reports both as disk access on the main
     * thread. It does no network work: the identity is sent to the backend in the background. If
     * that fails, the SDK logs it to Logcat with the tag `Joe` and stays usable. After a network
     * error, it sends the identity again on the next app start. An invalid API key disables SDK
     * requests until the app restarts; see [dev.mobilejoe.api.network.JoeInvalidApiKeyException].
     *
     * If the identity can't be stored on the device, the SDK logs that as well and stays
     * unconfigured instead of throwing, so SDK components show their error state until the next
     * app start.
     *
     * Safe to call more than once: it only takes effect the first time it succeeds.
     *
     * @param applicationContext The host app's context, used to read app/device info and to
     * persist the identity.
     * @param apiKey The API key issued for the host app.
     * @param appUserId The host app's own user identifier to associate with the identity, or
     * `null` to keep the persisted identity: an anonymous one on first launch, or the last
     * identified app user. A `null` id never logs out; call [logout] on sign-out. See [identify]
     * for the requirements on this id and what happens when it differs from the previously
     * identified one.
     * @param isDebugMode Whether requests should be tagged as debug traffic (e.g. bypassing
     * server-side throttling).
     * @throws IllegalArgumentException if [appUserId] is empty or blank.
     */
    fun configure(
        applicationContext: Context,
        apiKey: String,
        appUserId: String? = null,
        isDebugMode: Boolean = false
    ) {
        networkClient.configure(
            applicationContext = applicationContext,
            apiKey = apiKey,
            externalId = appUserId,
            isDebugMode = isDebugMode,
        )
    }

    /**
     * Associates the current identity with [appUserId].
     *
     * [appUserId] must be a stable id that belongs to exactly one account of the host app (e.g.
     * the account's primary key). Placeholders, receipt ids, or ids of purchase SDKs that change
     * after a restore (e.g. anonymous RevenueCat ids) are not suitable. Apps without their own
     * accounts should not call [identify] at all.
     *
     * - From an anonymous identity, the anonymous id is kept, so anonymous activity carries over
     *   to the app user.
     * - With the same [appUserId] as before, nothing changes and nothing is sent.
     * - With a different [appUserId] than before, the SDK [logout]s first and identifies with fresh
     *   anonymous and device ids, so activity of two app users never gets mixed.
     *
     * The new identity is visible in [identity] and used for requests as soon as this returns. It
     * is written to disk on the calling thread, which `StrictMode` reports on the main thread, and
     * sent to the backend in the background; a failure there is logged to Logcat with the tag
     * `Joe`, and the identity is sent again on the next app start.
     *
     * @param appUserId The host app's own user identifier.
     * @throws IllegalArgumentException if [appUserId] is empty or blank.
     * @throws IllegalStateException if the SDK has not been [configure]d yet.
     * @throws dev.mobilejoe.api.identity.JoeStorageException if the identity could not be
     * persisted.
     */
    fun identify(appUserId: String) {
        networkClient.identify(externalId = appUserId)
    }

    /**
     * Clears the current identity and establishes a fresh, anonymous one in its place, which is
     * written to disk on the calling thread and sent to the backend in the background. This is
     * the only way back to an anonymous identity; configuring without an `appUserId` keeps the
     * identified app user.
     *
     * @throws IllegalStateException if the SDK has not been [configure]d yet.
     * @throws dev.mobilejoe.api.identity.JoeStorageException if the identity could not be deleted
     * or persisted.
     */
    fun logout() {
        networkClient.logout()
    }
}
