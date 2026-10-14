// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.samplejoe.ui.alert

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.mobilejoe.api.alert.Alerts
import dev.mobilejoe.ui.compose.R
import dev.mobilejoe.ui.compose.alert.AlertsView
import dev.mobilejoe.ui.compose.alert.AlertsViewModel
import dev.mobilejoe.samplejoe.R as SampleR

/**
 * Demo screen for the `mobilejoe-ui-compose` alerts feature.
 *
 * Renders a [Scaffold] with a back-enabled top bar, and a scrollable column that places the
 * library's [AlertsView] at the very top followed by six rows of gray [PlaceholderRow] boxes.
 * The placeholder rows exist purely to simulate real host-app content underneath the alerts
 * banner, so this screen demonstrates how [AlertsView] looks embedded above genuine content
 * rather than in isolation. [dev.mobilejoe.samplejoe.SampleNavHost] reaches this screen for
 * [dev.mobilejoe.samplejoe.ui.home.SampleItem.ALERTS].
 *
 * @param onNavigateUp invoked when the top bar's back button is pressed; typically wired to
 *   the hosting [androidx.navigation.NavHostController]'s `navigateUp()`.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun AlertsScreen(
    onNavigateUp: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onNavigateUp) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.generic_back),
                        )
                    }
                },
                title = {
                    Text(stringResource(SampleR.string.alerts_title))
                }
            )
        }
    ) { contentPadding ->
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            AlertsView(viewModel = viewModel { AlertsViewModel(Alerts.preview()) })

            repeat(6) {
                PlaceholderRow()
            }
        }
    }
}

/**
 * Renders one horizontal row of three equally-weighted [PlaceholderItem] boxes.
 *
 * [AlertsScreen] repeats this six times to build a grid-like stand-in for real host-app
 * content below the alerts banner; the row itself holds no state and takes no parameters.
 */
@Composable
private fun PlaceholderRow() {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        PlaceholderItem()
        PlaceholderItem()
        PlaceholderItem()
    }
}

/**
 * Renders a single light-gray, square placeholder box that fills its equal share of the
 * parent [Row]'s width via [RowScope.weight].
 *
 * Used only inside [PlaceholderRow] to fill out [AlertsScreen]'s demo content grid; it has no
 * click handling or content of its own.
 */
@Composable
private fun RowScope.PlaceholderItem() {
    Box(
        Modifier
            .weight(1f)
            .aspectRatio(1f)
            .background(Color.LightGray)
    )
}

/**
 * Compose preview of [AlertsScreen] rendered with its default arguments, i.e. a no-op
 * `onNavigateUp`, so the Android Studio preview pane can render the whole screen — alerts
 * banner plus placeholder grid — without a real navigation host wired up.
 */
@Composable
@Preview
private fun AlertsScreenPreview() {
    AlertsScreen()
}