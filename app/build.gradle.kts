import java.util.Properties

// Load API key from local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}
val streamApiKey: String = localProperties.getProperty("STREAM_API_KEY") ?: ""

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)          // KSP plugin (we're using KSP instead of kapt)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.example.museapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.museapp"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "BASE_URL", "\"http://localhost:3000/\"")
        buildConfigField("boolean", "USE_FAKE_REPO", "false")
        buildConfigField("String", "STREAM_API_KEY",  "\"$streamApiKey\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

// KSP arguments for Room (optional but recommended)
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.libphonenumber)

    // If you still use the old Material TopAppBar, keep this; otherwise you can remove it.
    implementation("androidx.compose.material:material")

    // Lifecycle extras
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)

    // Hilt with KSP (no kapt)
    implementation(libs.hilt.android)
    implementation(libs.play.services.auth)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.compose.foundation.android)
    implementation(libs.androidx.compose.foundation.android)
    implementation(libs.androidx.compose.foundation)
    ksp(libs.hilt.compiler)                    // ← Hilt KSP
    implementation(libs.hilt.navigation.compose)
    implementation("com.google.android.gms:play-services-location:21.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")


    // Coroutines
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)

    // Networking stack
    implementation(libs.retrofit)
    implementation(libs.retrofit.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)

    implementation("com.google.accompanist:accompanist-flowlayout:0.32.0")
    implementation("io.coil-kt:coil-compose:2.4.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.35.0-alpha")
    implementation("com.modernmt.text:profanity-filter:1.0.1")
    implementation("com.google.android.gms:play-services-identity:18.1.0")
    implementation("com.googlecode.libphonenumber:libphonenumber:8.13.11")
    implementation("com.airbnb.android:lottie-compose:6.1.0")
    implementation("androidx.core:core-splashscreen:1.0.0")
    implementation("io.coil-kt:coil-compose:2.5.0")
    implementation("io.coil-kt:coil-gif:2.5.0")

    // Room (runtime + ktx + KSP compiler)
    implementation("androidx.room:room-runtime:2.5.2")
    implementation("com.google.accompanist:accompanist-flowlayout:0.32.0")
    implementation("androidx.room:room-ktx:2.5.2")
    ksp("androidx.room:room-compiler:2.5.2")      // <-- REQUIRED for Room code generation with KSP

    // Optional: Room testing helpers
    testImplementation("androidx.room:room-testing:2.5.2")

    // DataStore (Preferences)
    implementation(libs.androidx.datastore)

    // Splash screen
    implementation(libs.androidx.splashscreen)

    // Tests / tooling
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
