plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.adaptivestrength.nativeapp"
    compileSdk = 35
    ndkVersion = "27.0.12077973"
    defaultConfig {
        applicationId = "com.adaptivestrength.nativeapp"
        minSdk = 26; targetSdk = 35; versionCode = 1; versionName = "0.1.0-dev"
        ndk { abiFilters += listOf("arm64-v8a") }
        externalNativeBuild { cmake { cppFlags += listOf("-std=c++17", "-O3") } }
    }
    externalNativeBuild { cmake { path = file("src/main/cpp/CMakeLists.txt"); version = "3.22.1" } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
    packaging { jniLibs { pickFirsts += setOf("**/libc++_shared.so") } }
}
dependencies {
    implementation(project(":core"))
    implementation("org.opencv:opencv:4.10.0")
    implementation("com.google.mediapipe:tasks-vision:0.10.21")
}
