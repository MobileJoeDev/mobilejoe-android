// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.identity.util

import java.util.UUID

/**
 * Default [IdFactory] implementation used by the SDK.
 *
 * Every id it mints is a random [UUID][java.util.UUID], [compact]ed to a dash-free lowercase hex
 * string, and prefixed so the id's kind is recognizable from the string alone (e.g. when
 * inspecting request headers or persisted preferences).
 */
internal object JoeIdFactory : IdFactory {

    /**
     * Generates a fresh, prefixed, random anonymous id.
     *
     * @return A `$MBJAnonymousID:`-prefixed, [UUID]-derived anonymous id.
     */
    override fun createAnonymousId(): String {
        val id = UUID.randomUUID().compact()
        return "\$MBJAnonymousID:$id"
    }

    /**
     * Derives a prefixed, hashed external id from the host app's own user id, so the original
     * value is never persisted or transmitted in the clear.
     *
     * @param externalId The host app's own (plaintext) user identifier.
     * @return A `$MBJExternalID:`-prefixed id built from the first 32 characters of
     * [externalId]'s SHA-256 hash.
     */
    override fun createExternalId(externalId: String): String {
        val externalId = externalId.sha265Hash().take(32)
        return "\$MBJExternalID:$externalId"
    }

    /**
     * Generates a fresh, prefixed, random device id.
     *
     * @return A `$MBJDeviceID:`-prefixed, [UUID]-derived device id.
     */
    override fun createDeviceId(): String {
        val id = UUID.randomUUID().compact()
        return "\$MBJDeviceID:$id"
    }
}
