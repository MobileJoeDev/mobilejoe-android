// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.alert

import dev.mobilejoe.api.network.parser.LocalDateTimeSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant
import kotlin.Int

/**
 * A single alert banner served by the MobileJoe backend, as returned by the `alerts` endpoint.
 *
 * Alerts are typically shown as dismissible banners at the top of a screen. Each alert carries a
 * [kind] that determines how prominently it's styled, and an optional [message] body that a UI
 * can choose to reveal only when the banner is expanded/tapped. Instances of this type are
 * fetched and cached through [Alerts]; call [Alerts.load] to populate a fresh list rather than
 * constructing [Alert]s directly (use [empty] only for previews/tests).
 *
 * @param id The alert's unique identifier.
 * @param title The short headline shown in the alert banner.
 * @param message The optional longer-form body text shown when the alert is expanded, or `null`
 * if the alert only has a title.
 * @param kind The visual severity of the alert, used to pick banner styling.
 * @param occurred The point in time the underlying event that triggered this alert happened.
 * @param updatedAt The point in time this alert was last updated on the backend.
 * @param createdAt The point in time this alert was first created on the backend.
 */
@Serializable
data class Alert(
    val id: Int,
    val title: String,
    val message: String?,
    val kind: Kind,
    @Serializable(with = LocalDateTimeSerializer::class)
    @SerialName("occurred_at")
    val occurred: Instant,

    @Serializable(with = LocalDateTimeSerializer::class)
    @SerialName("updated_at")
    val updatedAt: Instant,

    @Serializable(with = LocalDateTimeSerializer::class)
    @SerialName("created_at")
    val createdAt: Instant,
) {

    companion object {

        /**
         * Creates a placeholder [Alert] with default/empty values.
         *
         * Intended for previews and tests where a concrete instance is required but its exact
         * field values don't matter.
         *
         * @return A new [Alert] with `id = 0`, blank text fields, [Kind.ERROR], and all
         * timestamps set to [Instant.EPOCH].
         */
        fun empty(): Alert = Alert(
            id = 0,
            title = "",
            message = null,
            kind = Kind.ERROR,
            occurred = Instant.EPOCH,
            updatedAt = Instant.EPOCH,
            createdAt = Instant.EPOCH,
        )
    }

    /**
     * The visual severity of an [Alert], used to pick banner styling (e.g. color, icon).
     *
     * Serialized to/from the lowercase string values used by the backend (see each entry's
     * `@SerialName`).
     */
    enum class Kind {

        /** A non-critical, informational alert. */
        @SerialName("warning")
        WARNING,

        /** A critical alert that requires the user's attention. */
        @SerialName("error")
        ERROR
    }
}
