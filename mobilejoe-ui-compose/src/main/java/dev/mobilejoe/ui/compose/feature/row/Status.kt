// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.ui.compose.feature.row

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import dev.mobilejoe.api.feature.FeaturePlan
import dev.mobilejoe.api.feature.FeaturePlans
import dev.mobilejoe.ui.compose.R

/**
 * The localized user-facing label for this feature plan status.
 *
 * @receiver The status to label.
 * @return The localized status label string.
 */
@Composable
fun FeaturePlan.Status.label(): String {
    return stringResource(
        when (this) {
            FeaturePlan.Status.OPEN -> R.string.feature_plan_status_open
            FeaturePlan.Status.UNDER_REVIEW -> R.string.feature_plan_status_under_review
            FeaturePlan.Status.PLANNED -> R.string.feature_plan_status_planned
            FeaturePlan.Status.IN_PROGRESS -> R.string.feature_plan_status_in_progress
            FeaturePlan.Status.COMPLETED -> R.string.feature_plan_status_completed
            FeaturePlan.Status.CLOSED -> R.string.feature_plan_status_closed
        }
    )
}

/**
 * The accent color used to represent this feature plan status in row/badge UI (e.g. green for
 * [FeaturePlan.Status.OPEN], amber for [FeaturePlan.Status.UNDER_REVIEW]).
 *
 * @receiver The status to color.
 */
val FeaturePlan.Status.color: Color
    get() = when (this) {
        FeaturePlan.Status.OPEN -> Color(0xFF22C55E) // green-500
        FeaturePlan.Status.UNDER_REVIEW -> Color(0xFFF59E0B) // amber-500
        FeaturePlan.Status.PLANNED -> Color(0xFF3B82F6) // blue-500
        FeaturePlan.Status.IN_PROGRESS -> Color(0xFF8B5CF6) // violet-500
        FeaturePlan.Status.COMPLETED -> Color(0xFF10B981) // emerald-500
        FeaturePlan.Status.CLOSED -> Color(0xFF6B7280) // gray-500
    }

@Composable
internal fun FeaturePlan.Status.textColor(): Color {
    return colorResource(
        when (this) {
            FeaturePlan.Status.OPEN -> R.color.feature_plan_status_open_text
            FeaturePlan.Status.UNDER_REVIEW -> R.color.feature_plan_status_under_review_text
            FeaturePlan.Status.PLANNED -> R.color.feature_plan_status_planned_text
            FeaturePlan.Status.IN_PROGRESS -> R.color.feature_plan_status_in_progress_text
            FeaturePlan.Status.COMPLETED -> R.color.feature_plan_status_completed_text
            FeaturePlan.Status.CLOSED -> R.color.feature_plan_status_closed_text
        }
    )
}

/**
 * The localized user-facing label for this status filter option, shown in the feature plan
 * list's status filter chip. Multi-status filters ([FeaturePlans.StatusFilter.ALL],
 * [FeaturePlans.StatusFilter.ROADMAP]) use their own dedicated labels; single-status filters
 * reuse the corresponding [FeaturePlan.Status.label].
 *
 * @receiver The status filter to label.
 * @return The localized filter label string.
 */
@Composable
fun FeaturePlans.StatusFilter.label(): String {
    return when (this) {
        FeaturePlans.StatusFilter.ALL -> stringResource(R.string.feature_plan_status_all)
        FeaturePlans.StatusFilter.UNDER_REVIEW -> FeaturePlan.Status.UNDER_REVIEW.label()
        FeaturePlans.StatusFilter.PLANNED -> FeaturePlan.Status.PLANNED.label()
        FeaturePlans.StatusFilter.IN_PROGRESS -> FeaturePlan.Status.IN_PROGRESS.label()
        FeaturePlans.StatusFilter.COMPLETED -> FeaturePlan.Status.COMPLETED.label()
        FeaturePlans.StatusFilter.ROADMAP -> stringResource(R.string.feature_plan_status_roadmap)
    }
}