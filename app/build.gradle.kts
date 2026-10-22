plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "io.github.chiragjjjks.metronudge"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.chiragjjjks.metronudge"
        minSdk = 26
        targetSdk = 37
        versionCode = 4
        versionName = "0.1.3"
    }

    // Release signing comes from environment variables that CI fills from
    // repository secrets. Without them the release APK uses the debug key so
    // it still installs (but each CI run has a different debug key, so you
    // must uninstall before installing a newer build).
    val keystorePath = System.getenv("MN_KEYSTORE_PATH")
    if (!keystorePath.isNullOrBlank()) {
        signingConfigs.create("release") {
            storeFile = file(keystorePath)
            storePassword = System.getenv("MN_KEYSTORE_PASSWORD")
            keyAlias = System.getenv("MN_KEY_ALIAS")
            keyPassword = System.getenv("MN_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }

    buildFeatures { compose = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    dependenciesInfo {
        // No dependency metadata blob in the APK (F-Droid friendly).
        includeInApk = false
        includeInBundle = false
    }
}

// Station and channel data live at the repo root so contributors can edit
// them without touching app code; they ship as APK assets.
androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addStaticSourceDirectory(rootProject.file("data").absolutePath)
    }
}

kotlin { jvmToolchain(17) }

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.08.01"))
    implementation("androidx.compose.material3:material3")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
    // Android's org.json is stubbed in local unit tests; use the real one.
    testImplementation("org.json:json:20240303")
}
