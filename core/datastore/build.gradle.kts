plugins {
    alias(libs.plugins.nextbill.android.library)
    alias(libs.plugins.nextbill.hilt)
}

android {
    namespace = "dev.lioracode.nextbill.core.datastore"
}

dependencies {
    api(projects.core.model)
    api(libs.kotlinx.coroutines.core)
    api(libs.androidx.dataStore.preferences)

    testImplementation(projects.core.testing)
}
