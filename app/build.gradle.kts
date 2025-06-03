// Remove this line: import org.gradle.kotlin.dsl.implementation

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp") // KSP plugin for Room
    id("kotlin-parcelize")         // Kotlin Parcelize plugin
}

android {
    namespace = "com.example.mindscribe"
    compileSdk = 35 // Using SDK 35 as specified

    defaultConfig {
        applicationId = "com.example.mindscribe"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        // IMPORTANT: This version must be compatible with your Compose BOM version.
        // For compose-bom:2024.05.00, Kotlin Compiler Extension Version 1.5.11 is typically recommended.
        kotlinCompilerExtensionVersion = "1.5.11" // Updated to a more recent compatible version
    }
}

// Use the latest stable Room version
val room_version = "2.6.1" // Changed to latest stable Room version

dependencies {
    // Core AndroidX libraries (using libs.versions.toml)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose) // Provides ComponentActivity and rememberLauncherForActivityResult

    // Compose BOM (Bill of Materials) - ensures all Compose libraries are compatible
    implementation(platform(libs.androidx.compose.bom)) // This should pull in '2024.05.00' from your libs.versions.toml
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended) // For extended Material Icons

    implementation(libs.androidx.navigation.compose) // For Jetpack Compose Navigation

    // Room dependencies
    implementation("androidx.room:room-runtime:$room_version")
    ksp("androidx.room:room-compiler:$room_version") // KSP for Room annotation processing
    implementation("androidx.room:room-ktx:$room_version") // Kotlin extensions for Room, including coroutines support

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0") // Latest stable Coroutines library

    // Additional Compose/Lifecycle dependencies (ensure they are not redundant if already covered by BOM/activity-compose)
    // These specific versions might be overridden by the Compose BOM or lifecycle-runtime-ktx.
    // Use the latest stable versions if you explicitly need them outside of the BOM's management.
    implementation("androidx.compose.runtime:runtime-livedata:1.6.7") // Current stable for Compose 1.6.x
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0") // Matches lifecycle-runtime-ktx

    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.6.0")

    // Add Gson library for TypeConverters
    implementation("com.google.code.gson:gson:2.10.1")


    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.9.0")

    implementation("androidx.compose.runtime:runtime-saveable:1.8.2")

    // Splash Screen API
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Test dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom)) // BOM for androidTest dependencies
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}