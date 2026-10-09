// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.ui.compose.feature.row

import android.icu.text.CompactDecimalFormat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardDefaults.cardColors
import androidx.compose.material3.CardDefaults.outlinedCardColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter.Companion.tint
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.mobilejoe.api.feature.FeaturePlan
import dev.mobilejoe.ui.compose.R
import java.text.DateFormat
import java.time.Instant
import android.text.format.DateFormat as AndroidDateFormat

/** Maximum number of body text lines shown in [FeaturePlanRow]. */
private const val COLLAPSED_LINE_COUNT = 2

/**
 * Renders a single feature plan as a card: title, status badge (when not
 * [FeaturePlan.Status.OPEN]), creation date, body text, and an upvote control.
 *
 * @param featurePlan The feature plan to display.
 * @param modifier Modifier applied to the root [Card].
 * @param shape Shape of the root [Card].
 * @param dateFormat Formatter used to render [FeaturePlan.createdAt]; defaults to the device's
 * long date format.
 * @param onClicked Invoked when the card is tapped.
 * @param onUpVoteClicked Invoked when the upvote control is tapped.
 */
@Composable
internal fun FeaturePlanRow(
    featurePlan: FeaturePlan,
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.shape,
    dateFormat: DateFormat = AndroidDateFormat.getLongDateFormat(LocalContext.current),
    onClicked: () -> Unit = {},
    onUpVoteClicked: () -> Unit = {},
) {
    Card(
        colors = cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = shape,
        onClick = onClicked,
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            FeaturePlanHeader(
                featurePlan = featurePlan,
                titleStyle = MaterialTheme.typography.titleMedium,
                titleToMetaSpacing = 6.dp,
                dateFormat = dateFormat,
                onUpVoteClicked = onUpVoteClicked,
            )

            Text(
                text = featurePlan.body,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = COLLAPSED_LINE_COUNT,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
internal fun FeaturePlanHeader(
    featurePlan: FeaturePlan,
    titleStyle: TextStyle,
    titleToMetaSpacing: Dp,
    dateFormat: DateFormat,
    onUpVoteClicked: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(titleToMetaSpacing),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = featurePlan.title,
                style = titleStyle,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (featurePlan.status != FeaturePlan.Status.OPEN) {
                    StatusBadge(featurePlan.status)
                }

                Text(
                    text = dateFormat.format(featurePlan.createdAt.toEpochMilli()),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        VoteButton(featurePlan = featurePlan, onClicked = onUpVoteClicked)
    }
}

@Composable
private fun StatusBadge(status: FeaturePlan.Status) {
    Card(
        colors = cardColors(
            containerColor = status.color.copy(alpha = 0.1f),
            contentColor = status.textColor()
        ),
        shape = RoundedCornerShape(6.dp),
    ) {
        Text(
            modifier = Modifier
                .padding(horizontal = 6.dp, vertical = 2.dp),
            text = status.label(),
            fontSize = 12.sp,
            lineHeight = 1.em,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun VoteButton(featurePlan: FeaturePlan, onClicked: () -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    val scoreFormat = remember(locale) {
        CompactDecimalFormat.getInstance(locale, CompactDecimalFormat.CompactStyle.SHORT)
    }

    OutlinedCard(
        onClick = onClicked,
        colors = outlinedCardColors(
            containerColor = if (featurePlan.isVoted)
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            else
                Color.Transparent,
            contentColor = if (featurePlan.isVoted)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurface,
        ),
        border = BorderStroke(
            1.dp, if (featurePlan.isVoted)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurfaceVariant
        ),
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(
                2.dp,
                Alignment.CenterVertically
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(8.dp)
                // 1:1 ratio
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)

                    val size = placeable.height
                    layout(size, size) {
                        // Center the content inside this square
                        val dx = (size - placeable.width) / 2
                        val dy = (size - placeable.height) / 2
                        placeable.placeRelative(dx, dy)
                    }
                }
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_chevron_up_24dp_black),
                contentDescription = stringResource(R.string.feature_plan_vote_up),
                colorFilter = if (featurePlan.isVoted)
                    tint(MaterialTheme.colorScheme.primary)
                else
                    tint(MaterialTheme.colorScheme.onSurface),
                modifier = Modifier.sizeIn(maxHeight = 6.dp)
            )

            Text(
                text = scoreFormat.format(featurePlan.score),
                fontSize = 16.sp,
                lineHeight = 1.em,
                maxLines = 1,
            )
        }
    }
}

/** Preview of [FeaturePlanRow] with unvoted state. */
@Preview
@Composable
private fun OpenUnvotedPreview() {
    FeaturePlanRow(
        FeaturePlan.empty().copy(
            title = "Sample Feature",
            body = "This is a sample feature plan body.",
            score = 42,
            status = FeaturePlan.Status.OPEN,
            createdAt = Instant.ofEpochMilli(1754259294096),
            isVoted = false
        ),
    )
}

/** Preview of [FeaturePlanRow] with voted state. */
@Preview
@Composable
private fun PlannedVotedPreview() {
    FeaturePlanRow(
        FeaturePlan.empty().copy(
            title = "Sample Feature",
            body = "This is a sample feature plan body.",
            score = 42,
            status = FeaturePlan.Status.PLANNED,
            createdAt = Instant.ofEpochMilli(1754259294096),
            isVoted = true
        ),
    )
}
