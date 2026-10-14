// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.identity

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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

    @Test
    fun `Test isAnonymous without external ID`() {
        assertTrue(Identity(anonymousId = "anonymous-42", deviceId = "device-42").isAnonymous)
    }

    @Test
    fun `Test isAnonymous with external ID`() {
        val identity = Identity(
            anonymousId = "anonymous-42",
            deviceId = "device-42",
            externalId = "external-42"
        )

        assertFalse(identity.isAnonymous)
    }

    @Test
    fun `Test belongsTo only its own external ID`() {
        val identity = Identity(
            anonymousId = "anonymous-42",
            deviceId = "device-42",
            externalId = "external-42"
        )

        assertTrue(identity.belongsTo("external-42"))
        assertFalse(identity.belongsTo("external-43"))
    }

    @Test
    fun `Test anonymous identity belongs to no external ID`() {
        val identity = Identity(anonymousId = "anonymous-42", deviceId = "device-42")

        assertFalse(identity.belongsTo("external-42"))
    }
}
