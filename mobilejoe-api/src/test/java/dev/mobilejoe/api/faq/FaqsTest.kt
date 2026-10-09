// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.faq

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class FaqsTest {

    @Test
    fun `Test load faqs`() = runTest {
        val faqList = listOf(
            Faq.empty().copy(id = 8),
            Faq.empty().copy(id = 42),
        )
        val faqsGateway = mockk<FaqsGateway> {
            coEvery { load() } returns faqList
        }
        val faqs = Faqs(faqsGateway)

        faqs.load()

        coVerify(exactly = 1) { faqsGateway.load() }
        assertEquals(faqList, faqs.all.first())
    }

    @Test
    fun `Test load faq`() = runTest {
        val faqItem = Faq.empty().copy(id = 42)
        val faqsGateway = mockk<FaqsGateway> {
            coEvery { load(42) } returns faqItem
        }
        val faqs = Faqs(faqsGateway)

        faqs.load(42)

        coVerify(exactly = 1) { faqsGateway.load(42) }
        assertEquals(faqItem, faqs.single.first())
    }

    @Test
    fun `Test reload replaces the list`() = runTest {
        val firstFaqList = listOf(Faq.empty().copy(id = 8))
        val secondFaqList = listOf(Faq.empty().copy(id = 42))
        val faqsGateway = mockk<FaqsGateway> {
            coEvery { load() } returnsMany listOf(firstFaqList, secondFaqList)
        }
        val faqs = Faqs(faqsGateway)

        faqs.load()
        faqs.load()

        coVerify(exactly = 2) { faqsGateway.load() }
        assertEquals(secondFaqList, faqs.all.first())
    }

    @Test
    fun `Test reload keeps the previous list when it fails`() = runTest {
        val faqList = listOf(Faq.empty().copy(id = 8))
        val faqsGateway = mockk<FaqsGateway> {
            coEvery { load() } returns faqList andThenThrows Exception()
        }
        val faqs = Faqs(faqsGateway)

        faqs.load()
        try {
            faqs.load()
        } catch (e: Exception) {
            // Ignored for test purposes
        }

        assertEquals(faqList, faqs.all.first())
    }
}
