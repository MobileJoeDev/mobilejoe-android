// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.ui.compose.faq.list

import android.util.Log
import app.cash.turbine.test
import app.cash.turbine.turbineScope
import dev.mobilejoe.api.faq.Faq
import dev.mobilejoe.api.faq.Faqs
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
class FaqListViewModelTest {

    @Test
    fun `Test initial loading`() = runTest {
        val faqs = mockk<Faqs>(relaxed = true) {
            every { all } returns flow {}
        }
        val viewModel = FaqListViewModel(faqs, viewModelScope)

        assertEquals(
            FaqListViewModel.State.Loading,
            viewModel.state.first(),
        )
    }

    @Test
    fun `Test initial error`() = runTest {
        turbineScope {
            mockkStatic(Log::class)
            every { Log.e(any(), any(), any()) } returns 0

            val faqs = mockk<Faqs>(relaxed = true) {
                every { all } returns flow {
                    emit(listOf())
                }
                coEvery { load() } throws Exception()
            }
            val viewModel = FaqListViewModel(faqs, viewModelScope)

            viewModel.state.test {
                skipItems(1) // Skip loading state

                assertEquals(
                    FaqListViewModel.State.Error,
                    awaitItem(),
                )
            }
        }
    }

    @Test
    fun `Test initial empty`() = runTest {
        turbineScope {
            val faqs = mockk<Faqs>(relaxed = true) {
                every { all } returns flow {
                    emit(listOf())
                }
                coEvery { load() } returns Unit
            }
            val viewModel = FaqListViewModel(faqs, viewModelScope)

            viewModel.state.test {
                skipItems(1) // Skip loading state

                assertEquals(
                    FaqListViewModel.State.Empty,
                    awaitItem(),
                )
            }
        }
    }

    @Test
    fun `Test initial success`() = runTest {
        turbineScope {
            val faqItems = listOf(
                Faq.empty().copy(id = 1),
                Faq.empty().copy(id = 2)
            )

            val faqs = mockk<Faqs>(relaxed = true) {
                every { all } returns flow {
                    emit(faqItems)
                }
                coEvery { load() } returns Unit
            }
            val viewModel = FaqListViewModel(faqs, viewModelScope)

            viewModel.state.test {
                skipItems(1) // Skip loading state

                assertEquals(
                    FaqListViewModel.State.Success(
                        faqItems = faqItems,
                    ),
                    awaitItem(),
                )
            }
        }
    }

    @Test
    fun `Test retry failed loading`() = runTest {
        turbineScope {
            mockkStatic(Log::class)
            every { Log.e(any(), any(), any()) } returns 0

            val faqItems = listOf(Faq.empty().copy(id = 1))
            val faqs = mockk<Faqs>(relaxed = true) {
                every { all } returns flow {
                    emit(faqItems)
                }
                coEvery { load() } throws Exception() andThen Unit
            }
            val viewModel = FaqListViewModel(faqs, viewModelScope)

            viewModel.state.test {
                skipItems(2) // Skip initial & error state

                viewModel.onRetryClicked()

                assertEquals(
                    FaqListViewModel.State.Success(
                        faqItems = faqItems,
                    ),
                    awaitItem(),
                )
                coVerify(exactly = 2) { faqs.load() } // initial load & retry
            }
        }
    }

    @Test
    fun `Test retry failure after initial error stays on error`() = runTest {
        turbineScope {
            mockkStatic(Log::class)
            every { Log.e(any(), any(), any()) } returns 0

            val faqs = mockk<Faqs>(relaxed = true) {
                every { all } returns flow {
                    emit(listOf())
                }
                coEvery { load() } throws Exception()
            }
            val viewModel = FaqListViewModel(faqs, viewModelScope)

            viewModel.state.test {
                skipItems(2) // Skip initial loading & error state

                viewModel.onRetryClicked()

                advanceUntilIdle()
                expectNoEvents()
            }

            assertEquals(FaqListViewModel.State.Error, viewModel.state.value)
            coVerify(exactly = 2) { faqs.load() } // initial load & retry
        }
    }

    @Test
    fun `Test refresh`() = runTest {
        turbineScope {
            mockkStatic(Log::class)
            every { Log.e(any(), any(), any()) } returns 0

            val faqItems = listOf(Faq.empty().copy(id = 1))
            val faqs = mockk<Faqs>(relaxed = true) {
                every { all } returns flow {
                    emit(faqItems)
                }
                coEvery { load() } throws Exception() andThen Unit
            }
            val viewModel = FaqListViewModel(faqs, viewModelScope)

            viewModel.state.test {
                skipItems(2) // Skip initial & error state

                viewModel.onRefresh()

                assertEquals(
                    FaqListViewModel.State.Success(
                        faqItems = faqItems,
                    ),
                    awaitItem(),
                )
            }

            coVerify(exactly = 2) { faqs.load() } // initial load & refresh
        }
    }

    @Test
    fun `Test refresh failure keeps previous list`() = runTest {
        turbineScope {
            mockkStatic(Log::class)
            every { Log.e(any(), any(), any()) } returns 0

            val faqItems = listOf(Faq.empty().copy(id = 1))
            val faqs = mockk<Faqs>(relaxed = true) {
                every { all } returns flow {
                    emit(faqItems)
                }
                coEvery { load() } returns Unit andThenThrows Exception()
            }
            val viewModel = FaqListViewModel(faqs, viewModelScope)

            viewModel.state.test {
                skipItems(2) // Skip loading, and list state

                viewModel.onRefresh()

                advanceUntilIdle()
                expectNoEvents()
            }
        }
    }
}