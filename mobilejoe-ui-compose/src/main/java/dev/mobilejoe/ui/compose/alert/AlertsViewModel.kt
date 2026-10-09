// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.ui.compose.alert

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.mobilejoe.api.alert.Alert
import dev.mobilejoe.api.alert.Alerts
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
 * Backs [AlertsView], exposing the current list of alerts as [state] and tracking which
 * alert (if any) has been tapped for detail view via [activeAlertInfo].
 *
 * Alerts are loaded once on creation and reloaded every time [onStart] is invoked (typically
 * wired to the host composable's `ON_START` lifecycle event). A reload failure is logged but
 * does not surface as an error state, so a previously successful alert list stays visible
 * rather than being replaced by an error screen.
 *
 * @param alerts The alerts data source to load from; defaults to the shared [Alerts] instance
 * backed by the real network gateway.
 * @param viewModelScope The coroutine scope used for all asynchronous work in this view model.
 * Defaults to a scope tied to `Dispatchers.Main.immediate`; overridable for testing.
 */
class AlertsViewModel(
    private val alerts: Alerts = Alerts(),
    viewModelScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
) : ViewModel(viewModelScope) {

    /** Backing field for the most recent load failure, or `null` when the last load succeeded. */
    private val _error = MutableStateFlow<Throwable?>(null)

    /**
     * The current alerts UI state, derived from the latest loaded alert list and the most
     * recent load error. Starts as [State.Loading] until the first load result (success or
     * failure) is observed.
     */
    val state: StateFlow<State> = combine(
        alerts.all,
        _error,
    ) { alerts, error ->
        when {
            error != null -> State.Error
            alerts == null -> State.Loading
            alerts.isEmpty() -> State.Empty
            else -> State.Success(alertItems = alerts)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), State.Loading)


    /** Backing field for [activeAlertInfo]. */
    private val _activeAlertInfo = MutableStateFlow<Alert?>(null)

    /** The alert currently selected for detail display, or `null` if none is selected. */
    val activeAlertInfo: StateFlow<Alert?> = _activeAlertInfo

    init {
        loadAlerts()
    }

    /**
     * Performs the initial alert load on view model creation. On failure, the thrown error is
     * logged and published via [_error], which drives [state] to [State.Error].
     */
    private fun loadAlerts() {
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            Log.e("AlertsViewModel", "Error loading alerts", throwable)
            _error.value = throwable
        }

        viewModelScope.launch(exceptionHandler) {
            _error.emit(null)
            alerts.load()
        }
    }

    /**
     * Requests a fresh alert load. Intended to be called when the hosting screen becomes
     * active again (e.g. the app returns to the foreground), so alerts stay reasonably
     * up to date without requiring a manual refresh gesture.
     */
    fun onStart() {
        reloadAlerts()
    }

    /**
     * Reloads alerts in the background. Unlike [loadAlerts], a failure here is only logged,
     * not published via [_error], so an already-visible alert list is left untouched instead
     * of being replaced by an error state.
     */
    private fun reloadAlerts() {
        val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
            Log.e("AlertsViewModel", "Error reload alerts", throwable)
        }

        viewModelScope.launch(exceptionHandler) {
            _error.emit(null)
            alerts.load()
        }
    }

    /**
     * Marks [alert] as the active alert, which causes [AlertsView] to present its detail
     * bottom sheet.
     *
     * @param alert The alert that was tapped.
     */
    fun onAlertClick(alert: Alert) {
        _activeAlertInfo.update { alert }
    }

    /** Clears the active alert, dismissing the detail bottom sheet. */
    fun onDismissAlertInfo() {
        _activeAlertInfo.update { null }
    }

    /**
     * The possible UI states for the alerts list, as exposed by [state].
     *
     * The view model starts in [Loading] and transitions to [Empty], [Error], or [Success]
     * after the initial load. A successful background reload triggered by [onStart] can produce
     * either [Success] or [Empty]; a failed reload does not publish a new error, so the current
     * state remains visible.
     */
    sealed interface State {

        /** Alerts have not yet finished loading for the first time. */
        data object Loading : State

        /** The load completed successfully but returned no alerts. */
        data object Empty : State

        /** The most recent load attempt failed. */
        data object Error : State

        /**
         * Alerts were loaded successfully.
         *
         * @property alertItems The loaded alerts, in the order returned by the API.
         */
        data class Success(val alertItems: List<Alert>) : State
    }
}