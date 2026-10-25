plugins {
    alias(libs.plugins.nextbill.android.library)
    alias(libs.plugins.nextbill.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "dev.lioracode.nextbill.core.data"
}

dependencies {
    api(projects.core.domain)
    api(projects.core.model)
    implementation(projects.core.database)
    implementation(projects.core.datastore)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.process)

    implementation(libs.kotlinx.serialization.json)

    testImplementation(projects.core.testing)
    testImplementation(libs.room.runtime)
}
