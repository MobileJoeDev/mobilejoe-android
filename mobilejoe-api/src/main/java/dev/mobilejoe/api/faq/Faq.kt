// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.faq

import dev.mobilejoe.api.network.parser.LocalDateTimeSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * A single frequently-asked-question entry served by the MobileJoe backend.
 *
 * FAQs are plain question/answer pairs ([title]/[body]) with an explicit backend-assigned
 * [position] used to order them in a list UI. Instances of this type are fetched and cached
 * through [Faqs]; call [Faqs.load] to populate the list, or call it with an id to fetch one FAQ's
 * full detail. Use [empty] only to build a placeholder instance for
 * previews/tests.
 *
 * @param id The FAQ's unique identifier.
 * @param title The question text.
 * @param body The answer text.
 * @param position The FAQ's display order relative to other FAQs (lower sorts first).
 * @param updatedAt The point in time this FAQ was last updated on the backend.
 * @param createdAt The point in time this FAQ was first created on the backend.
 */
@Serializable
data class Faq(
    val id: Int,
    val title: String,
    val body: String,
    val position: Int,

    @Serializable(with = LocalDateTimeSerializer::class)
    @SerialName("updated_at")
    val updatedAt: Instant,

    @Serializable(with = LocalDateTimeSerializer::class)
    @SerialName("created_at")
    val createdAt: Instant,
) {

    companion object Companion {

        /**
         * Creates a placeholder [Faq] with default/empty values.
         *
         * Intended for previews and tests where a concrete instance is required but its exact
         * field values don't matter.
         *
         * @return A new [Faq] with `id = 0`, blank text fields, `position = 0`, and all
         * timestamps set to [Instant.EPOCH].
         */
        fun empty(): Faq = Faq(
            id = 0,
            title = "",
            body = "",
            position = 0,
            updatedAt = Instant.EPOCH,
            createdAt = Instant.EPOCH,
        )
    }
}
