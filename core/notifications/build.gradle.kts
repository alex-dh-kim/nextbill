plugins {
    alias(libs.plugins.nextbill.android.library)
    alias(libs.plugins.nextbill.hilt)
}

android {
    namespace = "dev.lioracode.nextbill.core.notifications"
}

dependencies {
    api(projects.core.domain)
    api(projects.core.model)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core.ktx)

    testImplementation(projects.core.testing)
}
