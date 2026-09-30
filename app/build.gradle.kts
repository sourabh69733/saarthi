plugins {
    id("com.android.application")
}

android {
    namespace = "dev.saarthi.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.saarthi.app"
        minSdk = 28
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
        externalNativeBuild {
            cmake {
                cppFlags += listOf("-std=c++17", "-O3")
                arguments += listOf("-DWHISPER_BUILD_TESTS=OFF", "-DWHISPER_BUILD_EXAMPLES=OFF", "-DGGML_NATIVE=OFF")
            }
        }
    }

    buildTypes {
        release { isMinifyEnabled = false }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    externalNativeBuild { cmake { path = file("src/main/cpp/CMakeLists.txt") } }
    buildFeatures { buildConfig = true }
    packaging { jniLibs.useLegacyPackaging = true }
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.car.app:app:1.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
}

val verifyWhisperModel = tasks.register("verifyWhisperModel") {
    doLast {
        val model = file("src/main/assets/ggml-base.bin")
        check(model.exists() && model.length() == 147_951_465L) {
            "Missing Whisper base model. See README.md for the pinned download and checksum."
        }
    }
}
tasks.named("preBuild") { dependsOn(verifyWhisperModel) }
