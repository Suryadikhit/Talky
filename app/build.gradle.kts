plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.example.talky"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.talky"
        minSdk = 24
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
}

dependencies {
    implementation("io.coil-kt:coil:2.7.0") // Latest version
    implementation(libs.coil.compose)
        // Retrofit for API calls
        implementation("com.squareup.retrofit2:retrofit:2.11.0")
        implementation("com.squareup.retrofit2:converter-gson:2.11.0") // Gson for JSON parsing
    debugImplementation ("androidx.compose.ui:ui-tooling:1.7.8")

        // OkHttp for networking
        implementation("com.squareup.okhttp3:okhttp:4.12.0")
        implementation("com.squareup.okhttp3:logging-interceptor:4.12.0") // Logging for debugging

        // Coroutines for async API calls
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

        // Gson for JSON serialization
        implementation("com.google.code.gson:gson:2.12.1")

        // Multipart File Upload (Needed for Image Upload)
        implementation("com.squareup.retrofit2:converter-scalars:2.9.0")
        implementation(libs.accompanist.permissions)
        implementation(libs.android.image.cropper)
        implementation(libs.coil.compose)
        implementation(libs.accompanist.systemuicontroller)
        implementation(libs.hilt.android)
        implementation(libs.androidx.hilt.navigation.compose)
        kapt(libs.hilt.android.compiler)
        implementation(libs.androidx.core.ktx)
        implementation(libs.androidx.lifecycle.runtime.ktx)
        implementation(libs.androidx.activity.compose)
        implementation(platform(libs.androidx.compose.bom))
        implementation(libs.androidx.ui)
        implementation(libs.androidx.ui.graphics)
        implementation(libs.androidx.ui.tooling.preview)
        implementation(libs.androidx.material3)
        implementation(libs.androidx.navigation.runtime.ktx)
        implementation(libs.androidx.navigation.compose)

        implementation(libs.firebase.auth.ktx)
        implementation(libs.firebase.firestore.ktx)
        implementation(libs.firebase.storage.ktx)
        implementation(libs.firebase.database.ktx)
        implementation(libs.androidx.room.common)
        implementation(libs.androidx.room.ktx)

        testImplementation(libs.junit)
        androidTestImplementation(libs.androidx.junit)
        androidTestImplementation(libs.androidx.espresso.core)
        androidTestImplementation(platform(libs.androidx.compose.bom))
        androidTestImplementation(libs.androidx.ui.test.junit4)
        debugImplementation(libs.androidx.ui.tooling)
        debugImplementation(libs.androidx.ui.test.manifest)
    }
