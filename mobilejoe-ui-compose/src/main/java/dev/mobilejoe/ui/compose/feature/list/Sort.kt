// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.ui.compose.feature.list

import androidx.compose.runtime.Composable
import dev.mobilejoe.api.feature.FeaturePlans

/**
 * The user-facing label for this sorting option, shown in the feature plan list's sort chip.
 *
 * @receiver The sorting option to label.
 * @return `"Newest"` for [FeaturePlans.Sorting.BY_NEWEST], `"Rating"` for
 * [FeaturePlans.Sorting.BY_SCORE].
 */
@Composable
fun FeaturePlans.Sorting.label(): String {
    return when (this) {
        FeaturePlans.Sorting.BY_NEWEST -> "Newest"
        FeaturePlans.Sorting.BY_SCORE -> "Rating"
    }
}