// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.identity

import dev.mobilejoe.api.identity.util.IdFactory

/**
 * Owns the SDK's current [Identity]: creating it, persisting it via [identityGateway], and
 * keeping it in sync with the host app's own user id.
 *
 * [JoeNetworkClient][dev.mobilejoe.api.network.JoeNetworkClient] is the sole owner of an
 * [IdentityManager] instance. Every identity change is local and synchronous: [identify] and
 * [logout] switch to and persist the target identity right away and return it, so the caller can
 * send it to the backend afterwards. There is no confirmation step; a backend response never
 * changes the identity.
 *
 * All members are safe to call from any thread.
 *
 * @param identityGateway The store used to persist/restore the identity across launches.
 * @param idFactory The factory used to mint new anonymous/external/device ids.
 */
internal class IdentityManager(
    private val identityGateway: IdentityGateway,
    private val idFactory: IdFactory,
) {

    /** Where the identity stands in this process; see [state]. */
    private sealed interface State {

        /** No identity has been loaded or identified in this process yet. */
        data object Unknown : State

        /** [identity] is persisted and used for requests. */
        data class Identified(val identity: Identity) : State

        /** A logout is in progress or failed partway. The next [identify] completes it first. */
        data object LoggingOut : State
    }

    /** Guarded by this instance's monitor, like every other member. */
    private var state: State = State.Unknown

    /** The identity currently in effect, or `null` before one has been established. */
    val currentIdentity: Identity?
        @Synchronized get() = when (val state = state) {
            is State.Identified -> state.identity
            State.LoggingOut,
            State.Unknown -> null
        }

    /**
     * Switches to the identity for [appUserId] and persists it.
     *
     * - No persisted identity: a brand-new identity is created.
     * - [appUserId] is `null`: the persisted identity is kept, including its external id. Only an
     *   explicit [logout] returns to an anonymous identity.
     * - The identity is anonymous: its anonymous id is kept and [appUserId] is added, so anonymous
     *   activity carries over to the app user.
     * - The identity belongs to another app user id: it is deleted first and replaced by fresh
     *   ids, so the previous anonymous id is never linked to another app user. If persisting
     *   fails then, there is no [currentIdentity] until the next [identify] or [logout].
     *
     * @param appUserId The host app's own user identifier, or `null` to keep the persisted
     * identity.
     * @return The identity to send to the backend, or `null` if [currentIdentity] already belongs
     * to [appUserId] and nothing changed.
     * @throws IllegalArgumentException if [appUserId] is empty or blank.
     * @throws JoeStorageException if the identity could not be persisted.
     */
    @Synchronized
    fun identify(appUserId: String?): Identity? {
        requireValidAppUserId(appUserId)

        val externalId = appUserId?.let { id -> idFactory.createExternalId(externalId = id) }
        return prepareIdentity(externalId)
            ?.also { identity ->
                identityGateway.save(identity)
                state = State.Identified(identity)
            }
    }

    /**
     * [identify] for the first call of a process, where the identity is sent even if it didn't
     * change.
     *
     * @param appUserId The host app's own user identifier, or `null` to keep the persisted
     * identity.
     * @return The identity now in effect.
     * @throws IllegalArgumentException if [appUserId] is empty or blank.
     * @throws JoeStorageException if the identity could not be persisted.
     */
    @Synchronized
    fun identifyAtStart(appUserId: String?): Identity {
        return identify(appUserId) ?: checkNotNull(currentIdentity)
    }

    /**
     * Whether [identity] is still the one in effect, i.e. no later [identify] or [logout]
     * replaced it.
     *
     * @param identity The identity to check.
     */
    @Synchronized
    fun isCurrent(identity: Identity): Boolean {
        return identity == currentIdentity
    }

    /**
     * Deletes the persisted identity and switches to a fresh anonymous one with new anonymous and
     * device ids.
     *
     * Deletes before saving: if saving fails and the process dies, the previous ids must not come
     * back after a restart.
     *
     * @return The new anonymous identity.
     * @throws JoeStorageException if the identity could not be deleted or persisted.
     */
    @Synchronized
    fun logout(): Identity {
        val identity = beginLogout()
        identityGateway.save(identity)
        state = State.Identified(identity)
        return identity
    }

    /**
     * Deletes the persisted identity and creates a fresh anonymous one, without persisting it.
     * The logout stays pending until the caller has persisted the result or an identity derived
     * from it.
     *
     * @return The new anonymous identity.
     * @throws JoeStorageException if the identity could not be deleted.
     */
    private fun beginLogout(): Identity {
        state = State.LoggingOut
        identityGateway.delete()
        return newIdentity()
    }

    /**
     * Resolves the identity for [externalId] without persisting it.
     *
     * @param externalId The already hashed external id, or `null` to keep the identity as is.
     * @return The identity to switch to, or `null` if [currentIdentity] already matches.
     */
    private fun prepareIdentity(externalId: String?): Identity? {
        if (isIdentifiedAs(externalId)) {
            return null
        }
        completePendingLogout()

        val identity = findOrCreateIdentity()
        return if (externalId == null) identity else assign(externalId, identity)
    }

    /**
     * Whether [currentIdentity] already covers [externalId].
     *
     * @param externalId The already hashed external id, or `null`, which any identity covers.
     */
    private fun isIdentifiedAs(externalId: String?): Boolean {
        return currentIdentity?.let { identity ->
            externalId == null || identity.belongsTo(externalId)
        } ?: false
    }

    /** Finishes a [logout] that failed partway, before any persisted identity is reused. */
    private fun completePendingLogout() {
        when (state) {
            State.LoggingOut ->
                logout()

            State.Unknown,
            is State.Identified ->
                Unit
        }
    }

    /** Returns [currentIdentity], else the persisted identity, else a brand-new one. */
    private fun findOrCreateIdentity(): Identity {
        return currentIdentity ?: identityGateway.find() ?: newIdentity()
    }

    /**
     * Adds [externalId] to [identity], logging out first if [identity] already belongs to another
     * app user id.
     *
     * Only begins that logout: the caller persists the result, which saves writing the
     * intermediate anonymous identity to disk.
     *
     * @param externalId The already hashed external id.
     * @param identity The identity to assign [externalId] to.
     * @return The identity carrying [externalId].
     */
    private fun assign(externalId: String, identity: Identity): Identity {
        val belongsToOther = !identity.isAnonymous && !identity.belongsTo(externalId)
        val target = if (belongsToOther) beginLogout() else identity
        return target.copy(externalId = externalId)
    }

    /** Builds a brand-new anonymous [Identity] with fresh ids, without persisting it. */
    private fun newIdentity(): Identity {
        return Identity(
            anonymousId = idFactory.createAnonymousId(),
            deviceId = idFactory.createDeviceId(),
        )
    }

    /**
     * Rejects an empty or blank [appUserId] before any identity state changes.
     *
     * @param appUserId The host app's own user identifier, or `null`, which is always valid.
     * @throws IllegalArgumentException if [appUserId] is empty or blank.
     */
    private fun requireValidAppUserId(appUserId: String?) {
        require(appUserId == null || appUserId.isNotBlank()) {
            "The app user ID must not be empty. Use a stable account ID."
        }
    }
}
