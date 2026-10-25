plugins {
    alias(libs.plugins.nextbill.android.feature.impl)
}

android {
    namespace = "dev.lioracode.nextbill.feature.calendar.impl"
}

dependencies {
    implementation(projects.feature.calendar.api)
    implementation(projects.feature.detail.api)
}
