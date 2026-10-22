plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.zyon.prototype"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.zyon.prototype"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "0.2"
    }
}
