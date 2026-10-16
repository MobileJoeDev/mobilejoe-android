# Changelog

All notable changes to the Mobile Joe Android SDK are documented in this file.

## 1.0.2

Lowers the toolchain requirements for apps. You no longer need compileSdk 37, Android Gradle Plugin 9, Kotlin 2.4, or JDK 21 to use the SDK.

- Requires Android 8.0+ (API 26), compileSdk 35+, Android Gradle Plugin 8.6+, and Kotlin 2.1+.
- The SDK compiles to Java 17 bytecode, so apps can build with JDK 17 and JVM target 17.
- The SDK depends on lower minimum versions of AndroidX, Compose, Ktor, and kotlinx. If your app uses newer versions, Gradle keeps them.
- The SDK declares the `INTERNET` permission itself. Your app no longer needs to add it.
- Compose UI tooling, including its exported `PreviewActivity`, is no longer merged into release builds of your app.

## 1.0.1

Fixes alerts that were missing on a cold start. This release changes how you set up the SDK, so it may need small changes in your app. It also simplifies the FAQ list.

### Setup

- `Joe.configure`, `Joe.identify`, and `Joe.logout` are no longer `suspend` functions. Call `Joe.configure` directly in `Application.onCreate`, not in a coroutine.
- `Joe.configure` returns right away, and SDK components load content immediately. It is safe to call from any thread.
- The SDK sends the identity to Mobile Joe in the background, once per app start and once per identity change. If that fails, it logs an error to Logcat with the tag `Joe` and retries on the next app start.
- A rejected API key now throws `JoeInvalidApiKeyException`, a subclass of `JoeHttpException` with status 401. It disables SDK requests until the app restarts.
- `Joe.identify` and `Joe.logout` throw `JoeStorageException` when the identity can't be stored. `Joe.configure` logs it instead of throwing.

### Fixes

- `AlertsView` on the start screen showed no active alert on a cold start until the app returned from the background.
- Concurrent loads of the feature plan list could show duplicate entries.
- The feature plan list keeps its state, such as the filter, when you navigate back from the detail screen.
- A vote cast in the feature plan detail screen shows in the list after navigating back.

### FAQs

- The FAQ list shows only the question of each entry, without an excerpt of the answer. The answer opens in the detail screen.
- With a support recipient in `FaqUiConfiguration`, the FAQ list shows the contact hint and the email link below the entries, not only in the detail screen.
- The German default texts for the contact hint and the email link use the informal wording.

## 1.0.0

First public release of the Mobile Joe Android SDK.

- Feature plans: let users suggest and vote on features, with search and status filters (`FeaturePlanHost`).
- Alerts: show in-app banners with detail sheets for outages or critical bugs (`AlertsView`).
- FAQs: answer common questions in your app, with optional email support (`FaqHost`).
- Build your own UI on top of the core `mobilejoe-api` library.

Requires Android 8.0+ (API 26) and compileSdk 37+. The UI module is based on Jetpack Compose.
