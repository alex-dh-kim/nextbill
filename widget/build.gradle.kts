plugins {
    alias(libs.plugins.nextbill.android.library.compose)
    alias(libs.plugins.nextbill.hilt)
}

android {
    namespace = "dev.lioracode.nextbill.widget"
}

dependencies {
    implementation(projects.core.domain)
    implementation(projects.core.model)
    implementation(projects.core.designsystem)
    implementation(projects.core.notifications)
    implementation(projects.core.ui)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(projects.core.testing)
    testImplementation(libs.androidx.glance.appwidget.testing)
}
