plugins {
    id("com.android.library")
}
android {
    namespace = "com.oem.car.middleware.aidl"
    compileSdk = 34
    defaultConfig { minSdk = 28 }
    buildFeatures { aidl = true }
}
