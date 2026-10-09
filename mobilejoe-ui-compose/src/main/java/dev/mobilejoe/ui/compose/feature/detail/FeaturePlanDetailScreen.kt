// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.ui.compose.feature.detail

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults.cardColors
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.mobilejoe.api.feature.FeaturePlan
import dev.mobilejoe.ui.compose.R
import dev.mobilejoe.ui.compose.error.ErrorContent
import dev.mobilejoe.ui.compose.feature.row.FeaturePlanHeader
import dev.mobilejoe.ui.compose.loading.LoadingContent
import java.time.Instant
import android.text.format.DateFormat as AndroidDateFormat

/**
 * Stateful screen showing the full detail of a single feature plan, with a top bar and
 * upvote support.
 *
 * @param featurePlanId The id of the feature plan to display.
 * @param onNavigateUp Invoked when the back button is tapped. When `null`, no back button is
 * shown.
 * @param viewModel The view model backing this screen; defaults to a new instance loading
 * [featurePlanId] from the real [dev.mobilejoe.api.feature.FeaturePlans] API.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeaturePlanDetailScreen(
    featurePlanId: Int,
    onNavigateUp: (() -> Unit)? = null,
    viewModel: FeaturePlanDetailViewModel = remember {
        FeaturePlanDetailViewModel(featurePlanId)
    }
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    onNavigateUp?.let { navigateUp ->
        BackHandler { navigateUp() }
    }

    FeaturePlanDetailScreen(
        state = state,
        onNavigateUp = onNavigateUp,
        onUpVoteClicked = viewModel::onUpVoteClicked,
        onRetryClicked = viewModel::onRetryClicked,
    )
}

/**
 * Stateless rendering of the feature plan detail screen for a given [state]: loading spinner,
 * error screen with retry, or the loaded feature plan content.
 *
 * @param state The current feature plan detail load state to render.
 * @param onNavigateUp Invoked when the back button is tapped. When `null`, no back button is
 * shown.
 * @param onRetryClicked Invoked when the retry button is tapped in the error state.
 * @param onUpVoteClicked Invoked with the loaded feature plan when the upvote control is
 * tapped.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeaturePlanDetailScreen(
    state: FeaturePlanDetailViewModel.State,
    onNavigateUp: (() -> Unit)? = null,
    onRetryClicked: () -> Unit = {},
    onUpVoteClicked: (FeaturePlan) -> Unit = {},
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
                    Text(stringResource(R.string.feature_plans_feature_plan))
                },
            )
        },
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .padding(contentPadding)
        ) {
            when (state) {
                FeaturePlanDetailViewModel.State.Loading ->
                    LoadingContent()

                FeaturePlanDetailViewModel.State.Error ->
                    ErrorContent(
                        onRetryClicked = onRetryClicked
                    )

                is FeaturePlanDetailViewModel.State.Success ->
                    Detail(
                        state = state,
                        onUpVoteClicked = onUpVoteClicked,
                    )
            }
        }
    }
}

/**
 * Renders the loaded feature plan's full content.
 *
 * @param state The successfully loaded feature plan state.
 * @param onUpVoteClicked Invoked when the upvote control is tapped.
 */
@Composable
private fun Detail(
    state: FeaturePlanDetailViewModel.State.Success,
    onUpVoteClicked: (FeaturePlan) -> Unit = {},
) {
    val featurePlan = state.featurePlanItem

    Column(
        modifier = Modifier
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Card(
            colors = cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(24.dp),
                modifier = Modifier.padding(16.dp),
            ) {
                FeaturePlanHeader(
                    featurePlan = featurePlan,
                    titleStyle = MaterialTheme.typography.titleLarge,
                    titleToMetaSpacing = 10.dp,
                    dateFormat = AndroidDateFormat.getLongDateFormat(LocalContext.current),
                    onUpVoteClicked = { onUpVoteClicked(featurePlan) },
                )

                Text(
                    text = featurePlan.body,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

/** Preview of [FeaturePlanDetailScreen] rendering a loaded feature plan. */
@Preview
@Composable
private fun Preview() {
    FeaturePlanDetailScreen(
        FeaturePlanDetailViewModel.State.Success(
            featurePlanItem = FeaturePlan.empty().copy(
                title = "Sample Feature",
                body = "This is a sample feature plan body.",
                score = 42,
                status = FeaturePlan.Status.PLANNED,
                createdAt = Instant.ofEpochMilli(1754259294096),
                isVoted = false
            ),
        ),
    )
}