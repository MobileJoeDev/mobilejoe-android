// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.identity

import dev.mobilejoe.api.identity.util.IdFactory

/**
 * [IdentityGateway] keeping the identity in memory and counting writes. Like the real gateway,
 * [save] writes nothing if the identity is already stored.
 */
internal class InMemoryIdentityGateway(var identity: Identity? = null) : IdentityGateway {
    var saveCount = 0
        private set
    var deleteCount = 0
        private set

    /** Makes [save] fail for every identity this returns `true` for. */
    var failsSaving: (Identity) -> Boolean = { false }

    /** Makes [delete] fail while `true`. */
    var failsDeleting = false

    /** Makes every write in [save] take this long, to widen the window for concurrent calls. */
    var saveDelayMillis = 0L

    override fun find(): Identity? = identity

    override fun save(identity: Identity) {
        if (identity == this.identity) return
        if (failsSaving(identity)) throw JoeStorageException("Save identity IDs failed.")
        if (saveDelayMillis > 0) Thread.sleep(saveDelayMillis)

        saveCount++
        this.identity = identity
    }

    override fun delete() {
        if (failsDeleting) throw JoeStorageException("Delete identity IDs failed.")

        deleteCount++
        identity = null
    }
}

/** [IdFactory] minting predictable ids: `anonymous-1`, `device-1`, `external-<appUserId>`. */
internal class SequentialIdFactory : IdFactory {
    private var anonymousCount = 0
    private var deviceCount = 0

    override fun createAnonymousId() = "anonymous-${++anonymousCount}"

    override fun createExternalId(externalId: String) = "external-$externalId"

    override fun createDeviceId() = "device-${++deviceCount}"
}
