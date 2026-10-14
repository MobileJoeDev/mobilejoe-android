// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.identity

/**
 * Thrown when the [Identity] could not be read from or written to the device's storage, e.g.
 * because the disk is full or the storage is still locked after a reboot.
 *
 * [Joe.identify][dev.mobilejoe.api.Joe.identify] and [Joe.logout][dev.mobilejoe.api.Joe.logout]
 * throw it, so the host app learns that the identity change didn't go through.
 * [Joe.configure][dev.mobilejoe.api.Joe.configure] logs it instead. Calling the SDK before it is
 * configured is a different failure and throws [IllegalStateException].
 *
 * @param message What failed.
 * @param cause The underlying failure, if any.
 */
class JoeStorageException(
    message: String,
    cause: Throwable? = null,
) : RuntimeException(message, cause)
