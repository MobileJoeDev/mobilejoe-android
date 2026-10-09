// Copyright Bytekontor GmbH.
// Licensed under the MIT License. See LICENSE for details.

package dev.mobilejoe.samplejoe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

/**
 * Single-activity entry point for the whole sample app.
 *
 * There is exactly one [ComponentActivity] in this app: every screen and every navigation
 * transition lives inside the Compose tree installed by [onCreate], rather than being split
 * across multiple activities. That tree is rooted at [SampleTheme], which supplies the app's
 * Material 3 color scheme, wrapping [SampleNavHost], which owns the actual navigation graph
 * between the home list and each library feature's detail screens.
 *
 * Because the whole UI is a single Compose screen, this class has almost no responsibility of
 * its own beyond process/lifecycle plumbing: enabling edge-to-edge rendering and calling
 * `setContent` once in [onCreate].
 */
class MainActivity : ComponentActivity() {

    /**
     * Standard Android activity-creation callback, called once per activity instance, including
     * instances recreated after configuration changes or process death.
     *
     * Calls `enableEdgeToEdge()` so the Compose content can draw behind the system status and
     * navigation bars, then installs the Compose UI tree via `setContent`, rooted at
     * [SampleTheme] wrapping [SampleNavHost]. No other setup happens here; SDK configuration
     * is handled separately in [SampleApp.onCreate] at process start, before any activity
     * exists.
     *
     * @param savedInstanceState the previously saved instance state, or `null` if this activity
     *   is being created for the first time.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()

        setContent {
            SampleTheme {
                SampleNavHost()
            }
        }
    }
}
