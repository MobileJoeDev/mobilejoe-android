// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.identity

import org.junit.Test
import kotlin.test.assertEquals

class IdentityTest {

    @Test
    fun `Test id result with anonymous only`() {
        val identity = Identity(
            anonymousId = "anonymous-42",
            deviceId = "device-42"
        )

        assertEquals("anonymous-42", identity.id)
    }

    @Test
    fun `Test id result with external and anonymous`() {
        val identity = Identity(
            anonymousId = "anonymous-42",
            deviceId = "device-42",
            externalId = "external-42"
        )

        assertEquals("external-42", identity.id)
    }

    @Test
    fun `Test toString result with anonymous only`() {
        val identity = Identity(
            anonymousId = "anonymous-42",
            deviceId = "device-42"
        )

        assertEquals("anonymous-42", identity.toString())
    }

    @Test
    fun `Test toString result with external and anonymous`() {
        val identity = Identity(
            anonymousId = "anonymous-42",
            deviceId = "device-42",
            externalId = "external-42"
        )

        assertEquals("external-42", identity.toString())
    }
}
