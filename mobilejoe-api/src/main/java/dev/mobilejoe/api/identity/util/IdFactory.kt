// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.identity.util

/**
 * Mints the identifier strings used to build an
 * [Identity][dev.mobilejoe.api.identity.Identity].
 *
 * [IdentityManager][dev.mobilejoe.api.identity.IdentityManager] calls [createAnonymousId] and
 * [createDeviceId] when establishing a brand-new identity, and [createExternalId] whenever the
 * host app supplies its own user id. See [JoeIdFactory] for the default implementation.
 */
interface IdFactory {

    /**
     * Creates a new, random anonymous identifier.
     *
     * @return A freshly generated anonymous id string.
     */
    fun createAnonymousId(): String

    /**
     * Derives an external identifier from the host app's own user id, suitable for
     * persisting/transmitting without exposing the original value.
     *
     * @param externalId The host app's own (plaintext) user identifier.
     * @return A hashed, prefixed external id string derived from [externalId].
     */
    fun createExternalId(externalId: String): String

    /**
     * Creates a new, random device identifier.
     *
     * @return A freshly generated device id string.
     */
    fun createDeviceId(): String
}
