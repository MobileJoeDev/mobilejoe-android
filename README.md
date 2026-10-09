<h3 align="center">🦫 Mobile app toolkit for indies and small teams</h3>

<p align="center">
  <img src="https://img.shields.io/badge/Android-8.0%2B-brightgreen.svg" alt="Android 8.0+">
  <a href="https://central.sonatype.com/artifact/dev.mobilejoe/mobilejoe-api"><img src="https://img.shields.io/maven-central/v/dev.mobilejoe/mobilejoe-api" alt="Maven Central"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-lightgrey.svg" alt="MIT License"></a>
</p>

If you are a small team or an indie working on apps for iOS and Android: Don't waste time on anything outside your core business! [Mobile Joe](https://mobilejoe.dev) provides the backend and native mobile SDKs, so you can start using our tools in your app right away:

- **Feature plans**: Turn user suggestions into curated plans in your own words and use votes to prioritize what to build next.
- **Alerts**: Let users know right away when there is a critical bug, a server outage, or delayed orders.
- **FAQs**: Answer common questions directly in your app and update them without an app release.
- …and more coming soon.

Your users never sign up for a third-party account. Use the ready-made Jetpack Compose components or build your own UI on top of the core SDK.

[Sign up](https://mobilejoe.dev/#pricing) to get started.

## Features

| Feature | Compose component |
| --- | --- |
| Feature plans with voting, search, and status filters | `FeaturePlanHost` |
| FAQs with optional email support | `FaqHost` |
| In-app alert banners with detail sheets | `AlertsView` |

Requires Android 8.0+ (API 26) and compileSdk 37+. The UI module is based on Jetpack Compose.

## Installation

Add the Compose module from [Maven Central](https://central.sonatype.com/artifact/dev.mobilejoe/mobilejoe-ui-compose) to your app module and replace `<version>` with the latest version:

```kotlin
dependencies {
    implementation("dev.mobilejoe:mobilejoe-ui-compose:<version>")
}
```

It includes the core SDK `dev.mobilejoe:mobilejoe-api`. For a custom UI, add only `mobilejoe-api` instead.

## Quick start

Configure once at app startup in your `Application` class. Show SDK components only after the configuration has completed:

```kotlin
import android.app.Application
import android.util.Log
import dev.mobilejoe.api.Joe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                Joe.configure(applicationContext = this@MyApplication, apiKey = "YOUR_API_KEY")
            } catch (e: Exception) {
                Log.e("MyApplication", "Mobile Joe configuration failed", e)
            }
        }
    }
}
```

Register the `Application` class and allow network access in your `AndroidManifest.xml`:

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <uses-permission android:name="android.permission.INTERNET" />

    <application android:name=".MyApplication">
        <!-- ... -->
    </application>
</manifest>
```

Then show a component in your activity, for example your FAQs:

```kotlin
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import dev.mobilejoe.ui.compose.faq.FaqHost
import dev.mobilejoe.ui.compose.faq.FaqUiConfiguration

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                FaqHost(configuration = FaqUiConfiguration(context = this, recipient = "support@example.com"))
            }
        }
    }
}
```

## Documentation

API keys, user identity, feature plans, alerts, and custom UI are covered in the [Mobile Joe documentation](https://docs.mobilejoe.dev). Start with the [Android quickstart](https://docs.mobilejoe.dev/android/quickstart/).

The [sample app](sample-app) shows all components; feature plans and FAQs use preview data.
