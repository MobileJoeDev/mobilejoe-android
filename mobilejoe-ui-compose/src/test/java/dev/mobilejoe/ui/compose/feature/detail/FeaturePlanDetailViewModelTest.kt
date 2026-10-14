// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.ui.compose.feature.detail

import android.util.Log
import app.cash.turbine.test
import app.cash.turbine.turbineScope
import dev.mobilejoe.api.feature.FeaturePlan
import dev.mobilejoe.api.feature.FeaturePlans
import dev.mobilejoe.ui.compose.viewModelScope
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class FeaturePlanDetailViewModelTest {

    @Test
    fun `Test initial loading`() = runTest {
        val featurePlans = mockk<FeaturePlans>(relaxed = true) {
            every { single } returns flow {}
        }
        val viewModel = FeaturePlanDetailViewModel(42, featurePlans, viewModelScope)

        assertEquals(
            FeaturePlanDetailViewModel.State.Loading,
            viewModel.state.first()
        )
    }

    @Test
    fun `Test initial error`() = runTest {
        turbineScope {
            mockkStatic(Log::class)
            every { Log.e(any(), any(), any()) } returns 0

            val featurePlans = mockk<FeaturePlans>(relaxed = true) {
                every { single } returns flow {
                    emit(null)
                }
                coEvery { load(any()) } throws Exception()
            }
            val viewModel = FeaturePlanDetailViewModel(42, featurePlans, viewModelScope)

            viewModel.state.test {
                skipItems(1) // Skip loading state

                assertEquals(
                    FeaturePlanDetailViewModel.State.Error,
                    awaitItem()
                )
            }
        }
    }

    @Test
    fun `Test initial success`() = runTest {
        turbineScope {
            val featurePlanItem = FeaturePlan.empty().copy(id = 42)

            val featurePlans = mockk<FeaturePlans>(relaxed = true) {
                every { single } returns flow {
                    emit(featurePlanItem)
                }
                coEvery { load() } returns Unit
            }
            val viewModel = FeaturePlanDetailViewModel(42, featurePlans, viewModelScope)

            viewModel.state.test {
                skipItems(1) // Skip loading state

                assertEquals(
                    FeaturePlanDetailViewModel.State.Success(
                        featurePlanItem = featurePlanItem,
                    ),
                    awaitItem()
                )
            }
        }
    }

    @Test
    fun `Test feature plan with other ID keeps loading`() = runTest {
        turbineScope {
            val featurePlans = mockk<FeaturePlans>(relaxed = true) {
                every { single } returns flow {
                    emit(FeaturePlan.empty().copy(id = 7))
                }
                coEvery { load(42) } returns Unit
            }
            val viewModel = FeaturePlanDetailViewModel(42, featurePlans, viewModelScope)

            viewModel.state.test {
                assertEquals(
                    FeaturePlanDetailViewModel.State.Loading,
                    awaitItem()
                )

                advanceUntilIdle()
                expectNoEvents()
            }
        }
    }

    @Test
    fun `Test retry failed loading`() = runTest {
        turbineScope {
            mockkStatic(Log::class)
            every { Log.e(any(), any(), any()) } returns 0

            val featurePlanItem = FeaturePlan.empty().copy(id = 42)
            val featurePlans = mockk<FeaturePlans>(relaxed = true) {
                every { single } returns flow {
                    emit(featurePlanItem)
                }
                coEvery { load(42) } throws Exception() andThen Unit
            }
            val viewModel = FeaturePlanDetailViewModel(42, featurePlans, viewModelScope)

            viewModel.state.test {
                skipItems(1) // Skip error state

                viewModel.onRetryClicked()

                assertEquals(
                    FeaturePlanDetailViewModel.State.Success(
                        featurePlanItem = featurePlanItem,
                    ),
                    awaitItem()
                )
            }
        }
    }

    @Test
    fun `Test vote`() = runTest {
        turbineScope {
            val featurePlanItem = FeaturePlan.empty().copy(id = 42)

            val featurePlans = mockk<FeaturePlans>(relaxed = true) {
                every { single } returns flow {
                    emit(featurePlanItem)
                }
                coEvery { load(42) } returns Unit
            }
            val viewModel = FeaturePlanDetailViewModel(42, featurePlans, viewModelScope)

            viewModel.onUpVoteClicked(featurePlanItem)

            advanceUntilIdle()
            coVerify(exactly = 1) { featurePlans.vote(featurePlanItem) }
        }
    }

    @Test
    fun `Test vote error`() = runTest {
        turbineScope {
            mockkStatic(Log::class)
            every { Log.e(any(), any(), any()) } returns 0

            val featurePlanItem = FeaturePlan.empty().copy(id = 42)

            val featurePlans = mockk<FeaturePlans>(relaxed = true) {
                every { single } returns flow {
                    emit(featurePlanItem)
                }
                coEvery { load(42) } returns Unit
                coEvery { vote(featurePlanItem) } throws Exception()
            }
            val viewModel = FeaturePlanDetailViewModel(42, featurePlans, viewModelScope)

            viewModel.onUpVoteClicked(featurePlanItem)

            advanceUntilIdle()
            coVerify(exactly = 1) { featurePlans.vote(featurePlanItem) }
        }
    }
}
