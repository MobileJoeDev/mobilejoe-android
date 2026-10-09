// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.samplejoe

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import dev.mobilejoe.api.faq.Faqs
import dev.mobilejoe.api.feature.FeaturePlans
import dev.mobilejoe.samplejoe.ui.alert.AlertsScreen
import dev.mobilejoe.samplejoe.ui.home.HomeScreen
import dev.mobilejoe.samplejoe.ui.home.SampleItem
import dev.mobilejoe.ui.compose.faq.FaqHost
import dev.mobilejoe.ui.compose.faq.FaqUiConfiguration
import dev.mobilejoe.ui.compose.feature.FeaturePlanHost
import dev.mobilejoe.ui.compose.feature.FeaturePlanUiConfiguration
import kotlinx.serialization.Serializable

/**
 * Type-safe navigation route for the app's home screen.
 *
 * This is a Navigation-Compose destination marker, not a data holder — it carries no
 * arguments and exists purely so [SampleNavHost] can register a `composable<Home>` route and
 * so other screens can call `navController.navigate(Home)` to get back to the list. The screen
 * itself, [HomeScreen], lists one card per [SampleItem].
 */
@Serializable
internal object Home

/**
 * Type-safe navigation route for the detail screen of a single sample feature.
 *
 * Unlike [Home], this route carries an argument: which [SampleItem] was tapped. [SampleNavHost]
 * reads that argument back out of the route to decide which library host composable —
 * [FeaturePlanHost], [FaqHost], or [AlertsScreen] — to render, and with what configuration.
 *
 * @property item the sample feature to display; determines whether [FeaturePlanHost], [FaqHost],
 *   or [AlertsScreen] is rendered and with which configuration.
 */
@Serializable
internal data class Sample(val item: SampleItem)

/**
 * The sample app's single navigation graph, and effectively the whole app's screen wiring in
 * one place.
 *
 * Builds a `NavHost` with exactly two destinations: [Home], which shows [HomeScreen]'s list of
 * demo entries, and [Sample], which — based on which [SampleItem] was tapped — renders one of
 * the library's own host screens: [FeaturePlanHost] (list or a specific detail, depending on
 * the item), [FaqHost] (list or a specific detail), or [AlertsScreen]. The
 * `FeaturePlanUiConfiguration`/`FaqUiConfiguration` values are created here and supplied to the
 * library screens. Every non-home destination delegates its "navigate up" callback to
 * `navController.navigateUp()`; the resulting destination depends on the controller's back
 * stack and is [Home] only when Home is present beneath it.
 *
 * @param modifier [Modifier] applied to the underlying `NavHost`.
 * @param navController the `NavHostController` driving navigation; defaults to a freshly
 *   remembered controller.
 * @param startDestination the route the graph starts at; defaults to [Home].
 */
@Composable
fun SampleNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: Any = Home,
) {
    val context = LocalContext.current

    val featurePlanUiConfiguration = FeaturePlanUiConfiguration(
        recipient = "support@mobilejoe.dev"
    )

    val faqUiConfiguration = FaqUiConfiguration(
        context = context,
        recipient = "support@mobilejoe.dev",
    )

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
    ) {
        composable<Home> {
            HomeScreen(
                onItemClicked = { sampleItem ->
                    navController.navigate(
                        route = Sample(item = sampleItem)
                    )
                },
            )
        }

        composable<Sample> { backStackEntry ->
            val route = backStackEntry.toRoute<Sample>()

            when (route.item) {
                SampleItem.FEATURE_PLANS ->
                    FeaturePlanHost(
                        configuration = featurePlanUiConfiguration,
                        onNavigateUp = { navController.navigateUp() },
                        featurePlans = FeaturePlans::preview,
                    )

                SampleItem.FEATURE_PLAN_DETAIL ->
                    FeaturePlanHost(
                        configuration = featurePlanUiConfiguration,
                        selectedFeatureId = 2,
                        onNavigateUp = { navController.navigateUp() },
                        featurePlans = FeaturePlans::preview,
                    )

                SampleItem.FAQS ->
                    FaqHost(
                        configuration = faqUiConfiguration,
                        onNavigateUp = { navController.navigateUp() },
                        faqs = Faqs::preview,
                    )

                SampleItem.FAQ_DETAIL ->
                    FaqHost(
                        configuration = faqUiConfiguration,
                        selectedFaqId = 1,
                        onNavigateUp = { navController.navigateUp() },
                        faqs = Faqs::preview,
                    )

                SampleItem.ALERTS ->
                    AlertsScreen(onNavigateUp = navController::navigateUp)
            }
        }
    }
}
