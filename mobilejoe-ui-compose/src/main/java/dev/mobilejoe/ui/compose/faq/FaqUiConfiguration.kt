// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.ui.compose.faq

import android.content.Context
import android.net.Uri
import dev.mobilejoe.ui.compose.R

/**
 * UI configuration for the FAQ feature ([dev.mobilejoe.ui.compose.faq.FaqHost],
 * [dev.mobilejoe.ui.compose.faq.list.FaqListScreen], [dev.mobilejoe.ui.compose.faq.detail.FaqDetailScreen]):
 * controls whether and how a "contact support" mail-to link is offered.
 *
 * @property recipient The mail-to recipient address for the contact-support link. When `null`
 * or empty, [getMailToUrl] returns `null` and no contact link is shown.
 * @property contactSupportMailToLabel The tappable label text for the mail-to link.
 * @property contactSupportHint The hint text shown alongside the mail-to link.
 * @property subject The pre-filled subject line for the mail-to link, if any.
 * @property body The pre-filled body text for the mail-to link, if any.
 */
data class FaqUiConfiguration(
    val recipient: String?,
    val contactSupportMailToLabel: String,
    val contactSupportHint: String,
    val subject: String? = null,
    val body: String? = null,
) {

    /**
     * Creates configuration for FAQ UI with a contact-support mail-to link, sourcing the
     * label and hint strings from Android string resources.
     *
     * @param context Context used to resolve the label and hint string resources.
     * @param recipient The mail-to recipient address for the contact-support link.
     * @param subject The pre-filled subject line for the mail-to link, if any.
     * @param body The pre-filled body text for the mail-to link, if any.
     */
    constructor(
        context: Context,
        recipient: String,
        subject: String? = null,
        body: String? = null
    ) : this(
        recipient = recipient,
        subject = subject,
        body = body,
        contactSupportMailToLabel = context.getString(R.string.faq_contact_support_mail_button),
        contactSupportHint = context.getString(R.string.faq_contact_support_mail_hint)
    )

    /**
     * Creates configuration for FAQ UI which will not provide a contact button.
     */
    constructor() : this(recipient = null, contactSupportMailToLabel = "", contactSupportHint = "")

    /**
     * Builds the `mailto:` URI for the contact-support link, or `null` when no [recipient] is
     * configured.
     *
     * @return A `mailto:` [Uri] addressed to [recipient] with [subject] and [body] as query
     * parameters, or `null` if [recipient] is `null` or empty.
     */
    fun getMailToUrl(): Uri? {
        return recipient
            ?.takeIf(String::isNotEmpty)
            ?.let {
                Uri.Builder()
                    .scheme("mailto")
                    .path(recipient)
                    .appendQueryParameter("subject", subject)
                    .appendQueryParameter("body", body.orEmpty())
                    .build()
            }
    }
}
