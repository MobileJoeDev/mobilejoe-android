// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.identity

/**
 * Persists the current [Identity] across app launches.
 *
 * [IdentityManager] is the sole owner of this interface: it calls [find] once on startup to
 * restore whatever identity was previously established, and [save]/[delete] whenever that
 * identity changes. See [PreferencesIdentityGateway] for the default implementation.
 */
interface IdentityGateway {

    /**
     * Reads back a previously [save]d identity.
     *
     * @return The stored [Identity], or `null` if none has been saved yet (or it was [delete]d).
     */
    fun find(): Identity?

    /**
     * Persists [identity], overwriting whatever was previously stored.
     *
     * @param identity The identity to persist.
     * @throws IllegalStateException if the write could not be committed.
     */
    fun save(identity: Identity)

    /**
     * Removes any previously persisted identity.
     *
     * @throws IllegalStateException if the write could not be committed.
     */
    fun delete()
}
