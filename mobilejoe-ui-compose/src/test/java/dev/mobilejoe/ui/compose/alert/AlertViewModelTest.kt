// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.ui.compose.alert

import android.util.Log
import app.cash.turbine.test
import app.cash.turbine.turbineScope
import dev.mobilejoe.api.alert.Alert
import dev.mobilejoe.api.alert.Alerts
import dev.mobilejoe.ui.compose.viewModelScope
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class AlertViewModelTest {

    @Test
    fun `Test initial loading`() = runTest {
        val alerts = mockk<Alerts>(relaxed = true) {
            every { all } returns MutableStateFlow(null)
        }
        val viewModel = AlertsViewModel(alerts, viewModelScope)

        assertEquals(
            AlertsViewModel.State.Loading,
            viewModel.state.first()
        )
        assertNull(viewModel.activeAlertInfo.first())
    }

    @Test
    fun `Test initial error`() = runTest {
        turbineScope {
            mockkStatic(Log::class)
            every { Log.e(any(), any(), any()) } returns 0

            val alerts = mockk<Alerts>(relaxed = true) {
                // Production Alerts.all never emits a non-null value until the first
                // successful load, so an initial load failure must be observable while
                // `all` is still null.
                every { all } returns MutableStateFlow(null)
                coEvery { load() } throws Exception()
            }
            val viewModel = AlertsViewModel(alerts, viewModelScope)

            viewModel.state.test {
                skipItems(1) // Skip loading state

                assertEquals(
                    AlertsViewModel.State.Error,
                    awaitItem()
                )
            }
            assertNull(viewModel.activeAlertInfo.first())
        }
    }

    @Test
    fun `Test initial empty`() = runTest {
        turbineScope {
            val alerts = mockk<Alerts>(relaxed = true) {
                every { all } returns MutableStateFlow(listOf())
                coEvery { load() } returns Unit
            }
            val viewModel = AlertsViewModel(alerts, viewModelScope)

            viewModel.state.test {
                skipItems(1) // Skip loading state

                assertEquals(
                    AlertsViewModel.State.Empty,
                    awaitItem()
                )
            }
            assertNull(viewModel.activeAlertInfo.first())
        }
    }

    @Test
    fun `Test initial success`() = runTest {
        turbineScope {
            val alertItems = listOf(
                Alert.Companion.empty().copy(id = 42),
                Alert.Companion.empty().copy(id = 43)
            )

            val alerts = mockk<Alerts>(relaxed = true) {
                every { all } returns MutableStateFlow(alertItems)
                coEvery { load() } returns Unit
            }
            val viewModel = AlertsViewModel(alerts, viewModelScope)

            viewModel.state.test {
                skipItems(1) // Skip loading state

                assertEquals(
                    AlertsViewModel.State.Success(
                        alertItems = alertItems
                    ),
                    awaitItem()
                )
            }
            assertNull(viewModel.activeAlertInfo.first())
        }
    }

    @Test
    fun `Test click on alert`() = runTest {
        val alerts = mockk<Alerts>(relaxed = true) {
            every { all } returns MutableStateFlow(null)
        }
        val viewModel = AlertsViewModel(alerts, viewModelScope)

        val alert = Alert.empty().copy(id = 42)
        viewModel.onAlertClick(alert)

        assertEquals(
            alert,
            viewModel.activeAlertInfo.first()
        )
    }

    @Test
    fun `Test dismiss active alert info`() = runTest {
        val alerts = mockk<Alerts>(relaxed = true) {
            every { all } returns MutableStateFlow(null)
        }
        val viewModel = AlertsViewModel(alerts, viewModelScope)

        val alert = Alert.empty().copy(id = 42)
        viewModel.onAlertClick(alert)

        viewModel.onDismissAlertInfo()

        assertNull(
            viewModel.activeAlertInfo.first()
        )
    }

    @Test
    fun `Test foreground refresh`() = runTest {
        turbineScope {
            val alertItems = listOf(Alert.empty().copy(id = 42))
            val alerts = mockk<Alerts>(relaxed = true) {
                every { all } returns MutableStateFlow(alertItems)
                coEvery { load() } returns Unit
            }
            val viewModel = AlertsViewModel(alerts, viewModelScope)

            viewModel.state.test {
                skipItems(1) // Skip loading state

                assertEquals(
                    AlertsViewModel.State.Success(alertItems = alertItems),
                    awaitItem()
                )
            }

            viewModel.onStart()

            advanceUntilIdle()
            coVerify(exactly = 2) { alerts.load() } // initial load & foreground refresh
        }
    }

    @Test
    fun `Test foreground refresh failure keeps alerts visible`() = runTest {
        turbineScope {
            mockkStatic(Log::class)
            every { Log.e(any(), any(), any()) } returns 0

            val alertItems = listOf(Alert.empty().copy(id = 42))
            val alerts = mockk<Alerts>(relaxed = true) {
                every { all } returns MutableStateFlow(alertItems)
                coEvery { load() } returns Unit andThenThrows Exception()
            }
            val viewModel = AlertsViewModel(alerts, viewModelScope)

            viewModel.state.test {
                skipItems(1) // Skip loading state

                assertEquals(
                    AlertsViewModel.State.Success(alertItems = alertItems),
                    awaitItem()
                )
            }

            viewModel.onStart()

            advanceUntilIdle()
            coVerify(exactly = 2) { alerts.load() } // initial load & foreground refresh

            assertEquals(
                AlertsViewModel.State.Success(alertItems = alertItems),
                viewModel.state.value
            )
            verify { Log.e(any(), any(), any()) }
        }
    }
}