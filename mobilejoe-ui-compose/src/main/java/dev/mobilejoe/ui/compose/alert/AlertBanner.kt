// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.ui.compose.alert

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults.cardColors
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.mobilejoe.api.alert.Alert
import dev.mobilejoe.ui.compose.R

/**
 * Renders a single [Alert] as a compact, tappable banner card.
 *
 * The card's background, icon, and text colors are derived from [Alert.kind] (warning vs.
 * error) and adapt to the current light/dark theme. When [onClick] is non-null, a chevron
 * affordance is shown on the trailing edge to indicate the banner can be tapped for more
 * detail (typically to open [AlertInfoSheet]).
 *
 * @param alert The alert to display.
 * @param modifier Modifier applied to the root [Card].
 * @param onClick Invoked when the banner is tapped. When `null`, no trailing chevron is shown
 * and taps are effectively inert.
 */
@Composable
fun AlertsBanner(alert: Alert, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Card(
        colors = cardColors(containerColor = alert.kind.backgroundColor()),
        shape = MaterialTheme.shapes.extraLarge,
        onClick = { onClick?.invoke() },
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 12.dp),
            ) {
                val titleTextStyle = TextStyle(
                    fontSize = 15.sp,
                    lineHeight = 1.2.em,
                )

                val textMeasurer = rememberTextMeasurer()
                val titleTextLayout = textMeasurer.measure(
                    text = alert.title,
                    style = titleTextStyle,
                )

                val iconSize = 16.dp
                Icon(
                    painter = painterResource(R.drawable.ic_warning_24dp_black),
                    contentDescription = stringResource(R.string.alert_warning),
                    tint = alert.kind.iconColor(),
                    modifier = Modifier
                        .size(iconSize)
                        .offset {
                            val firstLineTop = titleTextLayout.getLineTop(0)
                            val firstLineBottom = titleTextLayout.getLineBottom(0)
                            val firstLineCenter = (firstLineTop + firstLineBottom) / 2f

                            IntOffset(
                                x = 0,
                                y = (firstLineCenter - (iconSize.toPx() / 2)).toInt()
                            )
                        }
                )

                Text(
                    text = alert.title,
                    color = alert.kind.textColor(),
                    style = titleTextStyle,
                    modifier = Modifier
                        .weight(1f)
                        .alignBy(FirstBaseline),
                )
            }

            if (onClick != null) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.alert_more_info),
                    tint = alert.kind.textColor(),
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
        }
    }
}

/**
 * The banner card's background color for this alert kind.
 *
 * @receiver The alert kind to resolve a color for.
 * @return The background [Color] for this kind.
 */
@Composable
private fun Alert.Kind.backgroundColor(): Color =
    when (this) {
        Alert.Kind.WARNING -> colorResource(R.color.alert_warning_background)
        Alert.Kind.ERROR -> colorResource(R.color.alert_error_background)
    }

/**
 * The banner's title/text color for this alert kind. Unlike [backgroundColor] and
 * [iconColor], this does not vary with the system theme because it is chosen for contrast
 * against [backgroundColor] rather than the surrounding surface.
 *
 * @receiver The alert kind to resolve a color for.
 * @return The text [Color] for this kind.
 */
@Composable
private fun Alert.Kind.textColor(): Color =
    when (this) {
        Alert.Kind.WARNING ->
            Color.Black

        Alert.Kind.ERROR ->
            Color.White
    }

/**
 * The warning icon's tint for this alert kind.
 *
 * @receiver The alert kind to resolve a color for.
 * @return The icon tint [Color] for this kind.
 */
@Composable
private fun Alert.Kind.iconColor(): Color =
    when (this) {
        Alert.Kind.WARNING -> colorResource(R.color.alert_warning_icon)
        Alert.Kind.ERROR -> colorResource(R.color.alert_error_icon)
    }

/** Preview of [AlertsBanner] rendering a non-interactive warning alert. */
@Composable
@Preview
private fun PreviewAlert() {
    AlertsBanner(
        alert = Alert.empty().copy(
            kind = Alert.Kind.WARNING,
            title = "Please be patient and stay tuned for further updates. We're working hard."
        )
    )
}

/** Preview of [AlertsBanner] rendering a tappable error alert (with trailing chevron). */
@Composable
@Preview
private fun PreviewError() {
    AlertsBanner(
        alert = Alert.empty().copy(
            kind = Alert.Kind.ERROR,
            title = "Please be patient and stay tuned for further updates. We're working hard."
        ),
        onClick = {}
    )
}
