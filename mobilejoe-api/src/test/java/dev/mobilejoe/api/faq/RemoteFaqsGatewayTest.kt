// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.faq

import dev.mobilejoe.api.network.NetworkClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals

class RemoteFaqsGatewayTest {

    @Test
    fun `Test load`() = runTest {
        val faqs = listOf(
            Faq.empty().copy(id = 8),
            Faq.empty().copy(id = 42),
        )

        val networkClient = mockk<NetworkClient> {
            coEvery { getFaqs() } returns faqs
        }
        val faqsGateway = RemoteFaqsGateway(networkClient)

        val result = faqsGateway.load()

        coVerify { networkClient.getFaqs() }
        assertEquals(faqs, result)
    }

    @Test
    fun `Test load single`() = runTest {
        val faq = Faq.empty().copy(id = 42)

        val networkClient = mockk<NetworkClient> {
            coEvery { getFaq(any()) } returns faq
        }
        val faqsGateway = RemoteFaqsGateway(networkClient)

        val result = faqsGateway.load(42)

        coVerify {
            networkClient.getFaq(42)
        }
        assertEquals(faq, result)
    }
}
