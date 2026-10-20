plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.oem.car.app"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.oem.car.app"
        minSdk = 28
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.8" }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
    implementation(project(":feature:dashboard"))
    implementation(project(":feature:cluster"))
    implementation(project(":feature:climate"))
    implementation(project(":feature:media"))
    implementation(project(":feature:connected"))
    implementation(project(":feature:navigation"))
    implementation(project(":feature:phone"))
    implementation(project(":feature:energy"))
    implementation(project(":feature:health"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:voice"))

    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation(platform("androidx.compose:compose-bom:2024.10.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
}
