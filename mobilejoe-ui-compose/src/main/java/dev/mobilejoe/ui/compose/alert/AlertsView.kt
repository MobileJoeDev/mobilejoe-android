// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.ui.compose.alert

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.mobilejoe.api.alert.Alert

/**
 * Displays the active list of alerts as a stack of [AlertsBanner]s, and shows an
 * [AlertInfoSheet] modal bottom sheet when a banner is tapped.
 *
 * Nothing is rendered while alerts are loading, empty, or failed to load
 * ([AlertsViewModel.State.Loading], [AlertsViewModel.State.Empty], [AlertsViewModel.State.Error]);
 * only [AlertsViewModel.State.Success] produces visible content, so this composable is safe to
 * always place in a layout regardless of whether alerts are currently available.
 *
 * Alerts are reloaded automatically every time this composable's lifecycle owner reaches
 * `ON_START` (e.g. the containing screen becomes visible again), via [AlertsViewModel.onStart].
 *
 * @param modifier Modifier applied to the alert list container.
 * @param viewModel The view model backing this screen; defaults to a new instance wired to the
 * real [dev.mobilejoe.api.alert.Alerts] API.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertsView(
    modifier: Modifier = Modifier,
    viewModel: AlertsViewModel = viewModel { AlertsViewModel() }
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val alertSheet by viewModel.activeAlertInfo.collectAsStateWithLifecycle()

    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        viewModel.onStart()
    }

    alertSheet?.let { alert ->
        ModalBottomSheet(
            containerColor = MaterialTheme.colorScheme.background,
            onDismissRequest = viewModel::onDismissAlertInfo
        ) {
            AlertInfoSheet(alert)
        }
    }

    when (state) {
        AlertsViewModel.State.Loading,
        AlertsViewModel.State.Empty,
        AlertsViewModel.State.Error -> Unit

        is AlertsViewModel.State.Success ->
            AlertsView(
                state = state as AlertsViewModel.State.Success,
                onAlertClick = viewModel::onAlertClick,
                modifier = modifier
            )
    }
}

/**
 * Stateless rendering of a successfully loaded alert list as a vertical stack of
 * [AlertsBanner]s.
 *
 * @param state The loaded alerts to render.
 * @param modifier Modifier applied to the root [Column].
 * @param onAlertClick Invoked with the tapped alert when one of the banners is clicked.
 */
@Composable
private fun AlertsView(
    state: AlertsViewModel.State.Success,
    modifier: Modifier = Modifier,
    onAlertClick: (alert: Alert) -> Unit = {},
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier) {
        state.alertItems.forEach { alert ->
            AlertsBanner(
                alert = alert,
                onClick = { onAlertClick(alert) },
            )
        }
    }
}