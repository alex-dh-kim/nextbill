import dev.lioracode.nextbill.lib
import dev.lioracode.nextbill.libs
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

/**
 * Hilt + KSP. Android 모듈 전용입니다. JVM 모듈(core:domain)은 javax.inject + dagger-compiler만 씁니다(ADR-11).
 */
class HiltConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
                throw GradleException(
                    "nextbill.hilt must not be applied to JVM module $path (see ADR-0011)"
                )
            }

            apply(plugin = "com.google.devtools.ksp")

            pluginManager.withPlugin("com.android.base") {
                apply(plugin = "com.google.dagger.hilt.android")
                dependencies {
                    "implementation"(libs.lib("hilt-android"))
                    "ksp"(libs.lib("hilt-compiler"))
                    "ksp"(libs.lib("kotlin-metadata"))
                }
            }
        }
    }
}
