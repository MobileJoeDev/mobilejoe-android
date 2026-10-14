// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.identity

import android.content.Context
import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class PreferencesIdentityGatewayTest {

    private val anonymousIdentity = Identity(anonymousId = "anonym-42", deviceId = "device-42")
    private val identifiedIdentity = anonymousIdentity.copy(externalId = "external-42")

    @Test
    fun `Test find identity with none existing ID`() {
        val gateway = gateway(stored = null)

        assertNull(gateway.find())
    }

    @Test
    fun `Test find identity without external ID`() {
        val gateway = gateway(stored = anonymousIdentity)

        assertEquals(anonymousIdentity, gateway.find())
    }

    @Test
    fun `Test find identity with external ID`() {
        val gateway = gateway(stored = identifiedIdentity)

        assertEquals(identifiedIdentity, gateway.find())
    }

    @Test
    fun `Test find treats an incomplete identity as missing`() {
        val sharedPreferences = mockk<SharedPreferences> {
            every { getString("anonymous_id", null) } returns "anonym-42"
            every { getString("device_id", null) } returns null
            every { getString("external_id", null) } returns "external-42"
        }

        val gateway = PreferencesIdentityGateway(sharedPreferences)
        assertNull(gateway.find())
    }

    @Test
    fun `Test save identity without external ID`() {
        val editor = editor()
        val gateway = gateway(stored = null, editor)

        gateway.save(anonymousIdentity)

        verify { editor.putString("anonymous_id", "anonym-42") }
        verify { editor.putString("device_id", "device-42") }
        verify { editor.remove("external_id") }
        verify { editor.commit() }
    }

    @Test
    fun `Test save identity with external ID`() {
        val editor = editor()
        val gateway = gateway(stored = null, editor)

        gateway.save(identifiedIdentity)

        verify { editor.putString("anonymous_id", "anonym-42") }
        verify { editor.putString("external_id", "external-42") }
        verify { editor.putString("device_id", "device-42") }
        verify(exactly = 0) { editor.remove(any()) }
        verify { editor.commit() }
    }

    @Test
    fun `Test save identity throws when commit fails`() {
        val editor = editor(commits = false)
        val gateway = gateway(stored = null, editor)

        assertFailsWith<JoeStorageException> { gateway.save(anonymousIdentity) }
    }

    @Test
    fun `Test save writes nothing when the identity is already stored`() {
        val sharedPreferences = sharedPreferences(stored = identifiedIdentity)

        PreferencesIdentityGateway(sharedPreferences).save(identifiedIdentity)

        verify(exactly = 0) { sharedPreferences.edit() }
    }

    @Test
    fun `Test opening locked preferences throws JoeStorageException`() {
        val context = mockk<Context> {
            every { getSharedPreferences(any(), any()) } throws IllegalStateException("locked")
        }

        assertFailsWith<JoeStorageException> { PreferencesIdentityGateway(context) }
    }

    @Test
    fun `Test delete`() {
        val editor = editor()
        val gateway = gateway(stored = identifiedIdentity, editor)

        gateway.delete()

        verify { editor.remove("anonymous_id") }
        verify { editor.remove("external_id") }
        verify { editor.remove("device_id") }
        verify(exactly = 0) { editor.putString(any(), any()) }
        verify { editor.commit() }
    }

    @Test
    fun `Test delete throws when commit fails`() {
        val editor = editor(commits = false)
        val gateway = gateway(stored = identifiedIdentity, editor)

        assertFailsWith<JoeStorageException> { gateway.delete() }
    }

    /** A gateway on top of [sharedPreferences] for [stored] and [editor]. */
    private fun gateway(stored: Identity?, editor: SharedPreferences.Editor? = null) =
        PreferencesIdentityGateway(sharedPreferences(stored, editor))

    /**
     * Preferences holding [stored], or nothing. Writes go to [editor]; without one, opening an
     * editor fails the test.
     */
    private fun sharedPreferences(
        stored: Identity?,
        editor: SharedPreferences.Editor? = null,
    ): SharedPreferences = mockk {
        every { getString("anonymous_id", null) } returns stored?.anonymousId
        every { getString("device_id", null) } returns stored?.deviceId
        every { getString("external_id", null) } returns stored?.externalId
        if (editor != null) every { edit() } returns editor
    }

    /** An editor that accepts every change and whose commit returns [commits]. */
    private fun editor(commits: Boolean = true): SharedPreferences.Editor = mockk {
        every { putString(any(), any()) } returns this@mockk
        every { remove(any()) } returns this@mockk
        every { commit() } returns commits
    }
}
