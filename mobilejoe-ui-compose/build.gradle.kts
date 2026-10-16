import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.dokka)
    alias(libs.plugins.android.kotlin)
}

kotlin {
    jvmToolchain(libs.versions.jvm.get().toInt())
}

android {
    namespace = "dev.mobilejoe.ui.compose"
    compileSdk = libs.versions.android.sdk.get().toInt()

    defaultConfig {
        minSdk = 26
    }

    buildFeatures {
        compose = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            consumerProguardFiles("consumer-rules.pro")
        }
    }
}

dependencies {
    api(project(":mobilejoe-api"))

    api(libs.androidx.lifecycle.viewmodel.ktx)
    api(platform(libs.compose.bom))
    api(libs.compose.runtime)
    api(libs.compose.ui)
    api(libs.compose.navigation)

    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)

    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}

mavenPublishing {
    pom {
        name.set("MobileJoe Compose UI")
        description.set("Jetpack Compose components for MobileJoe on Android.")
    }
}