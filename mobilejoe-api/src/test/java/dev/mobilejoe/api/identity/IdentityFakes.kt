// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.identity

import dev.mobilejoe.api.identity.util.IdFactory

/** [IdentityGateway] keeping the identity in memory and counting writes. */
internal class InMemoryIdentityGateway(var identity: Identity? = null) : IdentityGateway {
    var saveCount = 0
        private set
    var deleteCount = 0
        private set

    override fun find(): Identity? = identity

    override fun save(identity: Identity) {
        saveCount++
        this.identity = identity
    }

    override fun delete() {
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
