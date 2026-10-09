// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api

import kotlinx.serialization.json.Json

/**
 * The [Json] configuration shared by every serializer in the SDK: request bodies include default
 * values and omit explicit `null`s, while response parsing tolerates unknown keys so the SDK
 * doesn't break if the backend adds new fields.
 */
internal val json = Json {
    // Encode
    encodeDefaults = true
    explicitNulls = false

    // Decode
    ignoreUnknownKeys = true
}
