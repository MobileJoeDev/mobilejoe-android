// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.identity.util

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Test
import java.util.UUID
import kotlin.test.assertEquals

class JoeIdFactoryTest {

    @Test
    fun `Test create anonymous ID`() {
        mockkStatic(UUID::class)
        every { UUID.randomUUID() } returns mockk {
            every { this@mockk.toString() } returns "AAAA-1111-BBBB-2222"
        }

        val anonymousId = JoeIdFactory.createAnonymousId()

        assertEquals($$"$MBJAnonymousID:aaaa1111bbbb2222", anonymousId)
    }

    @Test
    fun `Test create external ID`() {
        mockkStatic("dev.mobilejoe.api.identity.util.IdExtensionsKt")
        every { any<String>().sha265Hash() } returns "123456789-123456789-123456789-123456789-"

        val externalId = JoeIdFactory.createExternalId("external-42")

        assertEquals($$"$MBJExternalID:123456789-123456789-123456789-12", externalId)
    }
}
