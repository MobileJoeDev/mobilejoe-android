// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.ui.compose.faq.detail

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.mobilejoe.api.faq.Faq
import dev.mobilejoe.ui.compose.R
import dev.mobilejoe.ui.compose.error.ErrorContent
import dev.mobilejoe.ui.compose.faq.FaqUiConfiguration
import dev.mobilejoe.ui.compose.faq.contact.FaqContactInfo
import dev.mobilejoe.ui.compose.loading.LoadingContent
import dev.mobilejoe.ui.compose.util.openUrl

/**
 * Stateful screen showing the full detail of a single FAQ, with a top bar and optional
 * "contact support" call to action.
 *
 * @param faqId The id of the FAQ to display.
 * @param configuration UI configuration, including whether/how to offer a contact-support link.
 * @param onNavigateUp Invoked when the back button is tapped. When `null`, no back button is
 * shown.
 * @param viewModel The view model backing this screen; defaults to a new instance loading
 * [faqId] from the real [dev.mobilejoe.api.faq.Faqs] API.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaqDetailScreen(
    faqId: Int,
    configuration: FaqUiConfiguration,
    onNavigateUp: (() -> Unit)? = null,
    viewModel: FaqDetailViewModel = viewModel(
        key = "FaqDetailViewModel:$faqId"
    ) { FaqDetailViewModel(faqId) }
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    onNavigateUp?.let { navigateUp ->
        BackHandler { navigateUp() }
    }

    FaqDetailScreen(
        state = state,
        configuration = configuration,
        onNavigateUp = onNavigateUp,
        onRetryClicked = viewModel::onRetryClicked,
        onContactClicked = configuration.getMailToUrl()?.let { url ->
            { context.openUrl(url) }
        }
    )
}

/**
 * Stateless rendering of the FAQ detail screen for a given [state]: loading spinner, error
 * screen with retry, or the loaded FAQ content.
 *
 * @param state The current FAQ detail load state to render.
 * @param configuration UI configuration, including whether/how to offer a contact-support link.
 * @param onNavigateUp Invoked when the back button is tapped. When `null`, no back button is
 * shown.
 * @param onRetryClicked Invoked when the retry button is tapped in the error state.
 * @param onContactClicked Invoked when the contact-support link is tapped. When `null`, no
 * contact link is shown.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FaqDetailScreen(
    state: FaqDetailViewModel.State,
    configuration: FaqUiConfiguration,
    onNavigateUp: (() -> Unit)? = null,
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
        ) {
            when (state) {
                FaqDetailViewModel.State.Loading ->
                    LoadingContent()

                FaqDetailViewModel.State.Error ->
                    ErrorContent(
                        onRetryClicked = onRetryClicked
                    )

                is FaqDetailViewModel.State.Success ->
                    Detail(
                        state = state,
                        configuration = configuration,
                        onContactClicked = onContactClicked,
                    )
            }
        }
    }
}

/**
 * Renders the loaded FAQ's full content with an optional [FaqContactInfo] block below it.
 *
 * @param state The successfully loaded FAQ state.
 * @param configuration UI configuration used to render [FaqContactInfo] when [onContactClicked]
 * is non-null.
 * @param onContactClicked Invoked when the contact-support link is tapped. When `null`, no
 * contact link is shown.
 */
@Composable
private fun Detail(
    state: FaqDetailViewModel.State.Success,
    configuration: FaqUiConfiguration,
    onContactClicked: (() -> Unit)? = null,
) {
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
                verticalArrangement = Arrangement.spacedBy(20.dp),
                modifier = Modifier.padding(16.dp),
            ) {
                Text(
                    text = state.faqItem.title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )

                Text(
                    text = state.faqItem.body,
                    style = MaterialTheme.typography.bodyLarge,
                )

                onContactClicked?.let {
                    FaqContactInfo(
                        configuration = configuration,
                        onClicked = onContactClicked,
                        modifier = Modifier.padding(top = 16.dp)
                    )
                }
            }
        }
    }
}

/** Preview of [FaqDetailScreen] without a contact-support link. */
@Preview
@Composable
private fun DetailsWithoutContactPreview() {
    FaqDetailScreen(
        state = FaqDetailViewModel.State.Success(
            faqItem = Faq.empty().copy(
                title = "Sample question?",
                body = "This is a sample question body.",
            ),
        ),
        configuration = FaqUiConfiguration(),
        onContactClicked = null,
    )
}

/** Preview of [FaqDetailScreen] with a contact-support link. */
@Preview
@Composable
private fun DetailsWithContactPreview() {
    FaqDetailScreen(
        state = FaqDetailViewModel.State.Success(
            faqItem = Faq.empty().copy(
                title = "Sample question?",
                body = "This is a sample question body.",
            ),
        ),
        configuration = FaqUiConfiguration(
            recipient = "support@mobilejoe.dev",
            contactSupportHint = "Still questions? Mail us!",
            contactSupportMailToLabel = "Send Mail",
        ),
        onContactClicked = {},
    )
}