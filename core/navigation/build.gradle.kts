plugins {
    alias(libs.plugins.nextbill.android.library.compose)
    // 테스트 NavKey 직렬화용(rememberNavBackStack은 키의 serializer가 필요합니다).
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.lioracode.nextbill.core.navigation"
}

dependencies {
    api(libs.androidx.navigation3.runtime)
    api(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewModel.navigation3)

    testImplementation(libs.androidx.compose.foundation)
    testImplementation(libs.kotlinx.serialization.core)
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.lifecycle.viewModelCompose)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
