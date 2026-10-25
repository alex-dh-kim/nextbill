plugins {
    alias(libs.plugins.nextbill.jvm.library)
    alias(libs.plugins.ksp)
}

dependencies {
    api(projects.core.model)
    api(libs.javax.inject)
    api(libs.kotlinx.coroutines.core)
    // KSP가 만드는 *_Factory가 dagger.internal.*을 import합니다(ADR-0011).
    compileOnly(libs.dagger)
    ksp(libs.dagger.compiler)
}
