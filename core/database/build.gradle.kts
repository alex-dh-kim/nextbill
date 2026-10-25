plugins {
    alias(libs.plugins.nextbill.android.library)
    alias(libs.plugins.nextbill.android.room)
    alias(libs.plugins.nextbill.hilt)
}

android {
    namespace = "dev.lioracode.nextbill.core.database"
}

dependencies {
    api(libs.kotlinx.coroutines.core)

    testImplementation(projects.core.testing)
}
