// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.ui.compose.util

import android.content.Context
import android.content.Intent
import android.net.Uri

/**
 * Launches an [Intent.ACTION_VIEW] intent for [uri], letting the system pick a handler (e.g.
 * the default mail app for a `mailto:` link, or a browser for `http(s)`).
 *
 * @receiver The context to start the activity from.
 * @param uri The URI to open.
 * @throws android.content.ActivityNotFoundException if no activity on the device can handle
 * [uri].
 */
fun Context.openUrl(uri: Uri) {
    val intent = Intent(Intent.ACTION_VIEW, uri)
    startActivity(intent)
}