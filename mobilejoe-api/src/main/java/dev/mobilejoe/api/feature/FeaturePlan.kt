// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.feature

import dev.mobilejoe.api.network.parser.LocalDateTimeSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * A single feature request/plan served by the MobileJoe backend.
 *
 * A feature plan represents one user-submitted (or curated) product idea, together with its
 * community [score] and lifecycle [status]. Instances of this type are fetched, paginated,
 * filtered, sorted, searched, and voted on through [FeaturePlans] — call [FeaturePlans.load] to
 * populate a list or [FeaturePlans.vote] to cast a vote, rather than constructing or mutating
 * [FeaturePlan] directly (use [empty] only for previews/tests, since this is an immutable data
 * class and every "mutation" from the backend arrives as a brand-new instance via `copy`).
 *
 * @param id The feature plan's unique identifier.
 * @param title The feature plan's short headline.
 * @param body The feature plan's full description.
 * @param score The current vote score (net upvotes) for this feature plan.
 * @param status Where this feature plan currently sits in its lifecycle.
 * @param updatedAt The point in time this feature plan was last updated on the backend.
 * @param createdAt The point in time this feature plan was first created on the backend.
 * @param isVoted Whether the current identity has already voted for this feature plan.
 */
@Serializable
data class FeaturePlan(
    val id: Int,
    val title: String,
    val body: String,
    val score: Int,
    val status: Status,

    @Serializable(with = LocalDateTimeSerializer::class)
    @SerialName("updated_at")
    val updatedAt: Instant,

    @Serializable(with = LocalDateTimeSerializer::class)
    @SerialName("created_at")
    val createdAt: Instant,

    @SerialName("voted")
    val isVoted: Boolean,
) {

    /**
     * Where a [FeaturePlan] currently sits in its lifecycle, from initial submission ([OPEN])
     * through triage ([UNDER_REVIEW]), scheduling ([PLANNED]) and active work ([IN_PROGRESS]), to
     * a terminal state of either [COMPLETED] or [CLOSED].
     *
     * Serialized to/from the lowercase, underscore-separated string values used by the backend
     * (see each entry's `@SerialName`).
     */
    enum class Status {

        /** Newly submitted, not yet triaged. */
        @SerialName("open")
        OPEN,

        /** Being evaluated by the team. */
        @SerialName("under_review")
        UNDER_REVIEW,

        /** Accepted and scheduled for future work. */
        @SerialName("planned")
        PLANNED,

        /** Actively being worked on. */
        @SerialName("in_progress")
        IN_PROGRESS,

        /** Shipped. */
        @SerialName("completed")
        COMPLETED,

        /** Rejected or otherwise will not be worked on. */
        @SerialName("closed")
        CLOSED
    }

    companion object {

        /**
         * Creates a placeholder [FeaturePlan] with default/empty values.
         *
         * Intended for previews and tests where a concrete instance is required but its exact
         * field values don't matter.
         *
         * @return A new [FeaturePlan] with `id = 0`, blank text fields, `score = 0`,
         * [Status.OPEN], `isVoted = false`, and all timestamps set to [Instant.EPOCH].
         */
        fun empty(): FeaturePlan = FeaturePlan(
            id = 0,
            title = "",
            body = "",
            score = 0,
            status = Status.OPEN,
            updatedAt = Instant.EPOCH,
            createdAt = Instant.EPOCH,
            isVoted = false,
        )
    }
}
