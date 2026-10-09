// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.ui.compose.feature.list

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.mobilejoe.api.feature.FeaturePlan
import dev.mobilejoe.api.feature.FeaturePlans
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Backs [dev.mobilejoe.ui.compose.feature.list.FeaturePlanListScreen], loading the feature
 * plan list and exposing its state as [state], including status filtering, sorting, search,
 * pagination, and upvoting.
 *
 * Distinguishes user-initiated retries from pull-to-refresh: a retry failure (there is no list
 * to fall back on yet) is surfaced via [State.ListState.Error], while a refresh failure (a
 * list is already showing) is only logged, leaving the previously loaded list visible.
 *
 * @param featurePlans The feature plans data source to load from; defaults to the shared
 * [FeaturePlans] instance backed by the real network gateway. Also supplies the initial
 * [_statusFilter] and [_sorting] values.
 * @param viewModelScope The coroutine scope used for all asynchronous work in this view model.
 * Defaults to a scope tied to `Dispatchers.Main`; overridable for testing.
 */
class FeaturePlanListViewModel(
    private val featurePlans: FeaturePlans = FeaturePlans(),
    viewModelScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) : ViewModel(viewModelScope) {

    /** Backing field for the most recent load/reload failure, or `null` when it succeeded. */
    private val _error = MutableStateFlow<Throwable?>(null)

    /** The currently applied status filter, initialized from [featurePlans]. */
    private val _statusFilter = MutableStateFlow(featurePlans.statusFilter)

    /** The currently applied sort order, initialized from [featurePlans]. */
    private val _sorting = MutableStateFlow(featurePlans.sorting)

    /** The current search mode (inactive, entering a query, or showing results for a query). */
    private val _search = MutableStateFlow<Search>(Search.None)

    /**
     * The current feature plan list load state (independent of whether search is active),
     * derived from the latest loaded plans, the most recent load error, and the current
     * filter/sort selections.
     */
    private val _listState: StateFlow<State.ListState> = combine(
        featurePlans.all,
        _error,
        _statusFilter,
        _sorting,
    ) { plans, error, statusFilter, sorting ->
        when {
            error != null -> State.ListState.Error(statusFilter, sorting)
            plans != null && plans.isEmpty() -> State.ListState.Empty(statusFilter, sorting)
            plans != null -> State.ListState.Success(
                featurePlanItems = plans,
                statusFilter = statusFilter,
                sorting = sorting
            )

            else -> State.ListState.Loading(
                FeaturePlans.StatusFilter.ALL,
                FeaturePlans.Sorting.BY_NEWEST
            )
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        State.ListState.Loading(
            FeaturePlans.StatusFilter.ALL,
            FeaturePlans.Sorting.BY_NEWEST
        )
    )

    /**
     * The current top-level UI state, combining [_listState] with whether search is inactive,
     * being entered, or showing results.
     */
    val state: StateFlow<State> = combine(_search, _listState) { search, listState ->
        when (search) {
            is Search.None -> State.Default(listState)
            is Search.Input -> State.SearchInput
            is Search.Result -> State.SearchResult(search.query, listState)
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        State.Default(
            State.ListState.Loading(
                FeaturePlans.StatusFilter.ALL,
                FeaturePlans.Sorting.BY_NEWEST
            )
        )
    )

    init {
        loadFeaturePlans()
    }

    /**
     * Requests the next page of feature plans; called when the list has been scrolled near
     * its end. Reuses [loadFeaturePlans], so it is also effectively the initial load trigger
     * when called before any plans are loaded.
     */
    fun onListEndReached() {
        loadFeaturePlans()
    }

    /**
     * Loads the next page of feature plans (or the first page, on initial load). On failure,
     * the thrown error is logged and published via [_error], which drives [state] to
     * [State.ListState.Error].
     */
    private fun loadFeaturePlans() {
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            Log.e("FeaturePlanListViewModel", "Error loading feature plans", throwable)
            _error.value = throwable
        }

        viewModelScope.launch(exceptionHandler) {
            _error.emit(null)
            featurePlans.load()
        }
    }

    /**
     * Casts an upvote for [featurePlan]. Failures are only logged; [state] is left untouched
     * so a failed vote does not disrupt the currently displayed list.
     *
     * @param featurePlan The feature plan to upvote.
     */
    fun onUpVoteClicked(featurePlan: FeaturePlan) {
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            Log.e("FeaturePlanListViewModel", "Error up vote feature", throwable)
        }

        viewModelScope.launch(exceptionHandler) {
            featurePlans.vote(featurePlan)
        }
    }

    /**
     * Applies [statusFilter] and reloads the list under it. On failure, the thrown error is
     * logged and published via [_error], which drives [state] to [State.ListState.Error].
     *
     * @param statusFilter The status filter to apply.
     */
    fun onStatusFilterSelected(statusFilter: FeaturePlans.StatusFilter) {
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            Log.e("FeaturePlanListViewModel", "Error loading feature plans", throwable)
            _error.value = throwable
        }

        viewModelScope.launch(exceptionHandler) {
            _statusFilter.emit(statusFilter)
            featurePlans.setStatusFilter(statusFilter)
        }
    }

    /**
     * Applies [sorting] and reloads the list under it. On failure, the thrown error is logged
     * and published via [_error], which drives [state] to [State.ListState.Error].
     *
     * @param sorting The sort order to apply.
     */
    fun onSortSelected(sorting: FeaturePlans.Sorting) {
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            Log.e("FeaturePlanListViewModel", "Error loading feature plans", throwable)
            _error.value = throwable
        }

        viewModelScope.launch(exceptionHandler) {
            _sorting.emit(sorting)
            featurePlans.setSorting(sorting)
        }
    }

    /** Switches [state] to [State.SearchInput] so the user can enter a search query. */
    fun onSearchClicked() {
        _search.update { Search.Input }
    }

    /**
     * Cancels search mode, clearing any active search query and reloading the unfiltered list.
     * On failure, the thrown error is logged and published via [_error], which drives [state]
     * to a [State.Default] wrapping [State.ListState.Error].
     */
    fun onCancelSearchClicked() {
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            Log.e("FeaturePlanListViewModel", "Error loading feature plans", throwable)
            _error.value = throwable
        }

        viewModelScope.launch(exceptionHandler) {
            _search.update { Search.None }
            featurePlans.setSearchQuery(null)
        }
    }

    /**
     * Submits [query] as the active search, switching [state] to [State.SearchResult] and
     * loading matching feature plans. On failure, the thrown error is logged and published via
     * [_error], which drives [state] to a [State.SearchResult] wrapping
     * [State.ListState.Error].
     *
     * @param query The search query text.
     */
    fun onSearchSubmitted(query: String) {
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            Log.e("FeaturePlanListViewModel", "Error loading feature plans", throwable)
            _error.value = throwable
        }

        viewModelScope.launch(exceptionHandler) {
            _search.update { Search.Result(query) }
            featurePlans.setSearchQuery(query)
        }
    }

    /**
     * Retries loading the feature plan list after a failure; called from the error screen's
     * retry button. Unlike [onRefresh], a failure here is reported via [State.ListState.Error]
     * since there is no previously loaded list to keep showing instead.
     */
    fun onRetryClicked() {
        reloadFeaturePlans(reportFailure = true)
    }

    /**
     * Reloads the feature plan list; called from a pull-to-refresh gesture. Unlike
     * [onRetryClicked], a failure here is only logged, leaving the previously loaded list
     * visible instead of replacing it with an error state.
     */
    fun onRefresh() {
        reloadFeaturePlans(reportFailure = false)
    }

    /**
     * Reloads the feature plan list under the current filter/sort/search selections,
     * replacing whatever was previously loaded.
     *
     * @param reportFailure When `true`, a failure is published via [_error] (driving [state]
     * to an [State.ListState.Error]). When `false`, a failure is only logged and [_error] is
     * left untouched, so an already-visible list stays visible.
     */
    private fun reloadFeaturePlans(reportFailure: Boolean) {
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            Log.e("FeaturePlanListViewModel", "Error reload feature plans", throwable)
            if (reportFailure) {
                _error.value = throwable
            }
        }

        viewModelScope.launch(exceptionHandler) {
            featurePlans.reload()
            _error.emit(null)
        }
    }

    /**
     * The possible top-level UI states for the feature plan list screen, as exposed by [state].
     *
     * The screen is in [Default] most of the time, showing the unfiltered/filtered list
     * described by its [ListState]. Tapping the search affordance (see [onSearchClicked])
     * moves it to [SearchInput], and submitting a query (see [onSearchSubmitted]) moves it to
     * [SearchResult], which wraps its own independent [ListState] for the search results.
     */
    sealed interface State {

        /**
         * Search is inactive; shows the regular feature plan list.
         *
         * @property listState The current load state of the list.
         */
        data class Default(val listState: ListState) :
            State

        /** The user is entering a search query; the search input screen is shown. */
        data object SearchInput : State

        /**
         * A search query has been submitted; shows matching feature plans.
         *
         * @property query The active search query.
         * @property listState The current load state of the search results.
         */
        data class SearchResult(val query: String, val listState: ListState) :
            State

        /**
         * The possible load states for a feature plan list (either the regular [Default] list
         * or a [SearchResult] list — this hierarchy is shared by both since they behave
         * identically once a load is in flight).
         *
         * A list starts in [Loading] and, once initial load resolves, moves to [Success], [Empty],
         * or [Error]. A successful refresh can produce [Success] or [Empty], while a failed refresh
         * preserves the existing state. A retry remains [Error] while in flight and then moves to
         * [Success] or [Empty] on success, or stays [Error] on failure.
         */
        sealed interface ListState {

            /** The status filter currently applied to this list. */
            val statusFilter: FeaturePlans.StatusFilter

            /** The sort order currently applied to this list. */
            val sorting: FeaturePlans.Sorting

            /**
             * The list has not yet finished loading for the first time.
             *
             * @property statusFilter The status filter currently applied.
             * @property sorting The sort order currently applied.
             */
            data class Loading(
                override val statusFilter: FeaturePlans.StatusFilter,
                override val sorting: FeaturePlans.Sorting
            ) : ListState

            /**
             * The list was loaded successfully.
             *
             * @property featurePlanItems The loaded feature plans, in the order returned by
             * the API.
             * @property statusFilter The status filter currently applied.
             * @property sorting The sort order currently applied.
             */
            data class Success(
                val featurePlanItems: List<FeaturePlan>,
                override val statusFilter: FeaturePlans.StatusFilter,
                override val sorting: FeaturePlans.Sorting
            ) : ListState

            /**
             * The load completed successfully but returned no feature plans.
             *
             * @property statusFilter The status filter currently applied.
             * @property sorting The sort order currently applied.
             */
            data class Empty(
                override val statusFilter: FeaturePlans.StatusFilter,
                override val sorting: FeaturePlans.Sorting
            ) : ListState

            /**
             * The most recent load or retry attempt failed.
             *
             * @property statusFilter The status filter currently applied.
             * @property sorting The sort order currently applied.
             */
            data class Error(
                override val statusFilter: FeaturePlans.StatusFilter,
                override val sorting: FeaturePlans.Sorting
            ) : ListState
        }
    }

    /**
     * The current search mode for the feature plan list, backing [_search].
     *
     * Starts as [None]. [onSearchClicked] moves it to [Input]; [onSearchSubmitted] moves it to
     * [Result]; [onCancelSearchClicked] moves it back to [None].
     */
    private sealed interface Search {

        /** Search is inactive. */
        data object None : Search

        /** The user is entering a search query. */
        data object Input : Search

        /**
         * A search query has been submitted.
         *
         * @property query The active search query.
         */
        data class Result(val query: String) :
            Search
    }
}