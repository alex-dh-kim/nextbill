import com.android.build.api.dsl.LibraryExtension
import dev.lioracode.nextbill.AndroidSdk
import dev.lioracode.nextbill.configureAndroidCommon
import dev.lioracode.nextbill.configureRobolectricSdk
import dev.lioracode.nextbill.configureRoborazziWhenApplied
import dev.lioracode.nextbill.configureWarningsAsErrors
import dev.lioracode.nextbill.lib
import dev.lioracode.nextbill.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.library")
            configureRoborazziWhenApplied()
            configureRobolectricSdk()
            configureWarningsAsErrors()

            extensions.configure<LibraryExtension> {
                configureAndroidCommon(this)
                testOptions.targetSdk = AndroidSdk.TARGET
                lint.targetSdk = AndroidSdk.TARGET
                testOptions.unitTests.isIncludeAndroidResources = true
            }

            dependencies {
                "testImplementation"(libs.lib("junit"))
                "testImplementation"(libs.lib("truth"))
                "testImplementation"(libs.lib("robolectric"))
            }
        }
    }
}
