plugins {
    alias(libs.plugins.nextbill.android.feature.impl)
}

android {
    namespace = "dev.lioracode.nextbill.feature.archive.impl"
}

dependencies {
    implementation(projects.feature.archive.api)
    implementation(projects.feature.detail.api)
}
