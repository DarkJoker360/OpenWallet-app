/*
 * SPDX-FileCopyrightText: 2025 Simone Esposito
 * SPDX-License-Identifier: Apache-2.0
 */

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.dagger.hilt.android)
    alias(libs.plugins.kotlin.ksp)
    id("kotlin-parcelize")
}

android {
    namespace = "com.esposito.openwallet"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.esposito.openwallet"
        minSdk = 31
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "VERSION_NAME", "\"${versionName}\"")
        buildConfigField("int", "VERSION_CODE", "$versionCode")
    }

    buildFeatures {
        buildConfig = true
        compose = true  
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".dev"
            buildConfigField("boolean", "ENABLE_DEVELOPER_OPTIONS", "true")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            buildConfigField("boolean", "ENABLE_DEVELOPER_OPTIONS", "false")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Hilt Dependency Injection
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    
    // Material Icons
    implementation(libs.material.icons.extended)
    
    // ViewModel and Navigation
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    
    // Room Database
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    
    // SQLCipher for encrypted database  
    implementation(libs.zetetic.sqlcipher.android)
    implementation(libs.androidx.sqlite)
    
    // Security Crypto for encrypted preferences
    implementation(libs.security.crypto)
    
    // Biometric authentication
    implementation(libs.biometric)
    
    // Permissions
    implementation(libs.accompanist.permissions)
    
    // JSON and Image Loading
    implementation(libs.gson)
    implementation(libs.activity.ktx)
    implementation(libs.coil.compose)
    
    // QR Code and Barcode
    implementation(libs.zxing.core)
    implementation(libs.zxing.android.embedded)
    
    // Camera and Barcode Scanning
    implementation(libs.camera.core)
    implementation(libs.camera.camera2)
    implementation(libs.camera.lifecycle)
    implementation(libs.camera.view)
    implementation(libs.mlkit.barcode)
    
    // Credit Card OCR Scanner (Using bundled ML Kit - NO GMS required!)
    implementation(libs.mlkit.text)
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
