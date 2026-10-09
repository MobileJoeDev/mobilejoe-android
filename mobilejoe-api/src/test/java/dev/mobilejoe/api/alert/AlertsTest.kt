// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.alert

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class AlertsTest {

    @Test
    fun `Test get alerts`() = runTest {
        val alertsList = listOf(
            Alert.empty().copy(id = 8),
            Alert.empty().copy(id = 42),
        )
        val alertsGateway = mockk<AlertsGateway> {
            coEvery { load() } returns alertsList
        }
        val alerts = Alerts(alertsGateway)

        alerts.load()

        coVerify(exactly = 1) { alertsGateway.load() }
        assertEquals(alertsList, alerts.all.first())
    }
}
