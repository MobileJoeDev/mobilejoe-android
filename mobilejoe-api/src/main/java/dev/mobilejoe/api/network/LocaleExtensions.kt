// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.network

import android.os.LocaleList
import java.util.IllformedLocaleException
import java.util.Locale

/**
 * Builds a BCP 47 language tag for this [Locale] containing only language, script and region,
 * dropping any Unicode extensions (e.g. `-u-ca-islamic`) the backend doesn't understand.
 *
 * @return The stripped-down language tag, e.g. `"pt-BR"` or `"zh-Hant-TW"`, or `null` if
 * [language] is blank (e.g. [Locale.ROOT], which [Locale.Builder] would otherwise happily
 * serialize as the undetermined-language tag `"und"`) or if [Locale.Builder] rejects the
 * script/region, which can happen for a [Locale] built through the legacy, non-validating
 * constructors (e.g. a 3-letter country code where BCP 47 expects 2 letters or 3 digits).
 */
internal fun Locale.toRegionalLanguageTag(): String? {
    if (language.isBlank()) {
        return null
    }

    return try {
        Locale.Builder()
            .setLanguage(language)
            .setScript(script)
            .setRegion(country)
            .build()
            .toLanguageTag()
    } catch (e: IllformedLocaleException) {
        null
    }
}

/**
 * Builds the `Preferred-Languages` header value: each locale's [toRegionalLanguageTag], in this
 * list's priority order, dropping invalid tags and duplicates.
 *
 * @return The regional language tags, e.g. `["de-DE", "en-GB", "zh-Hant-TW"]`, or an empty list
 * if none of this list's locales produce a valid tag.
 */
internal fun LocaleList.toRegionalLanguageTags(): List<String> {
    return (0 until size())
        .map(::get)
        .map(Locale::toRegionalLanguageTag)
        .filterNotNull()
        .distinct()
}
