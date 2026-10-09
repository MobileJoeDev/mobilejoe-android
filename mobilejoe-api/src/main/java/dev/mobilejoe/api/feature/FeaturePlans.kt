// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.feature

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Public entry point for observing, loading, filtering, sorting, searching, and voting on
 * feature plans.
 *
 * Wraps a [FeaturePlansGateway] and exposes both the full (paginated) feature plan list and a
 * single selected feature plan as hot [Flow]s. [statusFilter], [sorting], and [search] hold the
 * current query parameters; changing any of them via [setStatusFilter], [setSorting], or
 * [setSearchQuery] triggers a [reload].
 *
 * @param featurePlansGateway The gateway used to fetch and vote on feature plans. Defaults to a
 * [RemoteFeaturePlansGateway] backed by the real network client; callers wire in a fake/mock for
 * tests.
 */
class FeaturePlans(
    private val featurePlansGateway: FeaturePlansGateway = RemoteFeaturePlansGateway()
) {

    /** Backing store for [all]; `null` until the first call to [load] or [reload]. */
    private val _all = MutableStateFlow<List<FeaturePlan>?>(null)

    /** The current feature plan list, or `null` before it has been loaded for the first time. */
    val all: Flow<List<FeaturePlan>?> = _all

    /** Backing store for [single]; `null` until [load] is called with an id. */
    private val _single = MutableStateFlow<FeaturePlan?>(null)

    /** The feature plan most recently requested via [load], or `null` if none loaded yet. */
    val single: Flow<FeaturePlan?> = _single

    /** The sort order currently applied to [all]. Change it via [setSorting]. */
    var sorting: Sorting = Sorting.BY_NEWEST
        private set

    /** The status filter currently applied to [all]. Change it via [setStatusFilter]. */
    var statusFilter: StatusFilter = StatusFilter.ALL
        private set

    /** The free-text search query currently applied to [all], or `null` if none is active. */
    var search: String? = null
        private set

    /**
     * Fetches the next page of feature plans (using the current [statusFilter], [sorting], and
     * [search]) from [featurePlansGateway] and appends it to whatever is already held in [all].
     *
     * Safe to call repeatedly to page through results; use [reload] instead to discard the
     * current list and fetch a fresh first page.
     *
     * @throws Exception if the underlying gateway request fails. On failure, [all] is left
     * unchanged so any previously loaded feature plans remain visible to observers.
     */
    suspend fun load() {
        val features = featurePlansGateway.load(
            statusFilter,
            sorting,
            search?.takeIf { s -> s.isNotBlank() },
        )
        _all.emit(_all.value.orEmpty().plus(features))
    }

    /**
     * Loads a single feature plan by [id] from [featurePlansGateway] and publishes it to
     * [single].
     *
     * [single] is cleared to `null` before the request starts, so observers see a brief
     * "not loaded yet" state while the new feature plan is being fetched.
     *
     * @param id The identifier of the feature plan to load.
     * @throws Exception if the underlying gateway request fails. On failure, [single] is left at
     * `null` (its value was already cleared before the request started).
     */
    suspend fun load(id: Int) {
        _single.emit(null)

        val feature = featurePlansGateway.load(id)
        _single.emit(feature)
    }

    /**
     * Resets pagination and fetches a fresh first page of feature plans (using the current
     * [statusFilter], [sorting], and [search]), replacing [all] with it in one step.
     *
     * The fetch happens before [all] is touched, so if it fails the previously loaded list in
     * [all] is left untouched rather than being cleared first and possibly never repopulated.
     *
     * @throws Exception if the underlying gateway request fails. On failure, [all] is left
     * unchanged.
     */
    suspend fun reload() {
        featurePlansGateway.reset()
        val features = featurePlansGateway.load(
            statusFilter,
            sorting,
            search?.takeIf { s -> s.isNotBlank() },
        )
        _all.emit(features)
    }

    /**
     * Casts a vote for [featurePlan] through [featurePlansGateway] and applies the server's
     * updated [FeaturePlan] (new score/voted state) to both [all] and [single], if present in
     * either.
     *
     * @param featurePlan The feature plan to vote for.
     * @throws Exception if the underlying gateway request fails. On failure, [all] and [single]
     * are left unchanged.
     */
    suspend fun vote(featurePlan: FeaturePlan) {
        val updatedPlan = featurePlansGateway.vote(featurePlan)

        _all.emit(
            _all.value.orEmpty().map { plan ->
                if (plan.id == updatedPlan.id) updatedPlan else plan
            }
        )

        _single.value?.let { plan ->
            if (plan.id == updatedPlan.id) {
                _single.emit(updatedPlan)
            }
        }
    }

    /**
     * Updates [statusFilter] and triggers a [reload] with the new filter applied.
     *
     * @param statusFilter The new status filter to apply.
     * @throws Exception if the resulting [reload] fails.
     */
    suspend fun setStatusFilter(statusFilter: StatusFilter) {
        this.statusFilter = statusFilter
        reload()
    }

    /**
     * Updates [sorting] and triggers a [reload] with the new sort order applied.
     *
     * @param sorting The new sort order to apply.
     * @throws Exception if the resulting [reload] fails.
     */
    suspend fun setSorting(sorting: Sorting) {
        this.sorting = sorting
        reload()
    }

    /**
     * Updates [search] and triggers a [reload] with the new search query applied.
     *
     * @param query The new free-text search query, or `null`/blank to clear search.
     * @throws Exception if the resulting [reload] fails.
     */
    suspend fun setSearchQuery(query: String?) {
        search = query
        reload()
    }

    companion object {

        fun preview(): FeaturePlans =
            FeaturePlans(InMemoryFeaturePlansGateway(InMemoryFeaturePlansGateway.seedStore))

        fun previewEmpty(): FeaturePlans = FeaturePlans(InMemoryFeaturePlansGateway())

        fun previewFailed(): FeaturePlans = FeaturePlans(
            InMemoryFeaturePlansGateway(loadError = IllegalStateException("Failed loading"))
        )
    }

    /**
     * Sort order applied to the feature plan list, sent to the backend as [key] whenever
     * [load]/[reload] runs. Change it via [setSorting].
     */
    enum class Sorting(
        /** The backend query parameter value for this sort order. */
        val key: String,
    ) {

        /** Newest feature plans first. */
        BY_NEWEST("created"),

        /** Highest-scoring feature plans first. */
        BY_SCORE("score"),
    }

    /**
     * Status filter applied to the feature plan list, sent to the backend as [keys] whenever
     * [load]/[reload] runs. Change it via [setStatusFilter].
     */
    enum class StatusFilter(
        /** The backend status values that count as a match for this filter. */
        val keys: List<String>,
    ) {

        /** Every non-terminal status. */
        ALL(listOf("open", "under_review", "planned", "in_progress")),

        /** Only feature plans currently under review. */
        UNDER_REVIEW(listOf("under_review")),

        /** Only feature plans that are planned. */
        PLANNED(listOf("planned")),

        /** Only feature plans currently in progress. */
        IN_PROGRESS(listOf("in_progress")),

        /** Only completed feature plans. */
        COMPLETED(listOf("completed")),

        /** Feature plans that are under review, planned, or in progress. */
        ROADMAP(listOf("under_review", "planned", "in_progress")),
    }
}
