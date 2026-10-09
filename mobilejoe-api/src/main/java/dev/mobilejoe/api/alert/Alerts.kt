// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.alert

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Public entry point for observing and loading alert banners.
 *
 * Wraps an [AlertsGateway] and exposes the current list of alerts as a hot [Flow], letting
 * multiple UI observers share a single underlying fetch/cache without each needing its own
 * network call.
 *
 * This class does not fetch anything on construction: [all] immediately emits `null` until
 * [load] has been called and completed successfully at least once. A typical caller (e.g. a
 * ViewModel) constructs one [Alerts] instance, calls [load] once from an `init` block, and
 * collects [all] for the current list; subsequent [load] calls (e.g. from a pull-to-refresh
 * gesture or an app-foreground trigger) are cheap to call repeatedly since throttling is handled
 * entirely by [alertsGateway].
 *
 * @param alertsGateway The gateway used to fetch alerts. Defaults to a [RemoteAlertsGateway]
 * backed by the real network client; callers wire in a fake/mock for tests.
 */
class Alerts(
    private val alertsGateway: AlertsGateway = RemoteAlertsGateway()
) {

    /** Backing store for [all]; `null` until the first successful [load]. */
    private val _all = MutableStateFlow<List<Alert>?>(null)

    /**
     * The most recently loaded alerts, or `null` if [load] has not yet completed successfully.
     * Always has a current value, so combinators like [kotlinx.coroutines.flow.combine] can
     * observe an initial (unloaded) state instead of waiting silently for the first success.
     */
    val all: StateFlow<List<Alert>?> = _all

    /**
     * Loads alerts through [alertsGateway] and publishes the result to [all].
     *
     * Delegates all throttling/caching decisions to the gateway: if the gateway decides the
     * request should be skipped (e.g. it was called too recently), the gateway's cached result
     * is emitted instead of making a new network call.
     *
     * @throws Exception if the underlying gateway request fails. On failure, [all] is left
     * unchanged so previously loaded alerts remain visible to observers.
     */
    suspend fun load() {
        val alerts = alertsGateway.load()
        _all.emit(alerts)
    }

    companion object {

        fun preview(): Alerts = Alerts(InMemoryAlertsGateway(InMemoryAlertsGateway.seedData))

        fun previewSingle(): Alerts = Alerts(InMemoryAlertsGateway(InMemoryAlertsGateway.singleAlert))

        fun previewEmpty(): Alerts = Alerts(InMemoryAlertsGateway())
    }
}
