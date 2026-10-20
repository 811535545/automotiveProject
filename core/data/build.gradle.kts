plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.oem.car.core.data"
    compileSdk = 34
    defaultConfig { minSdk = 28 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:vehicle-service"))
    implementation(project(":core:uxrestrictions"))
    implementation(project(":core:power"))
    implementation(project(":core:audio"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}
