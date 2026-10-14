// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.identity

/**
 * Persists the current [Identity] across app launches.
 *
 * [IdentityManager] is the sole owner of this interface: it calls [find] to restore whatever
 * identity was previously established, and [save]/[delete] whenever that identity changes.
 * [find] can be called several times per process, and every call may arrive on the main thread,
 * so implementations must be fast and free of side effects. See [PreferencesIdentityGateway] for the default implementation.
 */
interface IdentityGateway {

    /**
     * Reads back a previously [save]d identity.
     *
     * @return The stored [Identity], or `null` if none has been saved yet (or it was [delete]d).
     */
    fun find(): Identity?

    /**
     * Persists [identity], overwriting whatever was previously stored. Implementations should
     * skip the write if [identity] is already stored.
     *
     * @param identity The identity to persist.
     * @throws JoeStorageException if the write could not be committed.
     */
    fun save(identity: Identity)

    /**
     * Removes any previously persisted identity.
     *
     * @throws JoeStorageException if the write could not be committed.
     */
    fun delete()
}
