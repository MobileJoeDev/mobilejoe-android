// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api

import dev.mobilejoe.api.identity.Identity
import dev.mobilejoe.api.network.JoeNetworkClient
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JoeTest {

    @Test
    fun `Test identify delegates to network client`() = runTest {
        mockkObject(JoeNetworkClient)
        coEvery { JoeNetworkClient.identify(any()) } returns Unit

        Joe.identify("user-42")

        coVerify { JoeNetworkClient.identify(externalId = "user-42") }

        unmockkAll()
    }

    @Test
    fun `Test logout delegates to network client`() = runTest {
        mockkObject(JoeNetworkClient)
        coEvery { JoeNetworkClient.logout() } returns Unit

        Joe.logout()

        coVerify { JoeNetworkClient.logout() }

        unmockkAll()
    }

    @Test
    fun `Test identity returns current identity from network client`() {
        val identity = Identity(anonymousId = "anonym-42", deviceId = "device-42")

        mockkObject(JoeNetworkClient)
        every { JoeNetworkClient.currentIdentity } returns identity

        assertEquals(identity, Joe.identity)

        unmockkAll()
    }

    @Test
    fun `Test identity returns null when network client has none`() {
        mockkObject(JoeNetworkClient)
        every { JoeNetworkClient.currentIdentity } returns null

        assertNull(Joe.identity)

        unmockkAll()
    }
}
