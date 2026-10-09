// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.feature

/**
 * Fetches and votes on feature plans from wherever they're sourced from (typically the
 * MobileJoe backend).
 *
 * Implementations are expected to track pagination state internally: successive [load] calls
 * return successive pages until [reset] is called. Query parameters are applied when a new
 * pagination sequence starts.
 */
interface FeaturePlansGateway {

    /**
     * Returns the next page of feature plans matching the given query.
     *
     * @param statusFilter The status filter to apply.
     * @param sorting The sort order to apply.
     * @param searchQuery The free-text search query to apply, or `null` for none.
     * @return The next page of matching feature plans; an empty list once pagination is
     * exhausted.
     * @throws Exception if the fetch fails.
     */
    suspend fun load(
        statusFilter: FeaturePlans.StatusFilter,
        sorting: FeaturePlans.Sorting,
        searchQuery: String?
    ): List<FeaturePlan>

    /**
     * Returns a single feature plan by its identifier.
     *
     * @param id The identifier of the feature plan to fetch.
     * @return The requested feature plan.
     * @throws Exception if the fetch fails, including when no feature plan with the given [id]
     * exists.
     */
    suspend fun load(id: Int): FeaturePlan

    /**
     * Discards any pagination state, so the next [load] call starts over from the first page.
     *
     * @throws Exception if resetting fails.
     */
    suspend fun reset()

    /**
     * Casts a vote for [featurePlan].
     *
     * @param featurePlan The feature plan to vote for.
     * @return The updated feature plan, reflecting the new vote (e.g. incremented score,
     * `isVoted = true`).
     * @throws Exception if the vote request fails.
     */
    suspend fun vote(featurePlan: FeaturePlan): FeaturePlan
}
