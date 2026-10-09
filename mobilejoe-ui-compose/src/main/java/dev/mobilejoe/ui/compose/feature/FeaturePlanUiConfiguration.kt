// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.ui.compose.feature

import android.content.Context
import android.net.Uri
import dev.mobilejoe.ui.compose.R

/**
 * UI configuration for the feature-plan feature: controls whether and how a "suggest a
 * feature" mail-to link is offered on [dev.mobilejoe.ui.compose.feature.list.FeaturePlanListScreen].
 *
 * @property recipient The mail-to recipient address for the suggest-a-feature link. When
 * `null` or empty, [getMailToUrl] returns `null` and no suggest button is shown.
 * @property subject The pre-filled subject line for the mail-to link. When `null`, a default
 * localized subject is used.
 * @property body The pre-filled body text for the mail-to link, if any.
 */
data class FeaturePlanUiConfiguration(
    val recipient: String? = null,
    val subject: String? = null,
    val body: String? = null,
) {

    /**
     * Builds the `mailto:` URI for the suggest-a-feature link, or `null` when no [recipient]
     * is configured.
     *
     * @param context Context used to resolve the default subject string resource when
     * [subject] is `null`.
     * @return A `mailto:` [Uri] addressed to [recipient] with subject and [body] as query
     * parameters, or `null` if [recipient] is `null` or empty.
     */
    fun getMailToUrl(context: Context): Uri? {
        return recipient
            ?.takeIf(String::isNotEmpty)
            ?.let {
                Uri.Builder()
                    .scheme("mailto")
                    .path(recipient)
                    .appendQueryParameter("subject", subject ?: context.getString(R.string.feature_plan_mail_subject))
                    .appendQueryParameter("body", body.orEmpty())
                    .build()
            }
    }
}
