// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.ui.compose.faq

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.mobilejoe.api.faq.Faqs
import dev.mobilejoe.ui.compose.faq.detail.FaqDetailScreen
import dev.mobilejoe.ui.compose.faq.detail.FaqDetailViewModel
import dev.mobilejoe.ui.compose.faq.list.FaqListScreen
import dev.mobilejoe.ui.compose.faq.list.FaqListViewModel
import kotlinx.serialization.Serializable

/**
 * Self-contained navigation host for the FAQ feature, switching between the FAQ list and a
 * single FAQ's detail screen.
 *
 * @param configuration UI configuration shared by the list and detail screens.
 * @param modifier Modifier applied to the [NavHost].
 * @param onNavigateUp Invoked when the user navigates up from the list screen (the host's own
 * top-level "leave FAQs" action). Detail-to-list navigation is handled internally.
 * @param selectedFaqId When non-null, the host starts directly on that FAQ's detail screen
 * instead of the list.
 * @param navController The navigation controller driving this host; defaults to a new
 * controller scoped to this composable.
 * @param faqs Creates the [Faqs] data source for each screen.
 */
@Composable
fun FaqHost(
    configuration: FaqUiConfiguration,
    modifier: Modifier = Modifier,
    onNavigateUp: (() -> Unit)? = null,
    selectedFaqId: Int? = null,
    navController: NavHostController = rememberNavController(),
    faqs: () -> Faqs = { Faqs() },
) {
    NavHost(
        navController = navController,
        startDestination = selectedFaqId?.let { FaqRoute.Detail(it) } ?: FaqRoute.List,
        modifier = modifier,
    ) {
        composable<FaqRoute.List> {
            FaqListScreen(
                configuration = configuration,
                onNavigateUp = onNavigateUp,
                onNavigateToDetails = { faq ->
                    navController.navigate(FaqRoute.Detail(faq.id))
                },
                viewModel = viewModel { FaqListViewModel(faqs()) },
            )
        }
        composable<FaqRoute.Detail> { backStackEntry ->
            val detailRoute = backStackEntry.toRoute<FaqRoute.Detail>()

            FaqDetailScreen(
                faqId = detailRoute.selectedFaqId,
                configuration = configuration,
                onNavigateUp = {
                    val hasPopBackToList = navController.popBackStack(
                        route = FaqRoute.List,
                        inclusive = false
                    )
                    if (!hasPopBackToList) {
                        navController.navigate(FaqRoute.List) {
                            popUpTo(FaqRoute.Detail::class) { inclusive = true }
                        }
                    }
                },
                viewModel = viewModel { FaqDetailViewModel(detailRoute.selectedFaqId, faqs()) },
            )
        }
    }
}

/**
 * The navigation destinations within [FaqHost]'s internal [NavHost].
 *
 * Each destination is `@Serializable` so it can be used directly as a Navigation-Compose route
 * object: [FaqHost] passes [List] or [Detail] as its `startDestination`, and navigates between
 * the two as the user drills into or backs out of a FAQ.
 */
private sealed interface FaqRoute {

    /** The FAQ list screen. */
    @Serializable
    data object List : FaqRoute

    /**
     * A single FAQ's detail screen.
     *
     * @property selectedFaqId The id of the FAQ to display.
     */
    @Serializable
    data class Detail(val selectedFaqId: Int) : FaqRoute
}