// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.ui.compose.feature

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.mobilejoe.api.feature.FeaturePlans
import dev.mobilejoe.ui.compose.feature.detail.FeaturePlanDetailScreen
import dev.mobilejoe.ui.compose.feature.detail.FeaturePlanDetailViewModel
import dev.mobilejoe.ui.compose.feature.list.FeaturePlanListScreen
import dev.mobilejoe.ui.compose.feature.list.FeaturePlanListViewModel
import kotlinx.serialization.Serializable

/**
 * Self-contained navigation host for the feature-plan feature, switching between the feature
 * plan list and a single feature plan's detail screen.
 *
 * @param configuration UI configuration used when composing the "suggest a feature" mail-to
 * link on the list screen.
 * @param modifier Modifier applied to the [NavHost].
 * @param onNavigateUp Invoked when the user navigates up from the list screen (the host's own
 * top-level "leave feature plans" action). Detail-to-list navigation is handled internally.
 * @param selectedFeatureId When non-null, the host starts directly on that feature plan's
 * detail screen instead of the list.
 * @param navController The navigation controller driving this host; defaults to a new
 * controller scoped to this composable.
 * @param featurePlans Creates the [FeaturePlans] data source. The host calls it once and shares
 * the instance between the list and the detail screen, so a vote cast in one shows in the other.
 */
@Composable
fun FeaturePlanHost(
    configuration: FeaturePlanUiConfiguration,
    modifier: Modifier = Modifier,
    onNavigateUp: (() -> Unit)? = null,
    selectedFeatureId: Int? = null,
    navController: NavHostController = rememberNavController(),
    featurePlans: () -> FeaturePlans = { FeaturePlans() },
) {
    NavHost(
        navController = navController,
        startDestination = selectedFeatureId?.let { FeaturePlanRoute.Detail(it) } ?: FeaturePlanRoute.List,
        modifier = modifier,
    ) {
        composable<FeaturePlanRoute.List> { backStackEntry ->
            val sharedFeaturePlans =
                rememberSharedFeaturePlans(navController, backStackEntry, featurePlans)

            FeaturePlanListScreen(
                configuration = configuration,
                onNavigateUp = onNavigateUp,
                onNavigateToDetails = { featurePlan ->
                    navController.navigate(FeaturePlanRoute.Detail(featurePlan.id))
                },
                viewModel = viewModel { FeaturePlanListViewModel(sharedFeaturePlans) },
            )
        }
        composable<FeaturePlanRoute.Detail> { backStackEntry ->
            val detailRoute = backStackEntry.toRoute<FeaturePlanRoute.Detail>()
            val sharedFeaturePlans =
                rememberSharedFeaturePlans(navController, backStackEntry, featurePlans)

            FeaturePlanDetailScreen(
                featurePlanId = detailRoute.featurePlanId,
                onNavigateUp = {
                    val hasPopBackToList = navController.popBackStack(
                        route = FeaturePlanRoute.List,
                        inclusive = false
                    )
                    if (!hasPopBackToList) {
                        navController.navigate(FeaturePlanRoute.List) {
                            popUpTo(FeaturePlanRoute.Detail::class) { inclusive = true }
                        }
                    }
                },
                viewModel = viewModel {
                    FeaturePlanDetailViewModel(detailRoute.featurePlanId, sharedFeaturePlans)
                },
            )
        }
    }
}

/**
 * Returns the [FeaturePlans] instance shared by all screens of one [FeaturePlanHost].
 *
 * The instance is scoped to the host's navigation graph: it survives configuration changes
 * and is released together with the host's back stack.
 *
 * @param navController The navigation controller driving the host.
 * @param backStackEntry The entry of the calling destination. The graph entry is looked up once
 * per entry, so a destination that is animating out doesn't look it up again.
 * @param featurePlans Creates the instance on first use.
 */
@Composable
private fun rememberSharedFeaturePlans(
    navController: NavHostController,
    backStackEntry: NavBackStackEntry,
    featurePlans: () -> FeaturePlans,
): FeaturePlans {
    val graphEntry = remember(backStackEntry) {
        navController.getBackStackEntry(navController.graph.id)
    }

    return viewModel(viewModelStoreOwner = graphEntry) {
        SharedFeaturePlansViewModel(featurePlans())
    }.featurePlans
}

/** Keeps the [FeaturePlans] instance of one [FeaturePlanHost] alive across its screens. */
private class SharedFeaturePlansViewModel(val featurePlans: FeaturePlans) : ViewModel()

/**
 * The navigation destinations within [FeaturePlanHost]'s internal [NavHost].
 *
 * Each destination is `@Serializable` so it can be used directly as a Navigation-Compose route
 * object: [FeaturePlanHost] passes [List] or [Detail] as its `startDestination`, and navigates
 * between the two as the user drills into or backs out of a feature plan.
 */
private sealed interface FeaturePlanRoute {

    /** The feature plan list screen. */
    @Serializable
    data object List : FeaturePlanRoute

    /**
     * A single feature plan's detail screen.
     *
     * @property featurePlanId The id of the feature plan to display.
     */
    @Serializable
    data class Detail(val featurePlanId: Int) : FeaturePlanRoute
}