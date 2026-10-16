plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.dokka)
    alias(libs.plugins.android.kotlin)
}

kotlin {
    jvmToolchain(libs.versions.jvm.get().toInt())

    compilerOptions {
        optIn.add("kotlin.time.ExperimentalTime")
    }
}

android {
    namespace = "dev.mobilejoe.api"
    compileSdk = libs.versions.android.sdk.get().toInt()

    defaultConfig {
        minSdk = 26

        buildConfigField(
            "String",
            "MOBILE_JOE_VERSION", "\"${providers.gradleProperty("VERSION_NAME").get()}\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            consumerProguardFiles("consumer-rules.pro")
        }
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.serialization.core)

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.bundles.ktor)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.client.mock)
    testRuntimeOnly(libs.slf4j.nop)
}

mavenPublishing {
    pom {
        name.set("MobileJoe API")
        description.set("MobileJoe API client for Android apps.")
    }
}