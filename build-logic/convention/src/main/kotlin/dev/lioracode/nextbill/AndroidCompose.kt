package dev.lioracode.nextbill

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.compose.compiler.gradle.ComposeCompilerGradlePluginExtension

/**
 * Compose 공통 설정. `-PcomposeReports=true`이면 모듈의 build/compose-reports에 안정성 리포트를 씁니다.
 * 리포트는 stable 여부만 보여 주고 성능을 증명하지 않습니다(설계 문서 §4.3).
 */
internal fun Project.configureAndroidCompose(extension: CommonExtension) {
    extension.buildFeatures.compose = true

    dependencies {
        val bom = platform(libs.lib("androidx-compose-bom"))
        "implementation"(bom)
        "testImplementation"(bom)
        "implementation"(libs.lib("androidx-compose-ui-tooling-preview"))
        "debugImplementation"(libs.lib("androidx-compose-ui-tooling"))
    }

    extensions.configure<ComposeCompilerGradlePluginExtension> {
        if (providers.gradleProperty("composeReports").orNull.toBoolean()) {
            reportsDestination.set(layout.buildDirectory.dir("compose-reports"))
        }
        stabilityConfigurationFiles.add(
            rootProject.layout.projectDirectory.file("compose_compiler_config.conf")
        )
    }
}
