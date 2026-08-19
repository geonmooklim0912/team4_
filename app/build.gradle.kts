plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example.team4uu"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.team4uu"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // AI 서버 주소. 코드에 하드코딩하지 않고 여기서 주입한다.
        //
        // 가비아 VM(공인 IP 1.201.116.93) + sslip.io 로 발급한 인증서를 쓴다.
        // ⚠️ 반드시 https 여야 한다 — targetSdk 37 이라 안드로이드가 평문 HTTP 를 차단한다.
        // ⚠️ 반드시 "/" 로 끝나야 한다 — Retrofit 이 baseUrl 에서 이걸 강제한다.
        buildConfigField("String", "API_BASE_URL", "\"https://1-201-116-93.sslip.io/\"")
    }

    buildTypes {
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
        compose = true
        // 위 buildConfigField 를 코드에서 읽으려면 켜야 한다. AGP 8 부터 기본값이 꺼짐이다.
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // CameraX
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.extensions)

    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    //백엔드와 API요청용
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // 서버가 내려주는 친구 캐릭터 이미지(sprite URL)를 불러와서 그리는 용도
    implementation("io.coil-kt:coil-compose:2.7.0")
}