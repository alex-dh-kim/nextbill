plugins {
    alias(libs.plugins.nextbill.android.feature.impl)
}

android {
    namespace = "dev.lioracode.nextbill.feature.editor.impl"
}

dependencies {
    implementation(projects.feature.editor.api)
    // 알림 권한 요청(rememberLauncherForActivityResult).
    implementation(libs.androidx.activity.compose)
}
