plugins {
    alias(libs.plugins.nextbill.android.application.compose)
    alias(libs.plugins.nextbill.hilt)
}

android {
    namespace = "dev.lioracode.nextbill"

    defaultConfig {
        applicationId = "dev.lioracode.nextbill"
        versionCode = 1
        versionName = "0.1.0"
    }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
}
