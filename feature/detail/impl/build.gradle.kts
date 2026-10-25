plugins {
    alias(libs.plugins.nextbill.android.feature.impl)
}

android {
    namespace = "dev.lioracode.nextbill.feature.detail.impl"
}

dependencies {
    implementation(projects.feature.detail.api)
    implementation(projects.feature.editor.api)
}
