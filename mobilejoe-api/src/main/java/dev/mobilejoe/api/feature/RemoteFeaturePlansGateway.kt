// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.feature

import dev.mobilejoe.api.network.JoeNetworkClient
import dev.mobilejoe.api.network.NetworkClient
import dev.mobilejoe.api.network.NetworkClient.Response

/**
 * [FeaturePlansGateway] backed by [NetworkClient]'s feature plan endpoints.
 *
 * Pagination is driven by the `next` link returned in each list response: [load] follows
 * [ListState] between calls to know whether to fetch the first page, the next page via its
 * stored URL, or nothing at all (pagination exhausted).
 *
 * @param networkClient The network client used to fetch and vote on feature plans. Defaults to
 * the shared [JoeNetworkClient] singleton.
 */
internal class RemoteFeaturePlansGateway(
    private val networkClient: NetworkClient = JoeNetworkClient
) : FeaturePlansGateway {

    /** Current pagination cursor; see [ListState]. Reset to [ListState.Init] by [reset]. */
    private var listState: ListState = ListState.Init

    /**
     * Fetches the next page of feature plans according to [listState]: the first page (filtered
     * by [statusFilter]/[sorting]/[searchQuery]) when [ListState.Init], the next page's URL when
     * [ListState.Next], or nothing when [ListState.Final]. Advances [listState] based on
     * whether the response carries a `next` link.
     *
     * @param statusFilter The status filter to apply. Only used when starting a new pagination
     * sequence (i.e. when [listState] is [ListState.Init]).
     * @param sorting The sort order to apply. Only used when starting a new pagination sequence.
     * @param searchQuery The free-text search query to apply, or `null` for none. Only used when
     * starting a new pagination sequence.
     * @return The next page of matching feature plans; an empty list once pagination is
     * exhausted.
     * @throws Exception if the underlying request fails.
     */
    override suspend fun load(
        statusFilter: FeaturePlans.StatusFilter,
        sorting: FeaturePlans.Sorting,
        searchQuery: String?,
    ): List<FeaturePlan> {
        val response = when (val state = listState) {
            ListState.Init -> networkClient.getFeaturePlans(
                statuses = statusFilter.keys,
                sorting = sorting.key,
                search = searchQuery
            )

            is ListState.Next -> networkClient.getFeaturePlans(requestUrl = state.url)
            ListState.Final -> Response(emptyList())
        }

        listState = response.nextUrl?.let {
            ListState.Next(it)
        } ?: ListState.Final

        return response.data
    }

    /**
     * Fetches a single feature plan's detail over the network. Does not consult or affect
     * [listState], since this call is independent of the paginated list.
     *
     * @param id The identifier of the feature plan to fetch.
     * @return The requested feature plan from [NetworkClient.getFeaturePlan].
     * @throws Exception if the underlying request fails.
     */
    override suspend fun load(id: Int): FeaturePlan {
        return networkClient.getFeaturePlan(featurePlanId = id)
    }

    /**
     * Casts a vote for [featurePlan] over the network.
     *
     * @param featurePlan The feature plan to vote for.
     * @return The updated feature plan from [NetworkClient.postVoteFeaturePlans].
     * @throws Exception if the underlying request fails.
     */
    override suspend fun vote(featurePlan: FeaturePlan): FeaturePlan {
        return networkClient.postVoteFeaturePlans(featurePlan.id)
    }

    /** Rewinds [listState] to [ListState.Init], so the next [load] starts a new first page. */
    override suspend fun reset() {
        listState = ListState.Init
    }

    /**
     * Pagination cursor tracked between successive [load] calls.
     *
     * Starts at [Init], advances to [Next] once a page reports a follow-up link, and settles at
     * [Final] once the server stops returning one.
     */
    private sealed interface ListState {

        /** No page has been fetched yet; the next [load] call should request the first page. */
        data object Init : ListState

        /**
         * A page has been fetched and the server reported another page is available.
         *
         * @param url The `next` link to request for the following page.
         */
        data class Next(val url: String) : ListState

        /** The last page has been fetched; further [load] calls return an empty list. */
        data object Final : ListState
    }
}
