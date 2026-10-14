// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.ui.compose.faq.list

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults.cardColors
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.mobilejoe.api.faq.Faq
import dev.mobilejoe.ui.compose.R
import dev.mobilejoe.ui.compose.empty.EmptyContent
import dev.mobilejoe.ui.compose.error.ErrorContent
import dev.mobilejoe.ui.compose.faq.FaqUiConfiguration
import dev.mobilejoe.ui.compose.faq.contact.FaqContactInfo
import dev.mobilejoe.ui.compose.faq.row.FaqRow
import dev.mobilejoe.ui.compose.loading.LoadingContent
import dev.mobilejoe.ui.compose.util.groupedItemShape
import dev.mobilejoe.ui.compose.util.openUrl

/**
 * Stateful screen listing all FAQs, with pull-to-refresh and tap-to-open-detail support.
 *
 * @param configuration UI configuration, including whether/how to offer a contact-support link
 * below the list.
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
    viewModel: FaqListViewModel = viewModel { FaqListViewModel() }
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    onNavigateUp?.let { navigateUp ->
        BackHandler { navigateUp() }
    }

    FaqListScreen(
        state = state,
        configuration = configuration,
        onNavigateUp = onNavigateUp,
        onItemClicked = onNavigateToDetails,
        onRefresh = viewModel::onRefresh,
        onRetryClicked = viewModel::onRetryClicked,
        onContactClicked = configuration.getMailToUrl()?.let { url ->
            { context.openUrl(url) }
        },
    )
}

/**
 * Stateless rendering of the FAQ list screen for a given [state]: loading spinner, error
 * screen with retry, empty placeholder, or the loaded list, all wrapped in a
 * [PullToRefreshBox].
 *
 * @param state The current FAQ list load state to render.
 * @param configuration UI configuration, including whether/how to offer a contact-support link.
 * @param onNavigateUp Invoked when the back button is tapped. When `null`, no back button is
 * shown.
 * @param onItemClicked Invoked with the tapped FAQ when a list item is clicked.
 * @param onRefresh Invoked when the user performs a pull-to-refresh gesture.
 * @param onRetryClicked Invoked when the retry button is tapped in the error state.
 * @param onContactClicked Invoked when the contact-support link is tapped. When `null`, no
 * contact link is shown.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FaqListScreen(
    state: FaqListViewModel.State,
    configuration: FaqUiConfiguration,
    onNavigateUp: (() -> Unit)? = null,
    onItemClicked: (Faq) -> Unit = {},
    onRefresh: () -> Unit = {},
    onRetryClicked: () -> Unit = {},
    onContactClicked: (() -> Unit)? = null,
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
                            configuration = configuration,
                            onItemClicked = onItemClicked,
                            onContactClicked = onContactClicked,
                        )

                }
            }
        }
    }
}

/**
 * Renders the loaded FAQs as a scrollable list of [FaqRow]s with an optional [FaqContactInfo]
 * card below them.
 *
 * @param modifier Modifier applied to the [LazyColumn].
 * @param state The successfully loaded FAQ list state.
 * @param configuration UI configuration used to render [FaqContactInfo] when [onContactClicked]
 * is non-null.
 * @param onItemClicked Invoked with the tapped FAQ when a row is clicked.
 * @param onContactClicked Invoked when the contact-support link is tapped. When `null`, no
 * contact link is shown.
 */
@Composable
private fun List(
    modifier: Modifier = Modifier,
    state: FaqListViewModel.State.Success,
    configuration: FaqUiConfiguration,
    onItemClicked: (Faq) -> Unit = {},
    onContactClicked: (() -> Unit)? = null,
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

        onContactClicked?.let {
            item {
                Card(
                    colors = cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth(),
                ) {
                    FaqContactInfo(
                        configuration = configuration,
                        onClicked = onContactClicked,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}


/** Preview of [FaqListScreen] rendering a loaded FAQ list without a contact-support link. */
@Preview
@Composable
private fun ListWithoutContactPreview() {
    FaqListScreen(
        state = previewState,
        configuration = FaqUiConfiguration(),
        onContactClicked = null,
    )
}

/** Preview of [FaqListScreen] rendering a loaded FAQ list with a contact-support link. */
@Preview
@Composable
private fun ListWithContactPreview() {
    FaqListScreen(
        state = previewState,
        configuration = FaqUiConfiguration(
            recipient = "support@mobilejoe.dev",
            contactSupportHint = "Still questions? Mail us!",
            contactSupportMailToLabel = "Send Mail",
        ),
        onContactClicked = {},
    )
}

private val previewState = FaqListViewModel.State.Success(
    faqItems = listOf(
        Faq.empty().copy(
            title = "Sample question?",
            body = "This is a sample question body.",
        ),
        Faq.empty().copy(
            title = "Other question that is long enough to wrap to a second line?",
            body = "This is a second FAQ question body.",
        ),
    ),
)
