import com.android.build.api.dsl.ApplicationExtension
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

class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "com.android.application")
            configureRoborazziWhenApplied()
            configureRobolectricSdk()
            configureWarningsAsErrors()

            extensions.configure<ApplicationExtension> {
                configureAndroidCommon(this)
                defaultConfig.targetSdk = AndroidSdk.TARGET
                testOptions.unitTests.isIncludeAndroidResources = true
                // 의존 모듈의 lint 분석을 :app:lintDebug에 묶습니다. JVM 모듈(core:model·core:domain)의
                // NewApi는 Lint가 검사하지 않고, 그 분석 앞에 붙은 checkAndroidApi가 검사합니다(AndroidApiCheck.kt).
                lint.checkDependencies = true
                // Lint 경고도 빌드를 실패시킵니다. 남겨 둘 경고는 그 자리에 tools:ignore·@SuppressLint로 이유와 함께 답니다.
                lint.warningsAsErrors = true
            }

            dependencies {
                "testImplementation"(libs.lib("junit"))
                "testImplementation"(libs.lib("truth"))
                "testImplementation"(libs.lib("robolectric"))
            }
        }
    }
}
