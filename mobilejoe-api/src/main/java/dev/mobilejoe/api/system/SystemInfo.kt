// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.api.system

import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.core.content.pm.PackageInfoCompat
import dev.mobilejoe.api.BuildConfig

/**
 * Device and host-app metadata sent with every SDK request, so the backend can attribute
 * traffic to a client version/platform.
 *
 * @param clientVersionCode The host app's version code.
 * @param clientVersionName The host app's version name.
 * @param platformVersion The Android SDK level the device is running.
 * @param version The MobileJoe SDK's own version.
 * @param model The device model, e.g. `"Pixel 8"`.
 * @param brand The device brand, e.g. `"google"`.
 * @param locales The device's current locales.
 */
data class SystemInfo(
    val clientVersionCode: Long,
    val clientVersionName: String,
    val platformVersion: Int,
    val version: String,
    val model: String,
    val brand: String,
    val locales: LocaleList,
) {

    companion object Companion {

        /**
         * Reads [SystemInfo] from [context]'s package info, device [Build] properties, and the
         * SDK's own [BuildConfig].
         *
         * @param context The context used to read the host app's package info and locale.
         * @return The current device/app metadata.
         * @throws android.content.pm.PackageManager.NameNotFoundException if [context]'s own
         * package info can't be found.
         */
        fun of(context: Context): SystemInfo {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)

            return SystemInfo(
                clientVersionCode = PackageInfoCompat.getLongVersionCode(packageInfo),
                clientVersionName = packageInfo.versionName ?: "Unknown",
                model = getBuildModel(),
                brand = getBuildBrand(),
                platformVersion = getBuildVersionSdkInt(),
                version = BuildConfig.MOBILE_JOE_VERSION,
                locales = context.resources.configuration.locales,
            )

        }
    }
}

/**
 * Reads the device's model name directly off the platform. Broken out as its own top-level
 * function (rather than inlined into [SystemInfo.of]) purely so it can be mocked in tests without
 * needing a real [android.os.Build].
 *
 * @return The current device's model name ([Build.MODEL]).
 */
internal fun getBuildModel(): String = Build.MODEL

/**
 * Reads the device's brand name directly off the platform. Broken out as its own top-level
 * function for the same testability reason as [getBuildModel].
 *
 * @return The current device's brand name ([Build.BRAND]).
 */
internal fun getBuildBrand(): String = Build.BRAND

/**
 * Reads the device's Android SDK level directly off the platform. Broken out as its own top-level
 * function for the same testability reason as [getBuildModel].
 *
 * @return The current device's Android SDK level (`Build.VERSION.SDK_INT`).
 */
internal fun getBuildVersionSdkInt(): Int = Build.VERSION.SDK_INT
