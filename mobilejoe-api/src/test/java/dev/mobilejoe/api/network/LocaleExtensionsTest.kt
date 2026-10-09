// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.network

import android.os.LocaleList
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import java.util.Locale
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocaleExtensionsTest {

    private fun localeListOf(vararg locales: Locale): LocaleList = mockk {
        every { size() } returns locales.size
        every { get(any<Int>()) } answers { locales[firstArg<Int>()] }
    }

    @Test
    fun `Test toRegionalLanguageTag keeps a language-only tag unchanged`() {
        assertEquals("en", Locale.forLanguageTag("en").toRegionalLanguageTag())
    }

    @Test
    fun `Test toRegionalLanguageTag keeps a language-region tag unchanged`() {
        assertEquals("pt-BR", Locale.forLanguageTag("pt-BR").toRegionalLanguageTag())
        assertEquals("en-GB", Locale.forLanguageTag("en-GB").toRegionalLanguageTag())
    }

    @Test
    fun `Test toRegionalLanguageTag keeps a language-script-region tag unchanged`() {
        assertEquals("zh-Hant-TW", Locale.forLanguageTag("zh-Hant-TW").toRegionalLanguageTag())
    }

    @Test
    fun `Test toRegionalLanguageTag keeps a region-only numeric area code unchanged`() {
        assertEquals("es-419", Locale.forLanguageTag("es-419").toRegionalLanguageTag())
    }

    @Test
    fun `Test toRegionalLanguageTag keeps older script-less region tags unchanged`() {
        assertEquals("zh-TW", Locale.forLanguageTag("zh-TW").toRegionalLanguageTag())
        assertEquals("zh-CN", Locale.forLanguageTag("zh-CN").toRegionalLanguageTag())
    }

    @Test
    fun `Test toRegionalLanguageTag strips a Unicode extension`() {
        assertEquals(
            "en-US",
            Locale.forLanguageTag("en-US-u-ca-islamic-nu-latn").toRegionalLanguageTag(),
        )
    }

    @Test
    fun `Test toRegionalLanguageTag strips a variant`() {
        assertEquals("ca-ES", Locale.forLanguageTag("ca-ES-valencia").toRegionalLanguageTag())
    }

    @Test
    fun `Test toRegionalLanguageTag returns null for a non-ISO region`() {
        // The legacy constructor doesn't validate its arguments, unlike Locale.Builder/forLanguageTag,
        // so it can produce a Locale whose country Locale.Builder.setRegion() then rejects.
        @Suppress("DEPRECATION")
        val malformedLocale = Locale("en", "USA")

        assertNull(malformedLocale.toRegionalLanguageTag())
    }

    @Test
    fun `Test toRegionalLanguageTag returns null for Locale ROOT instead of the undetermined tag`() {
        // Locale.Builder treats a blank language as merely unset, not malformed, and happily
        // serializes it as "und" (BCP 47's undetermined-language tag) rather than throwing.
        assertNull(Locale.ROOT.toRegionalLanguageTag())
    }

    @Test
    fun `Test toRegionalLanguageTags keeps priority order`() {
        val locales = localeListOf(
            Locale.forLanguageTag("de-DE"),
            Locale.forLanguageTag("en-GB"),
            Locale.forLanguageTag("zh-Hant-TW"),
        )

        assertEquals(listOf("de-DE", "en-GB", "zh-Hant-TW"), locales.toRegionalLanguageTags())
    }

    @Test
    fun `Test toRegionalLanguageTags drops invalid tags`() {
        @Suppress("DEPRECATION")
        val locales = localeListOf(
            Locale.forLanguageTag("de-DE"),
            Locale("en", "USA"),
            Locale.forLanguageTag("en-GB"),
        )

        assertEquals(listOf("de-DE", "en-GB"), locales.toRegionalLanguageTags())
    }

    @Test
    fun `Test toRegionalLanguageTags drops duplicates, keeping the first occurrence`() {
        val locales = localeListOf(
            Locale.forLanguageTag("en-US"),
            Locale.forLanguageTag("de-DE"),
            Locale.forLanguageTag("en-US-u-ca-islamic-nu-latn"),
        )

        assertEquals(listOf("en-US", "de-DE"), locales.toRegionalLanguageTags())
    }

    @Test
    fun `Test toRegionalLanguageTags returns an empty list when no locale is valid`() {
        @Suppress("DEPRECATION")
        val locales = localeListOf(Locale("en", "USA"))

        assertTrue(locales.toRegionalLanguageTags().isEmpty())
    }

    @Test
    fun `Test toRegionalLanguageTags returns an empty list for an empty locale list`() {
        assertTrue(localeListOf().toRegionalLanguageTags().isEmpty())
    }
}
