// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.ui.compose.faq.detail

import android.util.Log
import app.cash.turbine.test
import app.cash.turbine.turbineScope
import dev.mobilejoe.api.faq.Faq
import dev.mobilejoe.api.faq.Faqs
import dev.mobilejoe.ui.compose.viewModelScope
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class FaqDetailViewModelTest {

    @Test
    fun `Test initial loading`() = runTest {
        val faqs = mockk<Faqs>(relaxed = true) {
            every { single } returns flow {}
        }
        val viewModel = FaqDetailViewModel(42, faqs, viewModelScope = viewModelScope)

        assertEquals(
            FaqDetailViewModel.State.Loading,
            viewModel.state.first()
        )
    }

    @Test
    fun `Test initial error`() = runTest {
        turbineScope {
            mockkStatic(Log::class)
            every { Log.e(any(), any(), any()) } returns 0

            val faqs = mockk<Faqs>(relaxed = true) {
                every { single } returns flow {
                    emit(null)
                }
                coEvery { load(any()) } throws Exception()
            }
            val viewModel = FaqDetailViewModel(42, faqs, viewModelScope = viewModelScope)

            viewModel.state.test {
                skipItems(1) // Skip loading state

                assertEquals(
                    FaqDetailViewModel.State.Error,
                    awaitItem()
                )
            }
        }
    }

    @Test
    fun `Test initial success`() = runTest {
        turbineScope {
            val faqItem = Faq.empty().copy(id = 42)

            val faqs = mockk<Faqs>(relaxed = true) {
                every { single } returns flow {
                    emit(faqItem)
                }
                coEvery { load() } returns Unit
            }
            val viewModel = FaqDetailViewModel(42, faqs, viewModelScope = viewModelScope)

            viewModel.state.test {
                skipItems(1) // Skip loading state

                assertEquals(
                    FaqDetailViewModel.State.Success(
                        faqItem = faqItem,
                    ),
                    awaitItem()
                )
            }
        }
    }

    @Test
    fun `Test retry failed loading`() = runTest {
        turbineScope {
            mockkStatic(Log::class)
            every { Log.e(any(), any(), any()) } returns 0

            val faqItem = Faq.empty().copy(id = 42)
            val faqs = mockk<Faqs>(relaxed = true) {
                every { single } returns flow {
                    emit(faqItem)
                }
                coEvery { load(42) } throws Exception() andThen Unit
            }
            val viewModel = FaqDetailViewModel(42, faqs, viewModelScope = viewModelScope)

            viewModel.state.test {
                skipItems(2) // Skip initial & error state

                viewModel.onRetryClicked()

                assertEquals(
                    FaqDetailViewModel.State.Success(
                        faqItem = faqItem,
                    ),
                    awaitItem()
                )
            }
        }
    }
}
