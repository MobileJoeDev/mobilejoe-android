// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.network.parser

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant
import java.time.format.DateTimeFormatter

/**
 * Serializes [Instant] to/from the ISO-8601 instant string format used by the MobileJoe backend
 * (e.g. `2024-01-01T00:00:00Z`).
 *
 * Registered on every timestamp field across the SDK's response models (see each field's
 * `@Serializable(with = LocalDateTimeSerializer::class)` annotation) rather than relying on
 * `kotlinx.serialization`'s default [Instant] handling, so the wire format stays pinned to what
 * the backend actually sends regardless of library defaults changing.
 */
internal object LocalDateTimeSerializer : KSerializer<Instant> {

    /** Formatter used for both encoding and decoding. */
    private val formatter: DateTimeFormatter = DateTimeFormatter.ISO_INSTANT

    /** Describes this serializer's wire format: a single ISO-8601 instant string. */
    override val descriptor = PrimitiveSerialDescriptor(
        "java.time.LocalDateTime",
        PrimitiveKind.STRING
    )

    /**
     * Writes [value] as an ISO-8601 instant string using [formatter].
     *
     * @param encoder The encoder to write to.
     * @param value The instant to serialize.
     */
    override fun serialize(encoder: Encoder, value: Instant) {
        encoder.encodeString(formatter.format(value))
    }

    /**
     * Reads an ISO-8601 instant string from [decoder] and parses it back into an [Instant] using
     * [formatter].
     *
     * @param decoder The decoder to read from.
     * @return The parsed [Instant].
     * @throws java.time.format.DateTimeParseException if the decoded string isn't a valid
     * ISO-8601 instant.
     */
    override fun deserialize(decoder: Decoder): Instant {
        return formatter.parse(decoder.decodeString(), Instant::from)
    }
}
