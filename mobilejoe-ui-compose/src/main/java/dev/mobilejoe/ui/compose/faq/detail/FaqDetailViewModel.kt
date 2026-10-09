// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.ui.compose.faq.detail

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.mobilejoe.api.faq.Faq
import dev.mobilejoe.api.faq.Faqs
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
 * Backs [dev.mobilejoe.ui.compose.faq.detail.FaqDetailScreen], loading a single FAQ entry by
 * id and exposing its load progress and result as [state].
 *
 * The FAQ is loaded once on construction. There is no automatic retry: if the load fails, the
 * screen is expected to offer a retry affordance wired to [onRetryClicked], which simply
 * re-runs the same load. See [State] for the states the screen can be in and how they map to
 * what should be rendered.
 *
 * @param faqId The id of the FAQ to load.
 * @param faqs The FAQs data source to load from; defaults to the shared [Faqs] instance backed
 * by the real network gateway.
 * @param viewModelScope The coroutine scope used for all asynchronous work in this view model.
 * Defaults to a scope tied to `Dispatchers.Main.immediate`; overridable for testing.
 */
class FaqDetailViewModel(
    private val faqId: Int,
    private val faqs: Faqs = Faqs(),
    viewModelScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) : ViewModel(viewModelScope) {

    /** Backing field for the most recent load failure, or `null` when the last load succeeded. */
    private val _error = MutableStateFlow<Throwable?>(null)

    /**
     * The current FAQ detail UI state, derived from the latest loaded FAQ and the most recent
     * load error. Starts as [State.Loading] until the first load result is observed.
     */
    val state: StateFlow<State> = combine(
        faqs.single,
        _error,
    ) { faq, error ->
        when {
            error != null -> State.Error
            faq != null -> State.Success(faqItem = faq)
            else -> State.Loading
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        State.Loading
    )

    init {
        loadFaq()
    }

    /**
     * Loads the FAQ identified by [faqId]. On failure, the thrown error is logged and
     * published via [_error], which drives [state] to [State.Error].
     */
    private fun loadFaq() {
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            Log.e("FaqDetailViewModel", "Error loading FAQ", throwable)
            _error.value = throwable
        }

        viewModelScope.launch(exceptionHandler) {
            _error.emit(null)
            faqs.load(faqId)
        }
    }

    /** Retries loading the FAQ after a failure; called from the error screen's retry button. */
    fun onRetryClicked() {
        loadFaq()
    }

    /**
     * The possible UI states for the FAQ detail screen, as exposed by [state].
     *
     * The screen starts in [Loading] and moves to either [Success] or [Error] once the load
     * started in [loadFaq] resolves; there is no intermediate "empty" state since a FAQ lookup
     * by id either succeeds with one item or fails.
     */
    sealed interface State {

        /** The FAQ has not yet finished loading for the first time. */
        data object Loading : State

        /**
         * The FAQ was loaded successfully.
         *
         * @property faqItem The loaded FAQ.
         */
        data class Success(val faqItem: Faq) : State

        /** The most recent load attempt failed. */
        data object Error : State
    }
}
