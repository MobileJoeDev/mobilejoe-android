// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.ui.compose.faq.list

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.mobilejoe.api.faq.Faq
import dev.mobilejoe.ui.compose.R
import dev.mobilejoe.ui.compose.empty.EmptyContent
import dev.mobilejoe.ui.compose.error.ErrorContent
import dev.mobilejoe.ui.compose.faq.FaqUiConfiguration
import dev.mobilejoe.ui.compose.faq.row.FaqRow
import dev.mobilejoe.ui.compose.loading.LoadingContent
import dev.mobilejoe.ui.compose.util.groupedItemShape

/**
 * Stateful screen listing all FAQs, with pull-to-refresh and tap-to-open-detail support.
 *
 * @param configuration UI configuration for the FAQ feature (currently unused by this screen
 * directly, but accepted for API symmetry with [dev.mobilejoe.ui.compose.faq.detail.FaqDetailScreen]).
 * @param onNavigateUp Invoked when the back button is tapped. When `null`, no back button is
 * shown.
 * @param onNavigateToDetails Invoked with the tapped FAQ when a list item is clicked.
 * @param viewModel The view model backing this screen; defaults to a new instance loading FAQs
 * from the real [dev.mobilejoe.api.faq.Faqs] API.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaqListScreen(
    configuration: FaqUiConfiguration,
    onNavigateUp: (() -> Unit)? = null,
    onNavigateToDetails: (Faq) -> Unit = {},
    viewModel: FaqListViewModel = remember { FaqListViewModel() }
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    onNavigateUp?.let { navigateUp ->
        BackHandler { navigateUp() }
    }

    FaqListScreen(
        state = state,
        onNavigateUp = onNavigateUp,
        onItemClicked = onNavigateToDetails,
        onRefresh = viewModel::onRefresh,
        onRetryClicked = viewModel::onRetryClicked,
    )
}

/**
 * Stateless rendering of the FAQ list screen for a given [state]: loading spinner, error
 * screen with retry, empty placeholder, or the loaded list, all wrapped in a
 * [PullToRefreshBox].
 *
 * @param state The current FAQ list load state to render.
 * @param onNavigateUp Invoked when the back button is tapped. When `null`, no back button is
 * shown.
 * @param onItemClicked Invoked with the tapped FAQ when a list item is clicked.
 * @param onRefresh Invoked when the user performs a pull-to-refresh gesture.
 * @param onRetryClicked Invoked when the retry button is tapped in the error state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FaqListScreen(
    state: FaqListViewModel.State,
    onNavigateUp: (() -> Unit)? = null,
    onItemClicked: (Faq) -> Unit = {},
    onRefresh: () -> Unit = {},
    onRetryClicked: () -> Unit = {},
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    onNavigateUp?.let { onClick ->
                        IconButton(onClick = onClick) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(R.string.generic_back),
                            )
                        }
                    }
                },
                title = {
                    Text(stringResource(R.string.faqs))
                },
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxSize()
        ) {
            PullToRefreshBox(
                isRefreshing = false,
                onRefresh = onRefresh,
            ) {
                when (state) {
                    FaqListViewModel.State.Loading ->
                        LoadingContent()

                    FaqListViewModel.State.Error ->
                        ErrorContent(
                            onRetryClicked = onRetryClicked
                        )

                    FaqListViewModel.State.Empty ->
                        EmptyContent()

                    is FaqListViewModel.State.Success ->
                        List(
                            state = state,
                            onItemClicked = onItemClicked,
                        )

                }
            }
        }
    }
}

/**
 * Renders the loaded FAQs as a scrollable, line-limited list of [FaqRow]s.
 *
 * @param modifier Modifier applied to the [LazyColumn].
 * @param state The successfully loaded FAQ list state.
 * @param onItemClicked Invoked with the tapped FAQ when a row is clicked.
 */
@Composable
private fun List(
    modifier: Modifier = Modifier,
    state: FaqListViewModel.State.Success,
    onItemClicked: (Faq) -> Unit = {},
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp)
    ) {
        itemsIndexed(state.faqItems) { index, item ->
            if (index > 0) {
                HorizontalDivider(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp)
                )
            }

            FaqRow(
                faq = item,
                shape = groupedItemShape(index, state.faqItems.size),
                onClicked = { onItemClicked(item) }
            )
        }
    }
}


/** Preview of [FaqListScreen] rendering a loaded FAQ list. */
@Preview
@Composable
private fun Preview() {
    FaqListScreen(
        state = FaqListViewModel.State.Success(
            faqItems = listOf(
                Faq.empty().copy(
                    title = "Sample question?",
                    body = "This is a sample question body.",
                ),
                Faq.empty().copy(
                    title = "Other question?",
                    body = "This is a second FAQ question body.",
                ),
            ),
        )
    )
}
