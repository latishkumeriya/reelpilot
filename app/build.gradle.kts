import com.android.build.api.dsl.ApplicationDefaultConfig

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.reelpilot.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.reelpilot.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "1.1.0"
    }

    // Release signing reads from ~/.gradle/gradle.properties (NEVER commit secrets):
    //   REEL_STORE_FILE=D:\\keys\\reel-pilot.jks
    //   REEL_STORE_PASSWORD=***
    //   REEL_KEY_ALIAS=reel
    //   REEL_KEY_PASSWORD=***
    // If any key is missing the release build stays unsigned (debug still works).
    val reelStoreFile: String? = providers.gradleProperty("REEL_STORE_FILE").orNull
    val reelStorePassword: String? = providers.gradleProperty("REEL_STORE_PASSWORD").orNull
    val reelKeyAlias: String? = providers.gradleProperty("REEL_KEY_ALIAS").orNull
    val reelKeyPassword: String? = providers.gradleProperty("REEL_KEY_PASSWORD").orNull
    val hasReleaseSigning =
        !reelStoreFile.isNullOrBlank() && !reelStorePassword.isNullOrBlank() &&
        !reelKeyAlias.isNullOrBlank() && !reelKeyPassword.isNullOrBlank() &&
        file(reelStoreFile).exists()

    signingConfigs {
        // Pinned shared debug key: every CI build gets the SAME signature,
        // so new builds update cleanly instead of "App not installed".
        // (Standard Android debug credentials — safe to commit, NOT a release key.)
        // NOTE: AGP pre-creates a "debug" config, so we reconfigure it via getByName.
        getByName("debug") {
            storeFile = rootProject.file("debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        create("reel") {
            if (hasReleaseSigning) {
                storeFile = file(reelStoreFile!!)
                storePassword = reelStorePassword
                keyAlias = reelKeyAlias
                keyPassword = reelKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("reel")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.service)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.hilt.android)
    implementation(libs.androidx.hilt.navigation.compose)
    ksp(libs.hilt.compiler)
}
