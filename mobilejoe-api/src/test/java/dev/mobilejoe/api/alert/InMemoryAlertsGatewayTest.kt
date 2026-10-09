// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.alert

import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class InMemoryAlertsGatewayTest {

    @Test
    fun `Test preview loads sample alerts`() = runTest {
        val alerts = Alerts.preview()

        alerts.load()

        assertEquals(InMemoryAlertsGateway.seedData, alerts.all.value)
    }

    @Test
    fun `Test preview single loads one alert`() = runTest {
        val alerts = Alerts.previewSingle()

        alerts.load()

        assertEquals(InMemoryAlertsGateway.singleAlert, alerts.all.value)
    }

    @Test
    fun `Test preview empty loads no alerts`() = runTest {
        val alerts = Alerts.previewEmpty()

        alerts.load()

        assertEquals(emptyList(), alerts.all.value)
    }
}
