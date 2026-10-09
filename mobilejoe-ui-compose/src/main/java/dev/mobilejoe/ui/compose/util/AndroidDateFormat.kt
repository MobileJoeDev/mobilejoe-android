// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.ui.compose.util

import android.content.Context
import java.text.DateFormat

/**
 * Builds a [DateFormat] that renders dates in the device's primary locale using
 * [DateFormat.FULL] style (e.g. "Wednesday, September 24, 2026").
 *
 * @param context Context used to resolve the device's primary configured locale.
 * @return A full-style date formatter for the device's primary locale.
 */
internal fun getFullDateFormat(context: Context): DateFormat {
    val locale = context.resources.configuration.locales.get(0)
    return DateFormat.getDateInstance(DateFormat.FULL, locale)
}
