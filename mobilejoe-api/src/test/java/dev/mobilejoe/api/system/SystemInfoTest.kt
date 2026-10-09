// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.system

import android.content.Context
import android.content.pm.PackageInfo
import android.os.LocaleList
import androidx.core.content.pm.PackageInfoCompat
import dev.mobilejoe.api.BuildConfig
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import org.junit.Test
import kotlin.test.assertEquals

class SystemInfoTest {

    @Test
    fun `Test system info from context`() {
        mockkStatic("dev.mobilejoe.api.system.SystemInfoKt")

        every { getBuildModel() } returns "Test Device"
        every { getBuildBrand() } returns "Google"
        every { getBuildVersionSdkInt() } returns 42

        val packageInfo = PackageInfo()
        packageInfo.versionName = "42.4.2"

        val localeList = mockk<LocaleList>()

        @Suppress("UsePropertyAccessSyntax")
        val context = mockk<Context> {
            every { getPackageManager() } returns mockk {
                every { getPackageName() } returns ""
                every { getPackageInfo(any<String>(), any<Int>()) } returns packageInfo
                every { resources } returns mockk {
                    every { configuration } returns mockk {
                        every { locales } returns localeList
                    }
                }
            }
        }

        mockkStatic(PackageInfoCompat::class)
        every { PackageInfoCompat.getLongVersionCode(any()) } returns 4242

        val systemInfo = SystemInfo.of(context)

        assertEquals(
            SystemInfo(
                clientVersionCode = 4242,
                clientVersionName = "42.4.2",
                platformVersion = 42,
                version = BuildConfig.MOBILE_JOE_VERSION,
                model = "Test Device",
                brand = "Google",
                locales = localeList
            ),
            systemInfo
        )
    }
}
