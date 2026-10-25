package dev.lioracode.nextbill

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion

/**
 * Android 모듈 공통 설정. Kotlin은 AGP 9 내장 Kotlin을 쓰고,
 * jvmTarget은 compileOptions.targetCompatibility(17)를 따릅니다.
 */
internal fun configureAndroidCommon(extension: CommonExtension) {
    extension.apply {
        compileSdk = AndroidSdk.COMPILE
        defaultConfig.minSdk = AndroidSdk.MIN
        compileOptions.sourceCompatibility = JavaVersion.VERSION_17
        compileOptions.targetCompatibility = JavaVersion.VERSION_17
    }
}
