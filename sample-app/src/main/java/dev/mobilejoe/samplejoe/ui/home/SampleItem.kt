// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.samplejoe.ui.home

import androidx.annotation.StringRes
import kotlinx.serialization.Serializable
import dev.mobilejoe.ui.compose.R as JoeComposeR
import dev.mobilejoe.samplejoe.R as SampleR

/**
 * Identifies one demo entry on [HomeScreen] — one card in its list — and, via
 * [dev.mobilejoe.samplejoe.Sample], which
 * library host screen that card navigates to.
 *
 * Each constant pairs a display title ([titleId]) with the specific library feature it
 * demonstrates. [HomeScreen] renders one card per constant (by default every value in
 * [SampleItem.entries]); tapping a card wraps the tapped constant in a
 * [dev.mobilejoe.samplejoe.Sample] route, and [dev.mobilejoe.samplejoe.SampleNavHost]'s
 * `when` on this enum decides which library composable — [dev.mobilejoe.ui.compose.feature.FeaturePlanHost]
 * or [dev.mobilejoe.ui.compose.faq.FaqHost] — to render, and with what starting state.
 *
 * @property titleId string resource id shown as this item's title on the home list.
 */
@Serializable
internal enum class SampleItem(@param:StringRes val titleId: Int) {

    /**
     * Opens the feature plan list screen
     * ([dev.mobilejoe.ui.compose.feature.FeaturePlanHost]) with no feature pre-selected, i.e.
     * starting on the list itself.
     */
    FEATURE_PLANS(JoeComposeR.string.feature_plans),

    /**
     * Opens the same [dev.mobilejoe.ui.compose.feature.FeaturePlanHost] screen as
     * [FEATURE_PLANS], but pre-navigated straight to a single, hard-coded feature plan's
     * detail view (feature id `99`) instead of starting on the list — see
     * [dev.mobilejoe.samplejoe.SampleNavHost] for where that id is wired in.
     */
    FEATURE_PLAN_DETAIL(SampleR.string.feature_plan_selected),

    /**
     * Opens the FAQ list screen ([dev.mobilejoe.ui.compose.faq.FaqHost]) with no FAQ
     * pre-selected, i.e. starting on the list itself.
     */
    FAQS(JoeComposeR.string.faqs),

    /**
     * Opens the same [dev.mobilejoe.ui.compose.faq.FaqHost] screen as [FAQS], but
     * pre-navigated straight to a single, hard-coded FAQ's detail view (FAQ id `4`) instead of
     * starting on the list — see [dev.mobilejoe.samplejoe.SampleNavHost] for where that id is
     * wired in.
     */
    FAQ_DETAIL(SampleR.string.faq_selected),

    /**
     * Opens [dev.mobilejoe.samplejoe.ui.alert.AlertsScreen], the demo for the library's
     * standalone alerts banner rather than a "list + detail" feature host.
     */
    ALERTS(SampleR.string.alerts_title),
}
