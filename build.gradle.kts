import com.diffplug.gradle.spotless.SpotlessExtension

// 모든 플러그인을 루트 클래스패스에 한 번만 올려, 모듈마다 다른 버전이 섞이지 않게 합니다.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.compose) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.spotless)
    id("nextbill.module-rules")
}

// Spotless(ktlint)는 모든 프로젝트의 *.kt·*.kts에 적용합니다. 규칙은 루트 .editorconfig.
// 파일 하나만 고치기: ./gradlew spotlessApply -PspotlessIdeHook=<절대 경로>
val ktlintVersion = libs.versions.ktlint.get()
val editorConfig = layout.projectDirectory.file(".editorconfig")

allprojects {
    apply(plugin = "com.diffplug.spotless")
    extensions.configure<SpotlessExtension> {
        val isRoot = this@allprojects == rootProject
        kotlin {
            if (isRoot) {
                target("build-logic/**/*.kt")
            } else {
                target("src/**/*.kt")
            }
            targetExclude("**/build/**")
            ktlint(ktlintVersion).setEditorConfigPath(editorConfig)
        }
        kotlinGradle {
            if (isRoot) {
                target("*.kts", "build-logic/**/*.kts")
            } else {
                target("*.kts")
            }
            targetExclude("**/build/**")
            ktlint(ktlintVersion).setEditorConfigPath(editorConfig)
        }
    }
}
