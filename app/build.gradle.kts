plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.wantokpay"
    compileSdk = 37
    val apiBaseUrl = providers.gradleProperty("WANTOKPAY_API_BASE_URL")
        .orElse("https://api.example.invalid")
        .get()

    defaultConfig {
        applicationId = "com.example.wantokpay"
        minSdk = 24
        targetSdk = 36
        versionCode = 3
        versionName = "1.0.2"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField(
            "String",
            "WANTOKPAY_API_BASE_URL",
            "\"${apiBaseUrl.replace("\\", "\\\\").replace("\"", "\\\"")}\""
        )
    }
    buildTypes {
        debug {
            buildConfigField(
                "String",
                "WANTOKPAY_API_BASE_URL",
                "\"${providers.gradleProperty("WANTOKPAY_API_BASE_URL").orElse("http://10.0.2.2:8000").get().replace("\\", "\\\\").replace("\"", "\\\"")}\""
            )
        }
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.material)
    implementation("de.mkammerer:argon2-jvm:2.11")
    implementation("com.google.code.gson:gson:2.13.1")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}