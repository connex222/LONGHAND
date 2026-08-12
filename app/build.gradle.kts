plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Present only when CI decodes it from a secret, or when you drop your own
// keystore next to this file locally. Absent is fine — the release build
// falls back to the debug key so you always get an installable APK.
val keystore = rootProject.file("release.keystore")

android {
    namespace = "uk.co.actuabs.longhand"
    compileSdk = 36

    defaultConfig {
        applicationId = "uk.co.actuabs.longhand"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.1"
    }

    signingConfigs {
        if (keystore.exists()) {
            create("release") {
                storeFile = keystore
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS") ?: "longhand"
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            signingConfig = if (keystore.exists())
                signingConfigs.getByName("release")
            else
                signingConfigs.getByName("debug")
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.webkit:webkit:1.12.1")
}
