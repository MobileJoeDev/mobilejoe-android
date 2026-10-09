// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.samplejoe

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Accent = Color(0xFFFF8861)
private val AccentDark = Color(0xFFB5462A)
private val AccentLight = Color(0xFFFFDBD1)
private val AccentDeep = Color(0xFF5C1A0B)
private val AccentMuted = Color(0xFF7A2E1A)

/**
 * The light-mode [androidx.compose.material3.ColorScheme] used by [SampleTheme].
 *
 * Only overrides the accent tones and the handful of surface/background colors the sample
 * screens actually rely on (white surfaces, a light-gray background); everything else falls
 * back to Material 3's default light palette from [lightColorScheme].
 */
private val LightColorScheme = lightColorScheme(
    primary = AccentDark,
    primaryContainer = AccentLight,
    onPrimaryContainer = AccentDeep,
    surface = Color.White,
    surfaceContainer = Color.White,
    background = Color(0xFFEDEDED),
)

/**
 * The dark-mode [androidx.compose.material3.ColorScheme] used by [SampleTheme].
 *
 * Note: this is built with [lightColorScheme] (not `darkColorScheme`) since only a handful
 * of colors need overriding for the dark look; the remaining defaults come from the light
 * scheme's baseline.
 */
private val DarkColorScheme = lightColorScheme(
    primary = Accent,
    onPrimary = AccentDeep,
    primaryContainer = AccentMuted,
    onPrimaryContainer = AccentLight,
    surface = Color(0xFF191919),
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFb2b2b2),
    outlineVariant = Color(0xFF3A3A3A),
    surfaceContainer = Color(0xFF191919),
    surfaceContainerHigh = Color(0xFF2B2930),
    background = Color.Black,
)

/**
 * Root theme wrapper for the sample app.
 *
 * Applies [MaterialTheme] with either [LightColorScheme] or [DarkColorScheme] selected based
 * on [darkTheme], then renders [content] inside it. [MainActivity] wraps its entire Compose
 * tree — [SampleNavHost] and everything it navigates to — in a single call to this function,
 * so every screen in the app picks up the same color scheme automatically.
 *
 * @param darkTheme whether to use the dark color scheme; defaults to the system setting via
 *   [isSystemInDarkTheme].
 * @param content the themed content to render underneath this theme.
 */
@Composable
fun SampleTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (!darkTheme) {
        LightColorScheme
    } else {
        DarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}