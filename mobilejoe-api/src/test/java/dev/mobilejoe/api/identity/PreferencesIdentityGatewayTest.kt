// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.identity

import android.content.SharedPreferences
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class PreferencesIdentityGatewayTest {

    @Test
    fun `Test find identity with none existing ID`() {
        val sharedPreferences = mockk<SharedPreferences> {
            every { getString("anonymous_id", null) } returns null
            every { getString("external_id", null) } returns null
            every { getString("device_id", null) } returns null
        }
        val preferencesIdentityGateway = PreferencesIdentityGateway(sharedPreferences)

        val result = preferencesIdentityGateway.find()

        assertNull(result)
    }

    @Test
    fun `Test find identity without external ID`() {
        val sharedPreferences = mockk<SharedPreferences> {
            every { getString("anonymous_id", null) } returns "anonym-42"
            every { getString("device_id", null) } returns "device-42"
            every { getString("external_id", null) } returns null
        }
        val preferencesIdentityGateway = PreferencesIdentityGateway(sharedPreferences)

        val result = preferencesIdentityGateway.find()

        assertEquals(
            Identity(anonymousId = "anonym-42", externalId = null, deviceId = "device-42"),
            result
        )
    }

    @Test
    fun `Test find identity with external ID`() {
        val sharedPreferences = mockk<SharedPreferences> {
            every { getString("anonymous_id", null) } returns "anonym-42"
            every { getString("device_id", null) } returns "device-42"
            every { getString("external_id", null) } returns "external-42"
        }
        val preferencesIdentityGateway = PreferencesIdentityGateway(sharedPreferences)

        val result = preferencesIdentityGateway.find()

        assertEquals(
            Identity(anonymousId = "anonym-42", externalId = "external-42", deviceId = "device-42"),
            result
        )
    }

    @Test
    fun `Test save identity without external ID`() {
        val sharedPreferencesEditor = mockk<SharedPreferences.Editor> {
            every { putString(any(), any()) } returns this
            every { remove(any()) } returns this
            every { commit() } returns true
        }
        val sharedPreferences = mockk<SharedPreferences> {
            every { edit() } returns sharedPreferencesEditor
        }
        val preferencesIdentityGateway = PreferencesIdentityGateway(sharedPreferences)

        preferencesIdentityGateway.save(
            Identity(
                anonymousId = "anonym-42",
                externalId = null,
                deviceId = "device-42"
            )
        )

        verify { sharedPreferences.edit() }
        verify { sharedPreferencesEditor.putString("anonymous_id", "anonym-42") }
        verify { sharedPreferencesEditor.putString("device_id", "device-42") }
        verify { sharedPreferencesEditor.remove("external_id") }
        verify { sharedPreferencesEditor.commit() }
    }

    @Test
    fun `Test save identity with external ID`() {
        val sharedPreferencesEditor = mockk<SharedPreferences.Editor> {
            every { putString(any(), any()) } returns this
            every { commit() } returns true
        }
        val sharedPreferences = mockk<SharedPreferences> {
            every { edit() } returns sharedPreferencesEditor
        }
        val preferencesIdentityGateway = PreferencesIdentityGateway(sharedPreferences)

        preferencesIdentityGateway.save(
            Identity(anonymousId = "anonym-42", externalId = "external-42", deviceId = "device-42")
        )

        verify { sharedPreferences.edit() }
        verify { sharedPreferencesEditor.putString("anonymous_id", "anonym-42") }
        verify { sharedPreferencesEditor.putString("external_id", "external-42") }
        verify { sharedPreferencesEditor.putString("device_id", "device-42") }
        verify { sharedPreferencesEditor.commit() }
    }

    @Test
    fun `Test save identity throws when commit fails`() {
        val sharedPreferencesEditor = mockk<SharedPreferences.Editor> {
            every { putString(any(), any()) } returns this
            every { remove(any()) } returns this
            every { commit() } returns false
        }
        val sharedPreferences = mockk<SharedPreferences> {
            every { edit() } returns sharedPreferencesEditor
        }
        val preferencesIdentityGateway = PreferencesIdentityGateway(sharedPreferences)

        assertFailsWith<IllegalStateException> {
            preferencesIdentityGateway.save(
                Identity(anonymousId = "anonym-42", externalId = null, deviceId = "device-42")
            )
        }
    }

    @Test
    fun `Test delete`() {
        val sharedPreferencesEditor = mockk<SharedPreferences.Editor> {
            every { remove(any()) } returns this
            every { commit() } returns true
        }
        val sharedPreferences = mockk<SharedPreferences> {
            every { edit() } returns sharedPreferencesEditor
        }
        val preferencesIdentityGateway = PreferencesIdentityGateway(sharedPreferences)

        preferencesIdentityGateway.delete()

        verify { sharedPreferences.edit() }
        verify { sharedPreferencesEditor.remove("anonymous_id") }
        verify { sharedPreferencesEditor.remove("external_id") }
        verify { sharedPreferencesEditor.remove("device_id") }
        verify { sharedPreferencesEditor.commit() }
    }

    @Test
    fun `Test delete throws when commit fails`() {
        val sharedPreferencesEditor = mockk<SharedPreferences.Editor> {
            every { remove(any()) } returns this
            every { commit() } returns false
        }
        val sharedPreferences = mockk<SharedPreferences> {
            every { edit() } returns sharedPreferencesEditor
        }
        val preferencesIdentityGateway = PreferencesIdentityGateway(sharedPreferences)

        assertFailsWith<IllegalStateException> {
            preferencesIdentityGateway.delete()
        }
    }
}
