// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.alert

import dev.mobilejoe.api.network.JoeNetworkClient
import dev.mobilejoe.api.network.NetworkClient
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

/**
 * [AlertsGateway] backed by [NetworkClient.getAlerts], throttled to at most one successful
 * request per [REFRESH_INTERVAL] (bypassed while [NetworkClient.isDebugMode] is enabled).
 *
 * All the bookkeeping this class does — the eligibility check in [canRequestNewAlerts], the
 * network call, and the cache/timestamp update — happens inside [load] under a single [Mutex],
 * so it is safe to call [load] concurrently from multiple coroutines (e.g. an initial load racing
 * a foreground-triggered refresh): only one network request will ever be in flight, and a failed
 * request never advances [lastFetch], leaving the gateway eligible to retry on the next call.
 *
 * @param networkClient The network client used to fetch alerts. Defaults to the shared
 * [JoeNetworkClient] singleton.
 * @param clock The clock used to evaluate the refresh interval. Overridable for tests.
 */
internal class RemoteAlertsGateway(
    private val networkClient: NetworkClient = JoeNetworkClient,
    private val clock: Clock = Clock.System,
) : AlertsGateway {

    /**
     * Serializes the throttle check, the network request, and the cache update in [load] so
     * concurrent callers (e.g. an initial load racing a foreground-triggered refresh) can't
     * both observe [lastFetch] as stale before either has updated it, and can't have an
     * in-flight response overwrite a more recent one.
     */
    private val loadMutex = Mutex()

    /** The most recently fetched alerts, or an empty list before the first successful fetch. */
    private var alerts: List<Alert> = emptyList()

    /** The point in time of the last *successful* fetch, or `null` if none has succeeded yet. */
    private var lastFetch: Instant? = null

    /**
     * Whether [load] is allowed to perform a new network request right now.
     *
     * `true` when there has never been a successful fetch, when debug mode is enabled (which
     * bypasses throttling entirely), or when at least [REFRESH_INTERVAL] has elapsed since
     * [lastFetch].
     */
    private val canRequestNewAlerts: Boolean
        get() {
            val lastFetch = lastFetch

            return when {
                lastFetch == null -> true
                networkClient.isDebugMode -> true
                else -> clock.now() - lastFetch >= REFRESH_INTERVAL
            }
        }

    /**
     * Returns the cached [alerts] if [canRequestNewAlerts] is `false`; otherwise fetches a
     * fresh list from [networkClient], caches it, and records [lastFetch].
     *
     * The whole check-fetch-update sequence runs under [loadMutex], so this method is safe to
     * call concurrently from multiple coroutines.
     *
     * @return The cached alerts, or a freshly fetched list when eligible.
     * @throws Exception if a network request is attempted and fails. On failure, [alerts] and
     * [lastFetch] are left unchanged, so the next call remains eligible to retry.
     */
    override suspend fun load(): List<Alert> = loadMutex.withLock {
        if (!canRequestNewAlerts) {
            return@withLock alerts
        }

        alerts = networkClient.getAlerts()
        lastFetch = clock.now()
        alerts
    }

    private companion object {

        /** Minimum time that must pass between two successful, non-debug-mode fetches. */
        val REFRESH_INTERVAL: Duration = 15.minutes
    }
}
