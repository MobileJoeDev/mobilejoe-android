// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.samplejoe.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults.cardColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.mobilejoe.samplejoe.R

/**
 * The app's landing screen, and the first thing shown after [dev.mobilejoe.samplejoe.SampleApp]
 * finishes its startup configuration.
 *
 * Renders a [Scaffold] with a simple top bar and a scrollable [LazyColumn] listing one [Card]
 * per [SampleItem] in [sampleItems] — by default every entry in [SampleItem.entries] — each
 * showing that item's title string and a trailing chevron [Icon]. Tapping a card invokes
 * [onItemClicked] with that item; [dev.mobilejoe.samplejoe.SampleNavHost] wires this to
 * navigating to the corresponding [dev.mobilejoe.samplejoe.Sample] detail route.
 *
 * @param sampleItems the items to list, in order; defaults to every [SampleItem].
 * @param onItemClicked invoked with the tapped [SampleItem] when its card is clicked.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HomeScreen(
    sampleItems: List<SampleItem> = SampleItem.entries,
    onItemClicked: (SampleItem) -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) }
            )
        }
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(contentPadding)
                .padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 8.dp)
        ) {
            sampleItems.forEach { item ->
                item {
                    Card(
                        colors = cardColors(
                            containerColor = MaterialTheme.colorScheme.surface,
                        ),
                        onClick = { onItemClicked(item) }
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                modifier = Modifier.padding(16.dp),
                                text = stringResource(item.titleId)
                            )

                            Icon(
                                modifier = Modifier.padding(16.dp),
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Compose preview of [HomeScreen] listing every [SampleItem] with a no-op click handler, so the
 * Android Studio preview pane can render the full landing-screen list without a navigation
 * host.
 */
@Composable
@Preview
private fun HomeScreenPreview() {
    HomeScreen(
        sampleItems = SampleItem.entries,
        onItemClicked = {}
    )
}