// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.ui.compose.feature.list

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults.assistChipColors
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.mobilejoe.api.feature.FeaturePlan
import dev.mobilejoe.api.feature.FeaturePlans
import dev.mobilejoe.ui.compose.R
import dev.mobilejoe.ui.compose.empty.EmptyContent
import dev.mobilejoe.ui.compose.error.ErrorContent
import dev.mobilejoe.ui.compose.feature.FeaturePlanUiConfiguration
import dev.mobilejoe.ui.compose.feature.row.FeaturePlanRow
import dev.mobilejoe.ui.compose.feature.row.label
import dev.mobilejoe.ui.compose.loading.LoadingContent
import dev.mobilejoe.ui.compose.search.SearchScreen
import dev.mobilejoe.ui.compose.util.groupedItemShape
import dev.mobilejoe.ui.compose.util.openUrl
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.Instant

/**
 * Stateful screen listing feature plans, switching between the regular list, an active search
 * input, and search results based on [FeaturePlanListViewModel.state].
 *
 * @param configuration UI configuration used to build the "suggest a feature" mail-to link
 * shown as a floating action button.
 * @param onNavigateUp Invoked when the back button is tapped. When `null`, no back button is
 * shown.
 * @param onNavigateToDetails Invoked with the tapped feature plan when a list item is clicked.
 * @param viewModel The view model backing this screen; defaults to a new instance loading
 * feature plans from the real [dev.mobilejoe.api.feature.FeaturePlans] API.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeaturePlanListScreen(
    configuration: FeaturePlanUiConfiguration,
    onNavigateUp: (() -> Unit)? = null,
    onNavigateToDetails: (FeaturePlan) -> Unit = {},
    viewModel: FeaturePlanListViewModel = remember { FeaturePlanListViewModel() }
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    onNavigateUp?.let { navigateUp ->
        BackHandler { navigateUp() }
    }

    when (state) {
        is FeaturePlanListViewModel.State.Default -> FeaturePlanListScreen(
            state = (state as FeaturePlanListViewModel.State.Default).listState,
            onRefresh = viewModel::onRefresh,
            onUpVoteClicked = viewModel::onUpVoteClicked,
            onItemClicked = onNavigateToDetails,
            onListEndReached = viewModel::onListEndReached,
            onStatusFilterSelected = viewModel::onStatusFilterSelected,
            onSortSelected = viewModel::onSortSelected,
            onNavigateUp = onNavigateUp,
            onSearchClicked = viewModel::onSearchClicked,
            onRetryClicked = viewModel::onRetryClicked,
            onAddClicked = configuration.getMailToUrl(context)?.let { url ->
                { context.openUrl(url) }
            },
        )

        is FeaturePlanListViewModel.State.SearchResult -> FeaturePlanResultScreen(
            state = (state as FeaturePlanListViewModel.State.SearchResult).listState,
            query = (state as FeaturePlanListViewModel.State.SearchResult).query,
            onRefresh = viewModel::onRefresh,
            onUpVoteClicked = viewModel::onUpVoteClicked,
            onItemClicked = onNavigateToDetails,
            onListEndReached = viewModel::onListEndReached,
            onStatusFilterSelected = viewModel::onStatusFilterSelected,
            onSortSelected = viewModel::onSortSelected,
            onCancelSearch = viewModel::onCancelSearchClicked,
            onSearchClicked = viewModel::onSearchClicked,
            onRetryClicked = viewModel::onRetryClicked,
            onAddClicked = configuration.getMailToUrl(context)?.let { url ->
                { context.openUrl(url) }
            },
        )

        FeaturePlanListViewModel.State.SearchInput -> SearchScreen(
            onCancel = viewModel::onCancelSearchClicked,
            onSubmit = viewModel::onSearchSubmitted
        )
    }
}

/**
 * Stateless rendering of the regular (non-search) feature plan list screen: top bar with a
 * search action, an optional "suggest a feature" floating action button, a status/sort filter
 * row, and a pull-to-refreshable list for a given [state].
 *
 * @param state The current feature plan list load state to render.
 * @param onRefresh Invoked when the user performs a pull-to-refresh gesture.
 * @param onStatusFilterSelected Invoked when a status filter option is selected.
 * @param onSortSelected Invoked when a sort option is selected.
 * @param onNavigateUp Invoked when the back button is tapped. When `null`, no back button is
 * shown.
 * @param onUpVoteClicked Invoked with the tapped feature plan when its upvote control is
 * tapped.
 * @param onItemClicked Invoked with the tapped feature plan when a list item is clicked.
 * @param onListEndReached Invoked when the list has been scrolled near its end, to trigger
 * pagination.
 * @param onSearchClicked Invoked when the search action in the top bar is tapped.
 * @param onRetryClicked Invoked when the retry button is tapped in the error state.
 * @param onAddClicked Invoked when the floating action button is tapped. When `null`, no
 * floating action button is shown.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeaturePlanListScreen(
    state: FeaturePlanListViewModel.State.ListState,
    onRefresh: () -> Unit = {},
    onStatusFilterSelected: (FeaturePlans.StatusFilter) -> Unit = {},
    onSortSelected: (FeaturePlans.Sorting) -> Unit = {},
    onNavigateUp: (() -> Unit)? = null,
    onUpVoteClicked: (FeaturePlan) -> Unit = {},
    onItemClicked: (FeaturePlan) -> Unit = {},
    onListEndReached: () -> Unit = {},
    onSearchClicked: () -> Unit = {},
    onRetryClicked: () -> Unit = {},
    onAddClicked: (() -> Unit)? = null,
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
                    Text(stringResource(R.string.feature_plans))
                },
                actions = {
                    IconButton(onClick = onSearchClicked) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = stringResource(R.string.generic_search)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            onAddClicked?.let { onClicked ->
                FloatingActionButton(onClick = onClicked) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                }
            }
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxSize()
        ) {
            FilterRow(
                statusFilter = state.statusFilter,
                onStatusFilterSelected = onStatusFilterSelected,
                onSortingSelected = onSortSelected,
                sorting = state.sorting
            )

            PullToRefreshBox(
                isRefreshing = false,
                onRefresh = onRefresh,
            ) {
                when (state) {
                    is FeaturePlanListViewModel.State.ListState.Loading -> LoadingContent()

                    is FeaturePlanListViewModel.State.ListState.Success -> Items(
                        state = state,
                        onUpVoteClicked = onUpVoteClicked,
                        onItemClicked = onItemClicked,
                        onListEndReached = onListEndReached
                    )

                    is FeaturePlanListViewModel.State.ListState.Empty ->
                        EmptyContent()

                    is FeaturePlanListViewModel.State.ListState.Error ->
                        ErrorContent(onRetryClicked = onRetryClicked)
                }
            }
        }
    }
}

/**
 * Stateless rendering of the search-results feature plan list screen: identical layout to the
 * private [FeaturePlanListScreen] overload, but with the search [query] as the top bar title
 * and a back action that cancels search instead of navigating up.
 *
 * @param state The current search-result load state to render.
 * @param query The active search query, shown as the top bar title.
 * @param onRefresh Invoked when the user performs a pull-to-refresh gesture.
 * @param onStatusFilterSelected Invoked when a status filter option is selected.
 * @param onSortSelected Invoked when a sort option is selected.
 * @param onCancelSearch Invoked when the back button is tapped, to exit search mode.
 * @param onUpVoteClicked Invoked with the tapped feature plan when its upvote control is
 * tapped.
 * @param onItemClicked Invoked with the tapped feature plan when a list item is clicked.
 * @param onListEndReached Invoked when the list has been scrolled near its end, to trigger
 * pagination.
 * @param onSearchClicked Invoked when the search action in the top bar is tapped (to edit the
 * query again).
 * @param onRetryClicked Invoked when the retry button is tapped in the error state.
 * @param onAddClicked Invoked when the floating action button is tapped. When `null`, no
 * floating action button is shown.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeaturePlanResultScreen(
    state: FeaturePlanListViewModel.State.ListState,
    query: String,
    onRefresh: () -> Unit = {},
    onStatusFilterSelected: (FeaturePlans.StatusFilter) -> Unit = {},
    onSortSelected: (FeaturePlans.Sorting) -> Unit = {},
    onCancelSearch: () -> Unit = { },
    onUpVoteClicked: (FeaturePlan) -> Unit = {},
    onItemClicked: (FeaturePlan) -> Unit = {},
    onListEndReached: () -> Unit = {},
    onSearchClicked: () -> Unit = {},
    onRetryClicked: () -> Unit = {},
    onAddClicked: (() -> Unit)? = null,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = {
                    IconButton(onClick = onCancelSearch) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.generic_back),
                        )
                    }
                },
                title = {
                    Text(query)
                },
                actions = {
                    IconButton(onClick = onSearchClicked) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = stringResource(R.string.generic_search)
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            onAddClicked?.let { onClicked ->
                FloatingActionButton(onClick = onClicked) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                }
            }
        }
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxSize()
        ) {
            FilterRow(
                statusFilter = state.statusFilter,
                onStatusFilterSelected = onStatusFilterSelected,
                onSortingSelected = onSortSelected,
                sorting = state.sorting
            )

            PullToRefreshBox(
                isRefreshing = false,
                onRefresh = onRefresh,
            ) {
                when (state) {
                    is FeaturePlanListViewModel.State.ListState.Loading -> LoadingContent()

                    is FeaturePlanListViewModel.State.ListState.Success -> Items(
                        state = state,
                        onUpVoteClicked = onUpVoteClicked,
                        onItemClicked = onItemClicked,
                        onListEndReached = onListEndReached
                    )

                    is FeaturePlanListViewModel.State.ListState.Empty ->
                        EmptyContent()

                    is FeaturePlanListViewModel.State.ListState.Error ->
                        ErrorContent(onRetryClicked = onRetryClicked)
                }
            }
        }
    }
}

/**
 * Renders the loaded feature plans as a scrollable, line-limited list of [FeaturePlanRow]s,
 * triggering [onListEndReached] (to drive pagination) each time scrolling transitions into
 * being within 3 items of the end, and reserving trailing space so the last row isn't
 * obscured by the floating action button.
 *
 * @param modifier Modifier applied to the [LazyColumn].
 * @param state The successfully loaded feature plan list state.
 * @param onUpVoteClicked Invoked with the tapped feature plan when its upvote control is
 * tapped.
 * @param onItemClicked Invoked with the tapped feature plan when a row is clicked.
 * @param onListEndReached Invoked once each time the list transitions into being scrolled
 * within 3 items of its end (not repeatedly while it stays there).
 */
