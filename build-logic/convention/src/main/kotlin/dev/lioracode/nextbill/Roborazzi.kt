package dev.lioracode.nextbill

import io.github.takahirom.roborazzi.RoborazziExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Roborazzi를 적용한 모듈은 기준 이미지를 src/test/screenshots에 둡니다(커밋 대상).
 * 기준 이미지는 로컬에서 `./gradlew recordRoborazziDebug`로 찍습니다(CI 없음).
 */
internal fun Project.configureRoborazziWhenApplied() {
    pluginManager.withPlugin("io.github.takahirom.roborazzi") {
        extensions.configure<RoborazziExtension> {
            outputDir.set(layout.projectDirectory.dir("src/test/screenshots"))
        }
    }
}
