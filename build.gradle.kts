// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    // Android plugins
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false

    // Kotlin plugins
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.parcelize) apply false
    alias(libs.plugins.kotlin.compose) apply false

    // Google services
    alias(libs.plugins.google.gms.google.services) apply false

    // KSP plugin (not in version catalog)
    id("com.google.devtools.ksp") version "2.0.21-1.0.27" apply false

}