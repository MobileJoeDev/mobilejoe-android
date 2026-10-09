// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api

import android.content.Context
import dev.mobilejoe.api.identity.Identity
import dev.mobilejoe.api.network.JoeNetworkClient

/**
 * Top-level entry point for configuring and interacting with the MobileJoe SDK.
 *
 * A host app calls [configure] exactly once (typically from [android.app.Application.onCreate])
 * before touching any other part of the SDK — every other member of [Joe], and every SDK type
 * that talks to the network (e.g. `Alerts`, `Faqs`, `FeaturePlans`), assumes a configured
 * [networkClient] and will throw if used beforehand. Once configured, [identify] and [logout]
 * let the host app associate the SDK's identity with its own signed-in user and clear it again,
 * respectively.
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
     * Configures the SDK: identifies the current device/user and, once that succeeds, stores
     * [apiKey] and [isDebugMode] for use by subsequent calls.
     *
     * Must be called (and succeed) before using any other SDK functionality; a failed call
     * leaves the SDK unconfigured rather than partially configured.
     *
     * Safe to call more than once: it only takes effect the first time, and calls made at the
     * same time simply wait for one another instead of running at once.
     *
     * @param applicationContext The host app's context, used to read app/device info and to
     * persist the identity.
     * @param apiKey The API key issued for the host app.
     * @param appUserId The host app's own user identifier to associate with the identity, or
     * `null` to keep the persisted identity: an anonymous one on first launch, or the last
     * identified account. A `null` id never logs out; call [logout] on sign-out. See [identify]
     * for the requirements on this id and what happens when it differs from the previously
     * identified one.
     * @param isDebugMode Whether requests should be tagged as debug traffic (e.g. bypassing
     * server-side throttling).
     * @throws IllegalArgumentException if [appUserId] is empty or blank.
     * @throws dev.mobilejoe.api.network.JoeHttpException if identification responds with a
     * non-2xx status.
     * @throws java.io.IOException if identification fails due to a network problem.
     */
    suspend fun configure(
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
     *   to the account.
     * - With the same [appUserId] as before, nothing changes.
     * - With a different [appUserId] than before, the SDK [logout]s first and identifies with fresh
     *   anonymous and device ids, so activity of two accounts never gets mixed.
     *
     * The new account becomes visible in [identity] only after the backend accepted it. If that
     * fails after an implicit logout, [identity] stays the fresh anonymous one.
     *
     * @param appUserId The host app's own user identifier.
     * @throws IllegalArgumentException if [appUserId] is empty or blank.
     * @throws IllegalStateException if the SDK has not been [configure]d yet.
     * @throws dev.mobilejoe.api.network.JoeHttpException if identification responds with a
     * non-2xx status.
     * @throws java.io.IOException if identification fails due to a network problem.
     */
    suspend fun identify(appUserId: String) {
        networkClient.identify(externalId = appUserId)
    }

    /**
     * Clears the current identity and establishes a fresh, anonymous one in its place. This is the
     * only way back to an anonymous identity; configuring without an `appUserId` keeps the account.
     *
     * @throws Exception if the SDK has not been [configure]d yet.
     */
    suspend fun logout() {
        networkClient.logout()
    }
}
