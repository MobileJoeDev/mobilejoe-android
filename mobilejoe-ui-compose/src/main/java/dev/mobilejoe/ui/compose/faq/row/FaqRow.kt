// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.ui.compose.faq.row

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardDefaults.cardColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.mobilejoe.api.faq.Faq
import dev.mobilejoe.ui.compose.util.applyIf

/**
 * Renders a single FAQ as a card showing its title.
 *
 * @param faq The FAQ to display.
 * @param modifier Modifier applied to the root [Card].
 * @param shape Shape of the root [Card].
 * @param onClicked Invoked when the card is tapped.
 * @param additionalContent Optional extra content rendered below the title (e.g. a
 * contact-support block). Also affects the card's bottom padding: when non-null, no extra
 * bottom padding is added since the additional content supplies its own spacing.
 */
@Composable
internal fun FaqRow(
    faq: Faq,
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.shape,
    onClicked: () -> Unit = {},
    additionalContent: @Composable (() -> Unit)? = null,
) {
    Card(
        colors = cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = shape,
        onClick = onClicked,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            Modifier
                .padding(top = 16.dp)
                .applyIf(additionalContent == null) {
                    Modifier.padding(bottom = 16.dp)
                }
        ) {
            Text(
                text = faq.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
            )

            additionalContent?.invoke()
        }
    }
}

/** Preview of [FaqRow]. */
@Preview
@Composable
private fun Preview() {
    FaqRow(
        Faq.empty().copy(
            title = "Sample question?",
            body = "This is a sample question body.",
        ),
    )
}