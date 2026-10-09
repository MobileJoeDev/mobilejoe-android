// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.ui.compose.feature.detail

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
import kotlinx.coroutines.launch

/**
 * Backs [dev.mobilejoe.ui.compose.feature.detail.FeaturePlanDetailScreen], loading a single
 * feature plan by id and exposing its load progress and result as [state].
 *
 * The feature plan is loaded once on construction. If that load fails, the screen is expected
 * to offer a retry affordance wired to [onRetryClicked], which simply re-runs the same load.
 * Once a plan is showing, [onUpVoteClicked] lets the user cast an upvote for it without
 * disturbing the rest of the screen even if the vote request fails. See [State] for the states
 * the screen can be in.
 *
 * @param featurePlanId The id of the feature plan to load.
 * @param featurePlans The feature plans data source to load from; defaults to the shared
 * [FeaturePlans] instance backed by the real network gateway.
 * @param viewModelScope The coroutine scope used for all asynchronous work in this view model.
 * Defaults to a scope tied to `Dispatchers.Main`; overridable for testing.
 */
class FeaturePlanDetailViewModel(
    private val featurePlanId: Int,
    private val featurePlans: FeaturePlans = FeaturePlans(),
    viewModelScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) : ViewModel(viewModelScope) {

    /** Backing field for the most recent load failure, or `null` when the last load succeeded. */
    private val _error = MutableStateFlow<Throwable?>(null)

    /**
     * The current feature plan detail UI state, derived from the latest loaded feature plan
     * and the most recent load error. Starts as [State.Loading] until the first load result is
     * observed.
     */
    val state: StateFlow<State> = combine(
        featurePlans.single,
        _error,
    ) { featurePlan, error ->
        when {
            error != null -> State.Error
            featurePlan != null -> State.Success(featurePlanItem = featurePlan)
            else -> State.Loading
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        State.Loading
    )

    init {
        loadFeaturePlan()
    }

    /**
     * Loads the feature plan identified by [featurePlanId]. On failure, the thrown error is
     * logged and published via [_error], which drives [state] to [State.Error].
     */
    private fun loadFeaturePlan() {
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            Log.e("FeaturePlanDetailViewModel", "Error loading feature plan", throwable)
            _error.value = throwable
        }

        viewModelScope.launch(exceptionHandler) {
            _error.emit(null)
            featurePlans.load(featurePlanId)
        }
    }

    /**
     * Casts an upvote for [featurePlan]. Failures are only logged; [state] is left untouched
     * so a failed vote does not disrupt the currently displayed feature plan.
     *
     * @param featurePlan The feature plan to upvote.
     */
    fun onUpVoteClicked(featurePlan: FeaturePlan) {
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            Log.e("FeaturePlanDetailViewModel", "Error up vote feature", throwable)
        }

        viewModelScope.launch(exceptionHandler) {
            featurePlans.vote(featurePlan)
        }
    }

    /** Retries loading the feature plan after a failure; called from the error screen's retry button. */
    fun onRetryClicked() {
        loadFeaturePlan()
    }

    /**
     * The possible UI states for the feature plan detail screen, as exposed by [state].
     *
     * The screen starts in [Loading] and moves to either [Success] or [Error] once the load
     * started in [loadFeaturePlan] resolves. Voting via [onUpVoteClicked] does not introduce
     * any additional state of its own; the screen simply stays in [Success] while a vote is in
     * flight or fails.
     */
    sealed interface State {

        /** The feature plan has not yet finished loading for the first time. */
        data object Loading : State

        /**
         * The feature plan was loaded successfully.
         *
         * @property featurePlanItem The loaded feature plan.
         */
        data class Success(val featurePlanItem: FeaturePlan) : State

        /** The most recent load attempt failed. */
        data object Error : State
    }
}