// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.identity

import dev.mobilejoe.api.identity.util.IdFactory
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class IdentityManagerTest {

    private val identityGateway = InMemoryIdentityGateway()
    private val identityManager = IdentityManager(identityGateway, SequentialIdFactory())

    @Test
    fun `Test prepareIdentity without persisted identity creates a new one`() {
        val identity = identityManager.prepareIdentity("A")

        assertEquals(Identity("anonymous-1", "device-1", "external-A"), identity)
        assertNull(identityManager.currentIdentity)
        assertNull(identityGateway.identity)
    }

    @Test
    fun `Test commit persists identity and sets it as current`() {
        val identity = identityManager.prepareIdentity("A")

        identityManager.commit(identity)

        assertEquals(identity, identityManager.currentIdentity)
        assertEquals(identity, identityGateway.identity)
    }

    @Test
    fun `Test first login keeps anonymous ID`() {
        identityGateway.identity = Identity("anonymous-X", "device-X")

        val identity = identityManager.prepareIdentity("A")
        identityManager.commit(identity)

        val expectedIdentity = Identity("anonymous-X", "device-X", "external-A")
        assertEquals(expectedIdentity, identityManager.currentIdentity)
        assertEquals(expectedIdentity, identityGateway.identity)
    }

    @Test
    fun `Test same external ID again leaves identity unchanged`() {
        val storedIdentity = Identity("anonymous-X", "device-X", "external-A")
        identityGateway.identity = storedIdentity

        val identity = identityManager.prepareIdentity("A")
        identityManager.commit(identity)

        assertEquals(storedIdentity, identityManager.currentIdentity)
        assertEquals(0, identityGateway.saveCount)
        assertEquals(0, identityGateway.deleteCount)
    }

    @Test
    fun `Test anonymous identity stays unchanged without external ID`() {
        val storedIdentity = Identity("anonymous-X", "device-X")
        identityGateway.identity = storedIdentity

        val identity = identityManager.prepareIdentity(null)
        identityManager.commit(identity)

        assertEquals(storedIdentity, identityManager.currentIdentity)
        assertEquals(0, identityGateway.saveCount)
        assertEquals(0, identityGateway.deleteCount)
    }

    @Test
    fun `Test switching external ID logs out before identifying`() {
        identityGateway.identity = Identity("anonymous-X", "device-X", "external-A")

        val identity = identityManager.prepareIdentity("B")

        val anonymousIdentity = Identity("anonymous-1", "device-1")
        assertEquals(anonymousIdentity, identityManager.currentIdentity)
        assertEquals(anonymousIdentity, identityGateway.identity)
        assertEquals(1, identityGateway.deleteCount)

        identityManager.commit(identity)

        val expectedIdentity = Identity("anonymous-1", "device-1", "external-B")
        assertEquals(expectedIdentity, identityManager.currentIdentity)
        assertEquals(expectedIdentity, identityGateway.identity)
    }

    @Test
    fun `Test configure without external ID keeps identified account`() {
        val storedIdentity = Identity("anonymous-X", "device-X", "external-A")
        identityGateway.identity = storedIdentity

        val identity = identityManager.prepareIdentity(null)
        identityManager.commit(identity)

        assertEquals(storedIdentity, identity)
        assertEquals(storedIdentity, identityManager.currentIdentity)
        assertEquals(storedIdentity, identityGateway.identity)
        assertEquals(0, identityGateway.saveCount)
        assertEquals(0, identityGateway.deleteCount)

        identityManager.commit(identityManager.prepareIdentity("A"))

        assertEquals(storedIdentity, identityManager.currentIdentity)
        assertEquals(0, identityGateway.deleteCount)
    }

    @Test
    fun `Test configure without external ID then different account logs out`() {
        identityGateway.identity = Identity("anonymous-X", "device-X", "external-A")
        identityManager.commit(identityManager.prepareIdentity(null))

        val identity = identityManager.prepareIdentity("B")
        identityManager.commit(identity)

        val expectedIdentity = Identity("anonymous-1", "device-1", "external-B")
        assertEquals(expectedIdentity, identityManager.currentIdentity)
        assertEquals(expectedIdentity, identityGateway.identity)
        assertEquals(1, identityGateway.deleteCount)
    }

    @Test
    fun `Test identifying after logout keeps the new anonymous ID`() {
        identityGateway.identity = Identity("anonymous-X", "device-X", "external-A")
        identityManager.logout()

        val identity = identityManager.prepareIdentity("B")
        identityManager.commit(identity)

        val expectedIdentity = Identity("anonymous-1", "device-1", "external-B")
        assertEquals(expectedIdentity, identityManager.currentIdentity)
        assertEquals(expectedIdentity, identityGateway.identity)
        assertEquals(1, identityGateway.deleteCount)
    }

    @Test
    fun `Test failed switch keeps the fresh anonymous identity`() {
        identityGateway.identity = Identity("anonymous-X", "device-X", "external-A")

        // Backend rejects the identity, so it is never committed
        identityManager.prepareIdentity("B")

        val anonymousIdentity = Identity("anonymous-1", "device-1")
        assertEquals(anonymousIdentity, identityManager.currentIdentity)
        assertEquals(anonymousIdentity, identityGateway.identity)
    }

    @Test
    fun `Test failed first login keeps the anonymous identity`() {
        val storedIdentity = Identity("anonymous-X", "device-X")
        identityGateway.identity = storedIdentity
        identityManager.commit(storedIdentity)

        // Backend rejects the identity, so it is never committed
        identityManager.prepareIdentity("A")

        assertEquals(storedIdentity, identityManager.currentIdentity)
        assertEquals(storedIdentity, identityGateway.identity)
        assertEquals(0, identityGateway.saveCount)
    }

    @Test
    fun `Test failed identify without current identity keeps it unset`() {
        val storedIdentity = Identity("anonymous-X", "device-X")
        identityGateway.identity = storedIdentity

        // Backend rejects the identity, so it is never committed
        identityManager.prepareIdentity("A")

        assertNull(identityManager.currentIdentity)
        assertEquals(storedIdentity, identityGateway.identity)
        assertEquals(0, identityGateway.saveCount)
    }

    @Test
    fun `Test empty external ID is rejected`() {
        assertExternalIdRejected("")
    }

    @Test
    fun `Test external ID with only a space is rejected`() {
        assertExternalIdRejected(" ")
    }

    @Test
    fun `Test external ID with only whitespace characters is rejected`() {
        assertExternalIdRejected("\t\n")
    }

    private fun assertExternalIdRejected(appUserId: String) {
        val storedIdentity = Identity("anonymous-X", "device-X", "external-A")
        identityGateway.identity = storedIdentity
        val idFactory = mockk<IdFactory>()
        val identityManager = IdentityManager(identityGateway, idFactory)

        val error = assertFailsWith<IllegalArgumentException> { identityManager.prepareIdentity(appUserId) }

        assertEquals("The app user ID must not be empty. Use a stable account ID.", error.message)
        verify(exactly = 0) { idFactory.createExternalId(any()) }
        assertNull(identityManager.currentIdentity)
        assertEquals(storedIdentity, identityGateway.identity)
        assertEquals(0, identityGateway.deleteCount)
    }

    @Test
    fun `Test reset`() {
        identityGateway.identity = Identity("anonymous-X", "device-X", "external-A")

        identityManager.reset()

        assertNull(identityManager.currentIdentity)
        assertNull(identityGateway.identity)
    }

    @Test
    fun `Test logout deletes existing identity and creates a new anonymous one`() {
        identityGateway.identity = Identity("anonymous-X", "device-X", "external-A")

        val identity = identityManager.logout()

        val expectedIdentity = Identity("anonymous-1", "device-1")
        assertEquals(expectedIdentity, identity)
        assertEquals(expectedIdentity, identityManager.currentIdentity)
        assertEquals(expectedIdentity, identityGateway.identity)
        assertEquals(1, identityGateway.deleteCount)
    }
}
