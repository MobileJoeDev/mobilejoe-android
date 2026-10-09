// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.identity.util

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import junit.framework.TestCase.assertEquals
import org.junit.Test
import java.util.UUID

class IdExtensionsTest {

    @Test
    fun `Test UUID to compat string`() {
        mockkStatic(UUID::class)
        every { UUID.randomUUID() } returns mockk {
            every { this@mockk.toString() } returns "AAAA-1111-BBBB-2222"
        }

        val result = UUID.randomUUID().compact()

        assertEquals("aaaa1111bbbb2222", result)
    }
}
