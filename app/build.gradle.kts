plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.gms.google.services)


}

android {
    namespace = "com.example.wikinow"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.wikinow"
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
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildFeatures {
        compose = true
        viewBinding = true
    }
}

dependencies {

    // Need to add in app/build.gradle
    implementation ("com.google.firebase:firebase-firestore-ktx:24.10.0")
    // Add to top of dependencies block:
    implementation(libs.firebase.bom)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.accompanist.systemuicontroller)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.firebase.auth)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.navigation.runtime.ktx)
//    implementation(libs.androidx.navigation.compose.jvmstubs)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose.v280)
    implementation(libs.coil.compose)
    implementation("androidx.compose.material:material-icons-extended:1.5.4")
    implementation("androidx.compose.material:material-icons-core:1.5.4")
    implementation("androidx.compose.material3:material3:1.1.2")
    implementation ("androidx.compose.material3:material3:1.3.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)


    implementation(libs.androidx.lifecycle.viewmodel.ktx.v287)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

// Coil
    implementation("io.coil-kt:coil-compose:2.4.0")

// Accompanist Pager
    implementation("com.google.accompanist:accompanist-pager:0.28.0")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.10.0")

    implementation("io.coil-kt:coil-compose:2.4.0")

    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.0")



    //webview
    implementation("androidx.webkit:webkit:1.7.0")

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)

    implementation ("androidx.hilt:hilt-navigation-compose:1.0.0")
    implementation ("androidx.lifecycle:lifecycle-runtime-compose:2.6.0")
}