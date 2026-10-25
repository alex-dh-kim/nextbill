plugins {
    alias(libs.plugins.nextbill.android.feature.impl)
}

android {
    namespace = "dev.lioracode.nextbill.feature.home.impl"
}

dependencies {
    implementation(projects.feature.editor.api)
    implementation(projects.feature.home.api)
    implementation(projects.feature.detail.api)
}
