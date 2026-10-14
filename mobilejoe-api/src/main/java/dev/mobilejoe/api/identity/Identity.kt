// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.identity

/**
 * The set of identifiers used to attribute API requests to a particular end user/device.
 *
 * An [Identity] always carries an [anonymousId] and a [deviceId], generated locally the first
 * time the SDK is configured on a device, and persisted across app launches by
 * [IdentityGateway]. Once the host app identifies with its own user id for the first time, an
 * [externalId] is added on top without discarding the original [anonymousId]. Switching to a
 * different user id logs out first, so the new app user gets fresh ids instead. [id] is the single
 * property callers/requests should use to refer to "the current user", since it transparently
 * prefers [externalId] once one exists.
 *
 * @param anonymousId A stable, SDK-generated identifier assigned before any app user id is
 * known.
 * @param deviceId A stable, SDK-generated identifier for the current device/install.
 * @param externalId The host app's own user identifier (hashed), if the host app has identified
 * with one; `null` while the user is anonymous.
 */
data class Identity(
    internal val anonymousId: String,
    internal val deviceId: String,
    internal val externalId: String? = null,
) {

    /** The identifier to use when referring to this identity: [externalId] if set, else [anonymousId]. */
    val id: String
        get() = externalId ?: anonymousId

    /** Whether the host app hasn't identified with its own user id yet. */
    internal val isAnonymous: Boolean
        get() = externalId == null

    /**
     * Whether this identity is linked to [externalId].
     *
     * @param externalId The already hashed external id to compare with.
     */
    internal fun belongsTo(externalId: String): Boolean {
        return this.externalId == externalId
    }

    /**
     * Renders this identity as its [id], so logging/string-interpolating an [Identity] shows the
     * same value that's actually sent to the backend rather than a generic object dump.
     *
     * @return [id].
     */
    override fun toString(): String {
        return id
    }
}
