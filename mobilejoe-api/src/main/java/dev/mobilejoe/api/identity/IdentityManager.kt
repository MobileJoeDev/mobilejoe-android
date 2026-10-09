// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.identity

import dev.mobilejoe.api.identity.util.IdFactory

/**
 * Owns the SDK's current [Identity]: creating it, persisting it via [identityGateway], and
 * keeping it in sync with the host app's own user id.
 *
 * [JoeNetworkClient][dev.mobilejoe.api.network.JoeNetworkClient] is the sole owner of an
 * [IdentityManager] instance. Identifying is two-phased: [prepareIdentity] resolves the identity
 * to send to the backend, and [commit] makes it [currentIdentity] once the backend has accepted
 * it, so an account change becomes locally visible only after backend success. [logout]/[reset]
 * clear the identity, with [logout] additionally re-establishing a fresh anonymous one in its
 * place.
 *
 * @param identityGateway The store used to persist/restore the identity across launches.
 * @param idFactory The factory used to mint new anonymous/external/device ids.
 */
internal class IdentityManager(
    private val identityGateway: IdentityGateway,
    private val idFactory: IdFactory,
) {

    /** The identity currently in effect, or `null` before one has been established. */
    var currentIdentity: Identity? = null
        private set

    /**
     * Resolves the identity for [appUserId] without committing it. Send the result to the
     * backend and pass it to [commit] once the backend has accepted it.
     *
     * Only a needed [logout] changes [currentIdentity]. Depending on the persisted identity:
     * - No persisted identity: a brand-new identity is created.
     * - [appUserId] is `null`: the persisted identity is returned unchanged, including its
     *   external id. Only an explicit [logout] returns to an anonymous identity.
     * - Persisted identity has the same external id: it is returned unchanged.
     * - Persisted identity is anonymous only: its anonymous id is kept and [appUserId] is added,
     *   so anonymous activity carries over to the account.
     * - Persisted identity has a different external id: [logout] runs first and takes effect
     *   immediately, so the previous account's anonymous id is never linked to another account.
     *   [appUserId] is then added to the fresh anonymous identity, which stays [currentIdentity]
     *   if the backend rejects the new account.
     *
     * @param appUserId The host app's own user identifier, or `null` to keep the persisted
     * identity.
     * @return The identity to send to the backend and then [commit].
     * @throws IllegalArgumentException if [appUserId] is empty or blank.
     */
    fun prepareIdentity(appUserId: String?): Identity {
        requireValidAppUserId(appUserId)

        val externalId = appUserId?.let { id -> idFactory.createExternalId(externalId = id) }
        val persistedIdentity = identityGateway.find()
            ?: return newIdentity(externalId)

        return when {
            externalId == null -> persistedIdentity
            persistedIdentity.externalId == externalId -> persistedIdentity
            persistedIdentity.externalId == null -> persistedIdentity.copy(externalId = externalId)
            else -> logout().copy(externalId = externalId)
        }
    }

    /**
     * Makes [identity] the [currentIdentity], persisting it if it differs from the persisted one.
     *
     * @param identity The identity returned by [prepareIdentity] and accepted by the backend.
     */
    fun commit(identity: Identity) {
        if (identity != identityGateway.find()) {
            identityGateway.save(identity)
        }
        currentIdentity = identity
    }

    /** Clears [currentIdentity] and deletes the persisted identity without creating a new one. */
    fun reset() {
        currentIdentity = null
        identityGateway.delete()
    }

    /**
     * Clears the persisted identity and immediately establishes a fresh, fully anonymous
     * [currentIdentity] with new anonymous and device ids in its place.
     *
     * @return The new anonymous identity.
     */
    fun logout(): Identity {
        currentIdentity = null
        identityGateway.delete()

        val identity = newIdentity(externalId = null)
        identityGateway.save(identity)
        currentIdentity = identity
        return identity
    }

    /**
     * Builds a brand-new [Identity] with fresh anonymous and device ids, without persisting it.
     *
     * @param externalId The already hashed external id, or `null` for a purely anonymous identity.
     * @return The newly created identity.
     */
    private fun newIdentity(externalId: String?): Identity {
        return Identity(
            anonymousId = idFactory.createAnonymousId(),
            externalId = externalId,
            deviceId = idFactory.createDeviceId()
        )
    }
}

/**
 * Rejects an empty or blank [appUserId] before any identity state changes.
 *
 * @param appUserId The host app's own user identifier, or `null`, which is always valid.
 * @throws IllegalArgumentException if [appUserId] is empty or blank.
 */
internal fun requireValidAppUserId(appUserId: String?) {
    require(appUserId == null || appUserId.isNotBlank()) {
        "The app user ID must not be empty. Use a stable account ID."
    }
}