@Composable
private fun Items(
    modifier: Modifier = Modifier,
    state: FeaturePlanListViewModel.State.ListState.Success,
    onUpVoteClicked: (FeaturePlan) -> Unit = {},
    onItemClicked: (FeaturePlan) -> Unit = {},
    onListEndReached: () -> Unit = {},
) {
    val listState = rememberLazyListState()

    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .map { lastItemIndex -> lastItemIndex >= state.featurePlanItems.size - 3 }
            .distinctUntilChanged()
            .collect { endReached ->
                if (endReached) {
                    onListEndReached()
                }
            }
    }

    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        itemsIndexed(state.featurePlanItems) { index, item ->
            if (index > 0) {
                HorizontalDivider(
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(horizontal = 16.dp)
                )
            }

            FeaturePlanRow(
                featurePlan = item,
                shape = groupedItemShape(index, state.featurePlanItems.size),
                onUpVoteClicked = { onUpVoteClicked(item) },
                onClicked = { onItemClicked(item) }
            )
        }

        item {
            Box(Modifier.height(88.dp))
        }
    }
}

/**
 * Renders the sort and status filter chips side by side above the feature plan list.
 *
 * @param sorting The currently selected sort option, shown on [SortChip].
 * @param statusFilter The currently selected status filter, shown on [StatusChip].
 * @param onStatusFilterSelected Invoked when a status filter option is selected.
 * @param onSortingSelected Invoked when a sort option is selected.
 */
