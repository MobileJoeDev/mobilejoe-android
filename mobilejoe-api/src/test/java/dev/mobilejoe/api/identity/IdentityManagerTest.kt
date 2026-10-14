// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.identity

import dev.mobilejoe.api.identity.util.IdFactory
import dev.mobilejoe.api.runConcurrently
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class IdentityManagerTest {

    private val identityGateway = InMemoryIdentityGateway()
    private val identityManager = IdentityManager(identityGateway, SequentialIdFactory())

    @Test
    fun `Test identify without persisted identity creates and persists a new one`() {
        val identity = identityManager.identify("A")

        val expectedIdentity = Identity("anonymous-1", "device-1", "external-A")
        assertIdentified(expectedIdentity, identity)
    }

    @Test
    fun `Test first login keeps anonymous ID`() {
        identityGateway.identity = Identity("anonymous-X", "device-X")

        val identity = identityManager.identify("A")

        val expectedIdentity = Identity("anonymous-X", "device-X", "external-A")
        assertIdentified(expectedIdentity, identity)
    }

    @Test
    fun `Test persisted app user ID is returned once without writing`() {
        val storedIdentity = Identity("anonymous-X", "device-X", "external-A")
        identityGateway.identity = storedIdentity

        val identity = identityManager.identify("A")

        assertEquals(storedIdentity, identity)
        assertEquals(storedIdentity, identityManager.currentIdentity)
        assertNull(identityManager.identify("A"))
        assertEquals(0, identityGateway.saveCount)
        assertEquals(0, identityGateway.deleteCount)
    }

    @Test
    fun `Test anonymous identity stays unchanged without app user ID`() {
        val storedIdentity = Identity("anonymous-X", "device-X")
        identityGateway.identity = storedIdentity

        val identity = identityManager.identify(null)

        assertEquals(storedIdentity, identity)
        assertEquals(storedIdentity, identityManager.currentIdentity)
        assertEquals(0, identityGateway.saveCount)
        assertEquals(0, identityGateway.deleteCount)
    }

    @Test
    fun `Test switching app user ID logs out first without writing the intermediate identity`() {
        identityGateway.identity = Identity("anonymous-X", "device-X", "external-A")

        val identity = identityManager.identify("B")

        val expectedIdentity = Identity("anonymous-1", "device-1", "external-B")
        assertIdentified(expectedIdentity, identity)
        assertEquals(1, identityGateway.deleteCount)
        assertEquals(1, identityGateway.saveCount)
    }

    @Test
    fun `Test identify without app user ID keeps identified app user`() {
        val storedIdentity = Identity("anonymous-X", "device-X", "external-A")
        identityGateway.identity = storedIdentity

        val identity = identityManager.identify(null)

        assertIdentified(storedIdentity, identity)
        assertEquals(0, identityGateway.saveCount)
        assertEquals(0, identityGateway.deleteCount)

        assertNull(identityManager.identify("A"))

        assertEquals(storedIdentity, identityManager.currentIdentity)
        assertEquals(0, identityGateway.deleteCount)
    }

    @Test
    fun `Test identify without app user ID then different app user ID logs out`() {
        identityGateway.identity = Identity("anonymous-X", "device-X", "external-A")
        identityManager.identify(null)

        val identity = identityManager.identify("B")

        val expectedIdentity = Identity("anonymous-1", "device-1", "external-B")
        assertIdentified(expectedIdentity, identity)
        assertEquals(1, identityGateway.deleteCount)
    }

    @Test
    fun `Test identifying after logout keeps the new anonymous ID`() {
        identityGateway.identity = Identity("anonymous-X", "device-X", "external-A")
        identityManager.logout()

        val identity = identityManager.identify("B")

        val expectedIdentity = Identity("anonymous-1", "device-1", "external-B")
        assertIdentified(expectedIdentity, identity)
        assertEquals(1, identityGateway.deleteCount)
    }

    @Test
    fun `Test identifyAtStart returns the current identity when nothing changed`() {
        val anonymousIdentity = identityManager.logout()

        val identity = identityManager.identifyAtStart(null)

        assertEquals(anonymousIdentity, identity)
        assertEquals(1, identityGateway.saveCount)
    }

    @Test
    fun `Test identifyAtStart is rejecting empty app user ID`() {
        val error = assertFailsWith<IllegalArgumentException> {
            identityManager.identifyAtStart("")
        }

        assertEquals("The app user ID must not be empty. Use a stable account ID.", error.message)
        assertEquals(0, identityGateway.saveCount)
        assertEquals(0, identityGateway.deleteCount)
    }

    @Test
    fun `Test identifyAtStart is rejecting blank app user ID`() {
        val error = assertFailsWith<IllegalArgumentException> {
            identityManager.identifyAtStart("   ")
        }

        assertEquals("The app user ID must not be empty. Use a stable account ID.", error.message)
        assertEquals(0, identityGateway.saveCount)
        assertEquals(0, identityGateway.deleteCount)
    }

    @Test
    fun `Test isCurrent only for the identity in effect`() {
        val firstIdentity = identityManager.identifyAtStart("A")
        assertTrue(identityManager.isCurrent(firstIdentity))

        val anonymousIdentity = identityManager.logout()

        assertFalse(identityManager.isCurrent(firstIdentity))
        assertTrue(identityManager.isCurrent(anonymousIdentity))
    }

    @Test
    fun `Test failed switch drops the previous IDs and the next identify completes it`() {
        identityGateway.identity = Identity("anonymous-X", "device-X", "external-A")
        identityGateway.failsSaving = { true }

        assertFailsWith<JoeStorageException> { identityManager.identify("B") }

        assertNull(identityManager.currentIdentity)
        assertNull(identityGateway.identity)
        assertEquals(1, identityGateway.deleteCount)

        identityGateway.failsSaving = { false }
        val identity = identityManager.identify("B")

        val expectedIdentity = Identity("anonymous-2", "device-2", "external-B")
        assertIdentified(expectedIdentity, identity)
    }

    @Test
    fun `Test failed first login keeps the anonymous identity`() {
        val storedIdentity = Identity("anonymous-X", "device-X")
        identityGateway.identity = storedIdentity
        identityManager.identify(null)
        identityGateway.failsSaving = { true }

        assertFailsWith<JoeStorageException> { identityManager.identify("A") }

        assertEquals(storedIdentity, identityManager.currentIdentity)
        assertEquals(storedIdentity, identityGateway.identity)
        assertEquals(0, identityGateway.saveCount)
    }

    @Test
    fun `Test failed identify without current identity keeps it unset`() {
        val storedIdentity = Identity("anonymous-X", "device-X")
        identityGateway.identity = storedIdentity
        identityGateway.failsSaving = { true }

        assertFailsWith<JoeStorageException> { identityManager.identify("A") }

        assertNull(identityManager.currentIdentity)
        assertEquals(storedIdentity, identityGateway.identity)
        assertEquals(0, identityGateway.saveCount)
    }

    @Test
    fun `Test empty app user ID is rejected`() {
        assertAppUserIdRejected("")
    }

    @Test
    fun `Test app user ID with only a space is rejected`() {
        assertAppUserIdRejected(" ")
    }

    @Test
    fun `Test app user ID with only whitespace characters is rejected`() {
        assertAppUserIdRejected("\t\n")
    }

    private fun assertAppUserIdRejected(appUserId: String) {
        val storedIdentity = Identity("anonymous-X", "device-X", "external-A")
        identityGateway.identity = storedIdentity
        val idFactory = mockk<IdFactory>()
        val identityManager = IdentityManager(identityGateway, idFactory)

        val error = assertFailsWith<IllegalArgumentException> { identityManager.identify(appUserId) }

        assertEquals("The app user ID must not be empty. Use a stable account ID.", error.message)
        verify(exactly = 0) { idFactory.createExternalId(any()) }
        assertNull(identityManager.currentIdentity)
        assertEquals(storedIdentity, identityGateway.identity)
        assertEquals(0, identityGateway.deleteCount)
    }

    @Test
    fun `Test logout deletes existing identity and creates a new anonymous one`() {
        identityGateway.identity = Identity("anonymous-X", "device-X", "external-A")

        val identity = identityManager.logout()

        val expectedIdentity = Identity("anonymous-1", "device-1")
        assertIdentified(expectedIdentity, identity)
        assertEquals(1, identityGateway.deleteCount)
    }

    @Test
    fun `Test failed logout leaves no current identity and the next identify completes it`() {
        identityGateway.identity = Identity("anonymous-X", "device-X", "external-A")
        identityManager.identify(null)
        identityGateway.failsDeleting = true

        assertFailsWith<JoeStorageException> { identityManager.logout() }

        assertNull(identityManager.currentIdentity)

        identityGateway.failsDeleting = false
        val identity = identityManager.identify("A")

        val expectedIdentity = Identity("anonymous-1", "device-1", "external-A")
        assertIdentified(expectedIdentity, identity)
        assertEquals(1, identityGateway.deleteCount)
    }

    @Test
    fun `Test logout that fails saving does not bring the previous IDs back`() {
        identityGateway.identity = Identity("anonymous-X", "device-X", "external-A")
        identityGateway.failsSaving = { true }

        assertFailsWith<JoeStorageException> { identityManager.logout() }

        assertNull(identityManager.currentIdentity)
        assertNull(identityGateway.identity)
    }

    @Test
    fun `Test concurrent identify calls for the same app user ID return one identity to send`() {
        identityGateway.identity = Identity("anonymous-X", "device-X")
        identityGateway.saveDelayMillis = 20

        val identities = runConcurrently(threads = 8) { identityManager.identify("A") }

        val expectedIdentity = Identity("anonymous-X", "device-X", "external-A")
        assertEquals(listOf(expectedIdentity), identities.filterNotNull())
        assertEquals(expectedIdentity, identityManager.currentIdentity)
        assertEquals(1, identityGateway.saveCount)
    }

    @Test
    fun `Test concurrent logout calls leave the last identity persisted`() {
        identityGateway.identity = Identity("anonymous-X", "device-X", "external-A")
        identityGateway.saveDelayMillis = 5

        val identities = runConcurrently(threads = 8) { identityManager.logout() }

        assertEquals(8, identities.toSet().size)
        assertEquals(identityManager.currentIdentity, identityGateway.identity)
        assertTrue(identityManager.currentIdentity in identities)
        assertEquals(8, identityGateway.saveCount)
    }

    @Test
    fun `Test readers never see a missing identity while the app user ID switches`() {
        identityManager.identify("A")
        identityGateway.saveDelayMillis = 1
        val isSwitching = AtomicBoolean(true)
        val writer = thread {
            try {
                repeat(100) { round -> identityManager.identify(if (round % 2 == 0) "B" else "A") }
            } finally {
                isSwitching.set(false)
            }
        }

        var missingCount = 0
        while (isSwitching.get()) {
            if (identityManager.currentIdentity == null) missingCount++
        }
        writer.join()

        assertEquals(0, missingCount)
    }

    /** Asserts [expected] is the [returned], the current, and the persisted identity. */
    private fun assertIdentified(expected: Identity, returned: Identity?) {
        assertEquals(expected, returned)
        assertEquals(expected, identityManager.currentIdentity)
        assertEquals(expected, identityGateway.identity)
    }
}
