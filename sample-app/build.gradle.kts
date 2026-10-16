import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.dokka)
    alias(libs.plugins.android.kotlin)
}

kotlin {
    jvmToolchain(libs.versions.jvm.get().toInt())
}

android {
    namespace = "dev.mobilejoe.samplejoe"
    compileSdk = libs.versions.android.sdk.get().toInt()

    defaultConfig {
        applicationId = "dev.mobilejoe.samplejoe"

        minSdk = 26
        targetSdk = libs.versions.android.sdk.get().toInt()

        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            signingConfig = signingConfigs.getByName("debug")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    androidResources {
        //noinspection MissingResourcesProperties
        generateLocaleConfig = true
    }
}

dependencies {
    implementation(project(":mobilejoe-ui-compose"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    implementation(platform(libs.compose.bom))
    implementation(libs.bundles.compose)
    implementation(libs.compose.navigation)
    implementation(libs.androidx.activity.compose)

    runtimeOnly(libs.slf4j.android)
}