@Composable
private fun FilterRow(
    sorting: FeaturePlans.Sorting, statusFilter: FeaturePlans.StatusFilter,
    onStatusFilterSelected: (FeaturePlans.StatusFilter) -> Unit = {},
    onSortingSelected: (FeaturePlans.Sorting) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        SortChip(selected = sorting, onSelected = onSortingSelected)
        StatusChip(selected = statusFilter, onSelected = onStatusFilterSelected)
    }
}

/**
 * An assist chip showing the current sort selection, which opens a dropdown menu of all
 * [FeaturePlans.Sorting] options when tapped.
 *
 * @param selected The currently selected sort option, shown as the chip's label and checked
 * in the dropdown.
 * @param onSelected Invoked with the tapped option when a menu item is selected.
 */
@Composable
private fun SortChip(
    selected: FeaturePlans.Sorting,
    onSelected: (FeaturePlans.Sorting) -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }

    Box {
        DropdownMenu(
            expanded = showSortMenu,
            onDismissRequest = { showSortMenu = false }
        ) {
            FeaturePlans.Sorting.entries.forEach { sort ->
                DropdownMenuItem(
                    text = { Text(sort.label()) },
                    trailingIcon = {
                        if (sort == selected) {
                            MenuCheckIcon()
                        }
                    },
                    onClick = {
                        onSelected(sort)
                        showSortMenu = false
                    }
                )
            }
        }

        AssistChip(
            onClick = { showSortMenu = true },
            label = { Text(selected.label()) },
            colors = assistChipColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            trailingIcon = {
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = stringResource(R.string.generic_show_options),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        )
    }
}

