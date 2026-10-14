// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.ui.compose.faq.contact

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.mobilejoe.ui.compose.faq.FaqUiConfiguration

/**
 * Renders the "still have questions? contact us" hint text and a tappable mail-to label.
 *
 * @param configuration UI configuration supplying the hint and mail-to label text.
 * @param modifier Modifier applied to the root [Column].
 * @param onClicked Invoked when the mail-to label is tapped.
 */
@Composable
internal fun FaqContactInfo(
    configuration: FaqUiConfiguration,
    modifier: Modifier = Modifier,
    onClicked: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = modifier) {
        Text(
            text = configuration.contactSupportHint,
            style = MaterialTheme.typography.bodyLarge,
        )

        Text(
            text = configuration.contactSupportMailToLabel,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clickable(onClick = onClicked)
                .padding(vertical = 4.dp)
        )
    }
}

/** Preview of [FaqContactInfo]. */
@Preview
@Composable
private fun Preview() {
    FaqContactInfo(
        configuration = FaqUiConfiguration(
            recipient = "support@mobilejoe.dev",
            contactSupportHint = "Still questions? Mail us!",
            contactSupportMailToLabel = "Send Mail",
        ),
        onClicked = {},
    )
}
