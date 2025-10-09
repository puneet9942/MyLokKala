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
    alias(libs.plugins.ksp)          // NEW: use KSP instead of kapt
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.example.lokkala"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.lokkala"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080/\"")
        buildConfigField("boolean", "USE_FAKE_REPO", "true")
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

    // Hilt with KSP (no kapt, no javapoet issues)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)                    // ← replace kapt with ksp
    implementation(libs.hilt.navigation.compose)

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