/**
 * An assist chip showing the current status filter selection, which opens a dropdown menu of
 * all [FeaturePlans.StatusFilter] options when tapped.
 *
 * @param selected The currently selected status filter, shown as the chip's label and checked
 * in the dropdown.
 * @param onSelected Invoked with the tapped option when a menu item is selected.
 */
@Composable
private fun StatusChip(
    selected: FeaturePlans.StatusFilter,
    onSelected: (FeaturePlans.StatusFilter) -> Unit
) {
    var showStatusFilterMenu by remember { mutableStateOf(false) }

    Box {
        DropdownMenu(
            expanded = showStatusFilterMenu,
            onDismissRequest = { showStatusFilterMenu = false }
        ) {
            FeaturePlans.StatusFilter.entries.forEach { statusFilter ->
                DropdownMenuItem(
                    text = { Text(statusFilter.label()) },
                    trailingIcon = {
                        if (selected == statusFilter) {
                            MenuCheckIcon()
                        }
                    },
                    onClick = {
                        onSelected(statusFilter)
                        showStatusFilterMenu = false
                    }
                )
            }
        }

        AssistChip(
            onClick = { showStatusFilterMenu = true },
            label = { Text(selected.label()) },
            colors = assistChipColors(
                containerColor = MaterialTheme.colorScheme.surface,
            ),
            trailingIcon = {
                Icon(
                    Icons.Filled.ArrowDropDown,
                    contentDescription = stringResource(R.string.generic_show_options),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        )
    }
}

/** A small checkmark icon used as a dropdown menu item's trailing icon for the selected option. */
@Composable
private fun MenuCheckIcon() {
    Icon(
        Icons.Filled.Check,
        contentDescription = stringResource(R.string.generic_selected),
        modifier = Modifier.size(18.dp)
    )
}

/** Preview of the feature plan list screen with a floating action button. */
@Preview
@Composable
private fun ListWithAddPreview() {
    FeaturePlanListScreen(
        state = FeaturePlanListViewModel.State.ListState.Success(
            featurePlanItems = listOf(
                FeaturePlan.empty().copy(
                    title = "Sample Feature",
                    body = "This is a sample feature plan body.",
                    score = 42,
                    status = FeaturePlan.Status.PLANNED,
                    updatedAt = Instant.ofEpochMilli(1754259294096),
                    isVoted = false
                ),
                FeaturePlan.empty().copy(
                    title = "Other feature",
                    body = "This is a sample feature plan body.",
                    score = 1,
                    status = FeaturePlan.Status.IN_PROGRESS,
                    updatedAt = Instant.ofEpochMilli(1754259444096),
                    isVoted = true
                ),
            ),
            statusFilter = FeaturePlans.StatusFilter.ALL,
            sorting = FeaturePlans.Sorting.BY_SCORE,
        ),
        onAddClicked = {},
    )
}

/** Preview of the feature plan list screen without a floating action button. */
@Preview
@Composable
private fun ListWithoutAddPreview() {
    FeaturePlanListScreen(
        state = FeaturePlanListViewModel.State.ListState.Success(
            featurePlanItems = listOf(
                FeaturePlan.empty().copy(
                    title = "Sample Feature",
                    body = "This is a sample feature plan body.",
                    score = 42,
                    status = FeaturePlan.Status.PLANNED,
                    updatedAt = Instant.ofEpochMilli(1754259294096),
                    isVoted = false
                ),
                FeaturePlan.empty().copy(
                    title = "Other feature",
                    body = "This is a sample feature plan body.",
                    score = 1,
                    status = FeaturePlan.Status.IN_PROGRESS,
                    updatedAt = Instant.ofEpochMilli(1754259444096),
                    isVoted = true
                ),
            ),
            statusFilter = FeaturePlans.StatusFilter.ALL,
            sorting = FeaturePlans.Sorting.BY_SCORE,
        ),
        onAddClicked = null,
    )
}

/** Preview of the feature plan search-results screen with a floating action button. */
@Preview
@Composable
private fun ResultWithAddPreview() {
    FeaturePlanResultScreen(
        query = "my wished feature",
        state = FeaturePlanListViewModel.State.ListState.Success(
            featurePlanItems = listOf(
                FeaturePlan.empty().copy(
                    title = "Sample Feature",
                    body = "This is a sample feature plan body.",
                    score = 42,
                    status = FeaturePlan.Status.PLANNED,
                    updatedAt = Instant.ofEpochMilli(1754259294096),
                    isVoted = false
                ),
                FeaturePlan.empty().copy(
                    id = 2,
                    title = "Other feature",
                    body = "This is a sample feature plan body.",
                    score = 1,
                    status = FeaturePlan.Status.IN_PROGRESS,
                    updatedAt = Instant.ofEpochMilli(1754259444096),
                ),
            ),
            statusFilter = FeaturePlans.StatusFilter.ALL,
            sorting = FeaturePlans.Sorting.BY_SCORE,
        ),
        onAddClicked = {},
    )
}

/** Preview of the feature plan search-results screen without a floating action button. */
@Preview
@Composable
private fun ResultWithoutAddPreview() {
    FeaturePlanResultScreen(
        query = "my wished feature",
        state = FeaturePlanListViewModel.State.ListState.Success(
            featurePlanItems = listOf(
                FeaturePlan.empty().copy(
                    title = "Sample Feature",
                    body = "This is a sample feature plan body.",
                    score = 42,
                    status = FeaturePlan.Status.PLANNED,
                    updatedAt = Instant.ofEpochMilli(1754259294096),
                    isVoted = false
                ),
                FeaturePlan.empty().copy(
                    title = "Other feature",
                    body = "This is a sample feature plan body.",
                    score = 1,
                    status = FeaturePlan.Status.IN_PROGRESS,
                    updatedAt = Instant.ofEpochMilli(1754259444096),
                    isVoted = true
                ),
            ),
            statusFilter = FeaturePlans.StatusFilter.ALL,
            sorting = FeaturePlans.Sorting.BY_SCORE,
        ),
        onAddClicked = null,
    )
}

/** Preview of the search input screen used by the feature plan list's search action. */
@Preview
@Composable
private fun SearchPreview() {
    SearchScreen(
        onSubmit = {},
        onCancel = {},
    )
}
