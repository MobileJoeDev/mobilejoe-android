// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.faq

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class InMemoryFaqsGatewayTest {

    @Test
    fun `Test preview loads sample faqs`() = runTest {
        val faqs = Faqs.preview()

        faqs.load()

        assertEquals(InMemoryFaqsGateway.seedData, faqs.all.first())
    }

    @Test
    fun `Test load by id`() = runTest {
        val gateway = InMemoryFaqsGateway(InMemoryFaqsGateway.seedData)

        assertEquals(2, gateway.load(2).id)
        assertFailsWith<NoSuchElementException> { gateway.load(42) }
    }

    @Test
    fun `Test preview empty loads no faqs`() = runTest {
        val faqs = Faqs.previewEmpty()

        faqs.load()

        assertEquals(emptyList(), faqs.all.first())
    }

    @Test
    fun `Test preview failed throws on load`() = runTest {
        assertFailsWith<IllegalStateException> { Faqs.previewFailed().load() }
    }
}
