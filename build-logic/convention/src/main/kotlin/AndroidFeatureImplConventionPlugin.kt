import dev.lioracode.nextbill.lib
import dev.lioracode.nextbill.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

/**
 * feature:x:impl — 화면, ViewModel, UiState. 다른 feature는 api(NavKey)로만 압니다(설계 문서 §5.2).
 * 저장소는 core:domain의 인터페이스로만 압니다(core:data 의존 금지).
 */
class AndroidFeatureImplConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "nextbill.android.library.compose")
            apply(plugin = "nextbill.hilt")
            apply(plugin = "org.jetbrains.kotlin.plugin.serialization")
            apply(plugin = "io.github.takahirom.roborazzi")

            dependencies {
                "implementation"(project(":core:designsystem"))
                "implementation"(project(":core:ui"))
                "implementation"(project(":core:navigation"))
                "implementation"(project(":core:domain"))

                "implementation"(libs.lib("androidx-lifecycle-viewModelCompose"))
                "implementation"(libs.lib("androidx-lifecycle-runtimeCompose"))
                "implementation"(libs.lib("androidx-hilt-lifecycle-viewModelCompose"))
                "implementation"(libs.lib("androidx-lifecycle-viewModel-navigation3"))
                "implementation"(libs.lib("kotlinx-serialization-json"))

                "testImplementation"(project(":core:testing"))
                "testImplementation"(libs.lib("turbine"))
                "testImplementation"(libs.lib("roborazzi"))
                "testImplementation"(libs.lib("roborazzi-compose"))
                "testImplementation"(libs.lib("androidx-compose-ui-test-junit4"))
                "debugImplementation"(libs.lib("androidx-compose-ui-test-manifest"))
            }
        }
    }
}
