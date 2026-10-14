// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api

import android.content.Context
import dev.mobilejoe.api.identity.Identity
import dev.mobilejoe.api.network.JoeNetworkClient
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JoeTest {

    @Test
    fun `Test configure delegates to network client`() {
        val context = mockk<Context>()
        mockkObject(JoeNetworkClient)
        justRun { JoeNetworkClient.configure(any(), any(), any(), any()) }

        Joe.configure(context, apiKey = "j_o_e", appUserId = "user-42", isDebugMode = true)

        verify {
            JoeNetworkClient.configure(
                applicationContext = context,
                apiKey = "j_o_e",
                externalId = "user-42",
                isDebugMode = true,
            )
        }

        unmockkAll()
    }

    @Test
    fun `Test identify delegates to network client`() {
        mockkObject(JoeNetworkClient)
        justRun { JoeNetworkClient.identify(any()) }

        Joe.identify("user-42")

        verify { JoeNetworkClient.identify(externalId = "user-42") }

        unmockkAll()
    }

    @Test
    fun `Test logout delegates to network client`() {
        mockkObject(JoeNetworkClient)
        justRun { JoeNetworkClient.logout() }

        Joe.logout()

        verify { JoeNetworkClient.logout() }

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
