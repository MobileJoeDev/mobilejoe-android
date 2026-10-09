// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.alert

import dev.mobilejoe.api.network.NetworkClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class RemoteAlertsGatewayTest {

    @Test
    fun `Test initial load`() = runTest {
        val alerts = listOf(
            Alert.empty().copy(id = 8),
            Alert.empty().copy(id = 42),
        )

        val networkClient = mockk<NetworkClient> {
            coEvery { getAlerts() } returns alerts
            every { isDebugMode } returns false
        }
        val alertsGateway = RemoteAlertsGateway(networkClient)

        val result = alertsGateway.load()

        coVerify(exactly = 1) { networkClient.getAlerts() }
        assertEquals(alerts, result)
    }

    @Test
    fun `Test refresh after interval elapsed`() = runTest {
        val firstAlerts = listOf(Alert.empty().copy(id = 8))
        val secondAlerts = listOf(Alert.empty().copy(id = 42))

        var now = Instant.parse("2026-09-24T10:00:00Z")
        val clock = mockk<Clock> {
            every { now() } answers { now }
        }

        val networkClient = mockk<NetworkClient> {
            coEvery { getAlerts() } returns firstAlerts andThen secondAlerts
            every { isDebugMode } returns false
        }
        val alertsGateway = RemoteAlertsGateway(networkClient, clock)

        val firstResult = alertsGateway.load()
        now = now + 15.minutes + 1.seconds
        val secondResult = alertsGateway.load()

        coVerify(exactly = 2) { networkClient.getAlerts() }
        assertEquals(firstAlerts, firstResult)
        assertEquals(secondAlerts, secondResult)
    }

    @Test
    fun `Test suppress load within interval`() = runTest {
        val firstAlerts = listOf(Alert.empty().copy(id = 8))

        var now = Instant.parse("2026-09-24T10:00:00Z")
        val clock = mockk<Clock> {
            every { now() } answers { now }
        }

        val networkClient = mockk<NetworkClient> {
            coEvery { getAlerts() } returns firstAlerts
            every { isDebugMode } returns false
        }
        val alertsGateway = RemoteAlertsGateway(networkClient, clock)

        val firstResult = alertsGateway.load()
        now = now + 14.minutes
        val secondResult = alertsGateway.load()

        coVerify(exactly = 1) { networkClient.getAlerts() }
        assertEquals(firstAlerts, firstResult)
        assertEquals(firstAlerts, secondResult)
    }

    @Test
    fun `Test failed fetch does not start interval`() = runTest {
        val alerts = listOf(Alert.empty().copy(id = 8))

        val now = Instant.parse("2026-09-24T10:00:00Z")
        val clock = mockk<Clock> {
            every { now() } returns now
        }

        val networkClient = mockk<NetworkClient> {
            coEvery { getAlerts() } throws Exception() andThen alerts
            every { isDebugMode } returns false
        }
        val alertsGateway = RemoteAlertsGateway(networkClient, clock)

        val exception = runCatching { alertsGateway.load() }.exceptionOrNull()
        val result = alertsGateway.load()

        coVerify(exactly = 2) { networkClient.getAlerts() }
        assertEquals(true, exception != null)
        assertEquals(alerts, result)
    }

    @Test
    fun `Test concurrent loads are serialized`() = runTest {
        val alerts = listOf(Alert.empty().copy(id = 8))

        val networkClient = mockk<NetworkClient> {
            coEvery { getAlerts() } coAnswers {
                delay(1_000)
                alerts
            }
            every { isDebugMode } returns false
        }
        val alertsGateway = RemoteAlertsGateway(networkClient)

        // Both calls start before either has observed the other's effect on lastFetch.
        val first = async { alertsGateway.load() }
        val second = async { alertsGateway.load() }
        val results = awaitAll(first, second)

        coVerify(exactly = 1) { networkClient.getAlerts() }
        assertEquals(listOf(alerts, alerts), results)
    }

    @Test
    fun `Test debug mode bypasses interval`() = runTest {
        val firstAlerts = listOf(Alert.empty().copy(id = 8))
        val secondAlerts = listOf(Alert.empty().copy(id = 42))

        val networkClient = mockk<NetworkClient> {
            coEvery { getAlerts() } returns firstAlerts andThen secondAlerts
            every { isDebugMode } returns true
        }
        val alertsGateway = RemoteAlertsGateway(networkClient, Clock.System)

        val firstResult = alertsGateway.load()
        val secondResult = alertsGateway.load()

        coVerify(exactly = 2) { networkClient.getAlerts() }
        assertEquals(firstAlerts, firstResult)
        assertEquals(secondAlerts, secondResult)
    }
}
