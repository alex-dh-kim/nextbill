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

// 대상은 걷기 전에 build/·.gradle/을 잘라 낸 fileTree로 줍니다. 문자열 target + targetExclude는
// targetExclude 쪽도 프로젝트 디렉터리 전체를 걷기 때문에, 다른 태스크가 쓰고 있는 build/를 읽다가
// "Could not read path"로 실패합니다. 최상위 *.kts는 디렉터리를 잘라 내 하위로 들어가지 않습니다.
allprojects {
    apply(plugin = "com.diffplug.spotless")
    val isRoot = this == rootProject
    val pruned = arrayOf("**/build/**", "**/.gradle/**")
    val topLevelKts = fileTree(projectDir) {
        include("*.kts")
        // 디렉터리는 모두 잘라 내 최상위 파일만 봅니다("*/**"는 최상위 파일까지 제외하므로 쓰지 않음).
        exclude { it.isDirectory }
    }
    extensions.configure<SpotlessExtension> {
        kotlin {
            if (isRoot) {
                target(
                    fileTree("build-logic") {
                        include("**/*.kt")
                        exclude(*pruned)
                    }
                )
            } else {
                target(
                    fileTree("src") {
                        include("**/*.kt")
                        exclude(*pruned)
                    }
                )
            }
            ktlint(ktlintVersion).setEditorConfigPath(editorConfig)
        }
        kotlinGradle {
            if (isRoot) {
                target(
                    topLevelKts,
                    fileTree("build-logic") {
                        include("**/*.kts")
                        exclude(*pruned)
                    }
                )
            } else {
                target(topLevelKts)
            }
            ktlint(ktlintVersion).setEditorConfigPath(editorConfig)
        }
    }
}
