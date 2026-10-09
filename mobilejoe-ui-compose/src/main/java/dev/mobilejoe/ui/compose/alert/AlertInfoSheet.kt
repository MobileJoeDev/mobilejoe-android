// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.ui.compose.alert

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults.cardColors
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.mobilejoe.api.alert.Alert
import dev.mobilejoe.ui.compose.R
import dev.mobilejoe.ui.compose.util.getFullDateFormat
import java.text.DateFormat
import android.text.format.DateFormat as AndroidDateFormat

/**
 * Renders the full detail content for an [Alert]: its warning icon, title, optional message,
 * and the formatted date/time it [Alert.occurred]. Intended to be hosted inside a bottom
 * sheet (see [AlertsView]) when a user taps an [AlertsBanner].
 *
 * @param alert The alert whose details are shown.
 * @param modifier Modifier applied to the root [Column].
 * @param dateFormat Formatter used to render [Alert.occurred]'s date portion; defaults to the
 * device's full date format.
 * @param timeFormat Formatter used to render [Alert.occurred]'s time portion; defaults to the
 * device's system time format.
 */
@Composable
fun AlertInfoSheet(
    alert: Alert,
    modifier: Modifier = Modifier,
    dateFormat: DateFormat = getFullDateFormat(LocalContext.current),
    timeFormat: DateFormat = AndroidDateFormat.getTimeFormat(LocalContext.current),
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.Start,
        modifier = modifier.padding(16.dp)
    ) {
        Card(
            colors = cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp, horizontal = 16.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_warning_24dp_black),
                    contentDescription = stringResource(R.string.alert_warning),
                    tint = alert.kind.iconColor(),
                    modifier = Modifier
                        .size(60.dp)
                        .align(Alignment.CenterHorizontally)
                )

                Text(
                    alert.title,
                    style = TextStyle(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                )

                alert.message?.let { message ->
                    Text(
                        text = message,
                        style = TextStyle(
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        ),
                    )
                }
            }
        }

        Text(
            text = stringResource(
                R.string.alert_occurred,
                dateFormat.format(alert.occurred.toEpochMilli()),
                timeFormat.format(alert.occurred.toEpochMilli())
            ),
            style = TextStyle(fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant),
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

/**
 * The large warning icon's tint for this alert kind, used in the detail sheet.
 *
 * @receiver The alert kind to resolve a color for.
 * @return The icon tint [Color] for this kind.
 */
@Composable
private fun Alert.Kind.iconColor(): Color =
    when (this) {
        Alert.Kind.WARNING -> colorResource(R.color.alert_info_warning_icon)
        Alert.Kind.ERROR -> colorResource(R.color.alert_info_error_icon)
    }


/** Preview of [AlertInfoSheet] rendering a warning alert with a message. */
@Composable
@Preview
private fun PreviewAlert() {
    AlertInfoSheet(
        alert = Alert.empty().copy(
            kind = Alert.Kind.WARNING,
            title = "Please be patient and stay tuned for further updates. We're working hard.",
            message = "Note that we working on our system."
        )
    )
}


/** Preview of [AlertInfoSheet] rendering an error alert with a message. */
@Composable
@Preview
private fun PreviewError() {
    AlertInfoSheet(
        alert = Alert.empty().copy(
            kind = Alert.Kind.ERROR,
            title = "Please be patient and stay tuned for further updates. We're working hard.",
            message = "Some servers are down."
        )
    )
}