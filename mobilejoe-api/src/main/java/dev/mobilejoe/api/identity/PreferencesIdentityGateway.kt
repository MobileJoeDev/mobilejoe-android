// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.identity

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * [IdentityGateway] backed by Android [SharedPreferences].
 *
 * Each [Identity] field is stored under its own preference key ([KEY_ANONYMOUS_ID],
 * [KEY_EXTERNAL_ID], [KEY_DEVICE_ID]) rather than as a single serialized blob, so [find] can
 * detect "nothing saved yet" simply by checking whether the required keys are present.
 *
 * @param sharedPreferences The preferences store used to persist identity fields.
 */
internal class PreferencesIdentityGateway(
    private val sharedPreferences: SharedPreferences
) : IdentityGateway {

    /**
     * Convenience constructor that opens this SDK's private [SharedPreferences] file for
     * [context].
     *
     * @param context The context used to open the preferences file.
     * @throws JoeStorageException if the preferences file can't be opened, e.g. in a direct-boot
     * aware app before the user has unlocked the device.
     */
    constructor(context: Context) : this(openSharedPreferences(context))

    /**
     * Reconstructs an [Identity] from whatever's currently stored under [sharedPreferences].
     *
     * @return The stored [Identity] reconstructed from its individual preference keys, or `null`
     * if [KEY_ANONYMOUS_ID] or [KEY_DEVICE_ID] is missing (i.e. nothing has been [save]d yet, or
     * the stored identity is incomplete and the next [save] replaces it).
     */
    override fun find(): Identity? {
        return sharedPreferences.getString(KEY_ANONYMOUS_ID, null)?.let { anonymousId ->
            sharedPreferences.getString(KEY_DEVICE_ID, null)?.let { deviceId ->
                val externalId = sharedPreferences.getString(KEY_EXTERNAL_ID, null)
                Identity(
                    anonymousId = anonymousId,
                    externalId = externalId,
                    deviceId = deviceId,
                )
            }
        }
    }

    /**
     * Writes [identity]'s fields to their individual preference keys, removing
     * [KEY_EXTERNAL_ID] if [Identity.externalId] is `null`. Writes nothing if [identity] is
     * already stored.
     *
     * @param identity The identity to persist.
     * @throws JoeStorageException if [SharedPreferences.Editor.commit] returns `false`.
     */
    override fun save(identity: Identity) {
        if (identity == find()) {
            return
        }

        sharedPreferences.edit().apply {
            putString(KEY_ANONYMOUS_ID, identity.anonymousId)
            putString(KEY_DEVICE_ID, identity.deviceId)

            identity.externalId?.let { externalId ->
                putString(KEY_EXTERNAL_ID, externalId)
            } ?: remove(KEY_EXTERNAL_ID)

            if (!commit()) {
                throw JoeStorageException("Save identity IDs failed.")
            }
        }
    }

    /**
     * Removes all identity-related preference keys.
     *
     * @throws JoeStorageException if [SharedPreferences.Editor.commit] returns `false`.
     */
    override fun delete() {
        sharedPreferences.edit().apply {
            remove(KEY_ANONYMOUS_ID)
            remove(KEY_EXTERNAL_ID)
            remove(KEY_DEVICE_ID)

            if (!commit()) {
                throw JoeStorageException("Delete identity IDs failed.")
            }
        }
    }

    companion object {

        /** Preference key for [Identity.anonymousId]. */
        private const val KEY_ANONYMOUS_ID = "anonymous_id"

        /** Preference key for [Identity.externalId]. */
        private const val KEY_EXTERNAL_ID = "external_id"

        /** Preference key for [Identity.deviceId]. */
        private const val KEY_DEVICE_ID = "device_id"

        /**
         * Opens this SDK's private [SharedPreferences] file for [context].
         *
         * @param context The context used to open the preferences file.
         * @throws JoeStorageException if the preferences file can't be opened.
         */
        private fun openSharedPreferences(context: Context): SharedPreferences {
            return try {
                context.getSharedPreferences("dev.mobilejoe.identity", Context.MODE_PRIVATE)
            } catch (e: IllegalStateException) {
                throw JoeStorageException("Open identity preferences failed.", e)
            }
        }
    }
}
