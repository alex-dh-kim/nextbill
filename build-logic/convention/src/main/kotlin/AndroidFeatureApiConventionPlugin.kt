import dev.lioracode.nextbill.lib
import dev.lioracode.nextbill.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

/** feature:x:api — 화면 진입점 NavKey만 담습니다. NavKey는 kotlinx-serialization으로 직렬화합니다. */
class AndroidFeatureApiConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "nextbill.android.library.compose")
            apply(plugin = "org.jetbrains.kotlin.plugin.serialization")

            dependencies {
                "api"(project(":core:navigation"))
                "implementation"(libs.lib("kotlinx-serialization-core"))
            }
        }
    }
}
