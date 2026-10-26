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

    // AppModule이 BuildConfig.VERSION_NAME으로 @Named("appVersion")을 제공합니다.
    buildFeatures.buildConfig = true
}

dependencies {
    // 조립: 저장소·알림·위젯 구현과 각 feature의 진입점(NavKey).
    implementation(projects.core.data)
    implementation(projects.core.notifications)
    implementation(projects.widget)
    implementation(projects.core.navigation)
    implementation(projects.core.designsystem)
    implementation(projects.core.ui)
    implementation(projects.feature.home.api)
    implementation(projects.feature.home.impl)
    implementation(projects.feature.calendar.api)
    implementation(projects.feature.calendar.impl)
    implementation(projects.feature.editor.api)
    implementation(projects.feature.editor.impl)
    implementation(projects.feature.detail.api)
    implementation(projects.feature.detail.impl)
    implementation(projects.feature.archive.api)
    implementation(projects.feature.archive.impl)
    implementation(projects.feature.settings.api)
    implementation(projects.feature.settings.impl)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtimeCompose)
    implementation(libs.androidx.lifecycle.viewModelCompose)
    implementation(libs.androidx.hilt.lifecycle.viewModelCompose)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(projects.core.testing)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
