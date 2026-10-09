// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.ui.compose.error

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mobilejoe.ui.compose.R
import dev.mobilejoe.ui.compose.images.JoeImages
import dev.mobilejoe.ui.compose.images.rememberJoeSignX

/**
 * Renders the shared full-screen placeholder used for both error and empty states: the
 * "Joe sign sorry" illustration, a title, a description, and an optional retry button.
 *
 * Also used indirectly via [dev.mobilejoe.ui.compose.empty.EmptyContent] for empty-list states.
 *
 * @param modifier Modifier applied to the root [Column]; fills the available size by default.
 * @param description The body text explaining what went wrong or is missing; defaults to a
 * generic error message.
 * @param onRetryClicked Invoked when the retry button is tapped. When `null`, no retry button
 * is shown.
 */
@Composable
internal fun ErrorContent(
    modifier: Modifier = Modifier,
    description: String = stringResource(R.string.generic_error),
    onRetryClicked: (() -> Unit)? = null
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.fillMaxSize()
    ) {
        Image(
            imageVector = JoeImages.rememberJoeSignX(),
            contentDescription = null,
            contentScale = ContentScale.Fit,
        )

        Text(
            text = stringResource(R.string.generic_sorry),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurface
        )

        Text(
            text = description,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        onRetryClicked?.let { onClicked ->
            Button(onClicked, modifier = Modifier.padding(top = 16.dp)) {
                Text(text = stringResource(R.string.generic_retry))
            }
        }
    }
}
