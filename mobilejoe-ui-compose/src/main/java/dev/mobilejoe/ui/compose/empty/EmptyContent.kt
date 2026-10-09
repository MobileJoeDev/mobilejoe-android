// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.ui.compose.empty

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.mobilejoe.ui.compose.R
import dev.mobilejoe.ui.compose.error.ErrorContent

/**
 * Renders the standard "no items" placeholder, reusing [ErrorContent]'s layout with a
 * generic empty-state message and no retry button (since there is nothing to retry).
 *
 * @param modifier Modifier applied to the root layout.
 */
@Composable
fun EmptyContent(modifier: Modifier = Modifier) {
    ErrorContent(description = stringResource(R.string.generic_empty), modifier = modifier)
}