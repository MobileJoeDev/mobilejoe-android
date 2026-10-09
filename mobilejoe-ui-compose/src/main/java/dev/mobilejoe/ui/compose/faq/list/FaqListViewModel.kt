// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.ui.compose.faq.list

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
 * Backs [dev.mobilejoe.ui.compose.faq.list.FaqListScreen], loading the full FAQ list and
 * exposing its state as [state].
 *
 * Distinguishes user-initiated retries from pull-to-refresh: a retry failure (there is no list
 * to fall back on yet) is surfaced via [State.Error], while a refresh failure (a list is
 * already showing) is only logged, leaving the previously loaded list visible.
 *
 * @param faqs The FAQs data source to load from; defaults to the shared [Faqs] instance backed
 * by the real network gateway.
 * @param viewModelScope The coroutine scope used for all asynchronous work in this view model.
 * Defaults to a scope tied to `Dispatchers.Main`; overridable for testing.
 */
class FaqListViewModel(
    private val faqs: Faqs = Faqs(),
    viewModelScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main),
) : ViewModel(viewModelScope) {

    /** Backing field for the most recent load/reload failure, or `null` when it succeeded. */
    private val _error = MutableStateFlow<Throwable?>(null)

    /**
     * The current FAQ list UI state, derived from the latest loaded FAQs and the most recent
     * load error. Starts as [State.Loading] until the first load result is observed.
     */
    val state: StateFlow<State> = combine(
        faqs.all,
        _error,
    ) { faqs, error ->
        when {
            error != null -> State.Error
            faqs != null && faqs.isEmpty() -> State.Empty
            faqs != null -> State.Success(faqItems = faqs)
            else -> State.Loading
        }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        State.Loading
    )

    init {
        loadFaqs()
    }

    /**
     * Performs the initial FAQ list load on view model creation. On failure, the thrown error
     * is logged and published via [_error], which drives [state] to [State.Error].
     */
    private fun loadFaqs() {
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            Log.e("FaqListViewModel", "Error loading FAQs", throwable)
            _error.value = throwable
        }

        viewModelScope.launch(exceptionHandler) {
            _error.emit(null)
            faqs.load()
        }
    }

    /**
     * Retries loading the FAQ list after a failure; called from the error screen's retry
     * button. Unlike [onRefresh], a failure here is reported via [State.Error] since there is
     * no previously loaded list to keep showing instead.
     */
    fun onRetryClicked() {
        reloadFaqs(reportFailure = true)
    }

    /**
     * Reloads the FAQ list; called from a pull-to-refresh gesture. Unlike [onRetryClicked], a
     * failure here is only logged, leaving the previously loaded list visible instead of
     * replacing it with an error state.
     */
    fun onRefresh() {
        reloadFaqs(reportFailure = false)
    }

    /**
     * Reloads the full FAQ list, replacing whatever was previously loaded.
     *
     * @param reportFailure When `true`, a failure is published via [_error] (driving [state] to
     * [State.Error]). When `false`, a failure is only logged and [_error] is left untouched, so
     * an already-visible list stays visible.
     */
    private fun reloadFaqs(reportFailure: Boolean) {
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            Log.e("FaqListViewModel", "Error reloading FAQs", throwable)
            if (reportFailure) {
                _error.value = throwable
            }
        }

        viewModelScope.launch(exceptionHandler) {
            faqs.load()
            _error.emit(null)
        }
    }

    /**
     * The possible UI states for the FAQ list, as exposed by [state].
     *
     * The screen starts in [Loading] and moves to [Success], [Empty], or [Error] after the initial
     * load. A successful refresh can produce [Success] or [Empty], while failure preserves the
     * current state. A retry stays [Error] while in flight, then moves to [Success] or [Empty] on
     * success or remains [Error] on failure.
     */
    sealed interface State {

        /** The FAQ list has not yet finished loading for the first time. */
        data object Loading : State

        /**
         * The FAQ list was loaded successfully.
         *
         * @property faqItems The loaded FAQs, in the order returned by the API.
         */
        data class Success(
            val faqItems: List<Faq>,
        ) : State

        /** The load completed successfully but returned no FAQs. */
        data object Empty : State

        /** The most recent load or retry attempt failed. */
        data object Error : State
    }
}