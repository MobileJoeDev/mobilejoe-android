// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.identity.util

import java.security.MessageDigest
import java.util.UUID

/**
 * Formats this [UUID] as a lowercase hex string with the dashes removed.
 *
 * @return The compacted, lowercase, dash-free representation of this UUID.
 */
fun UUID.compact(): String {
    return toString().replace("-", "").lowercase()
}

/**
 * Hashes this string with SHA-256 and returns the digest as a lowercase hex string.
 *
 * Used to avoid persisting/transmitting a host app's raw user identifier.
 *
 * @return The lowercase hex-encoded SHA-256 digest of this string's UTF-8 bytes.
 */
@OptIn(ExperimentalStdlibApi::class)
fun String.sha265Hash(): String {
    return MessageDigest.getInstance("SHA-256")
        .digest(toByteArray())
        .toHexString()
}
