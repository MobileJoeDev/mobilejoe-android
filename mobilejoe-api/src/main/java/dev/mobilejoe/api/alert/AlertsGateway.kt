// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.alert

/**
 * Fetches alerts from wherever they're sourced from (typically the MobileJoe backend).
 *
 * This is the abstraction [Alerts] delegates to for the actual data fetch. Splitting it out lets
 * [Alerts] stay a thin, testable wrapper around a [Flow][kotlinx.coroutines.flow.Flow] while the
 * gateway owns everything about *how* and *how often* a fetch actually happens — see
 * [RemoteAlertsGateway] for the default implementation, which adds throttling on top of the
 * plain network call.
 */
interface AlertsGateway {

    /**
     * Returns the current list of alerts.
     *
     * Implementations are free to apply their own caching/throttling policy; callers should not
     * assume this always performs a network request.
     *
     * @return The current list of alerts.
     * @throws Exception if a fresh fetch is attempted and fails.
     */
    suspend fun load(): List<Alert>
}
