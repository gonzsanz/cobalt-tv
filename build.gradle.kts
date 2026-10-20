buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        // AGP 9 compiles Kotlin itself (built-in Kotlin). Declaring KGP here keeps the
        // Kotlin compiler and the Compose compiler plugin on the same, recent version.
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.4.21")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
