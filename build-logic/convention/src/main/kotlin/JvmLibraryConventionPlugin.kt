import com.android.build.api.dsl.Lint
import dev.lioracode.nextbill.configureAndroidApiCheck
import dev.lioracode.nextbill.configureWarningsAsErrors
import dev.lioracode.nextbill.lib
import dev.lioracode.nextbill.libs
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * Android 의존성이 없는 순수 Kotlin 모듈(core:model, core:domain, core:testing).
 * 이 코드도 기기(minSdk 26)에서 돌므로 Android Lint를 붙입니다(NiA와 같음). Lint의 NewApi는 JVM 모듈을
 * 검사하지 않으므로(AndroidApiCheck.kt), `checkAndroidApi`가 컴파일된 클래스를 SDK의 api-versions.xml과
 * 비교하고, app의 `lintDebug`(checkDependencies)가 부르는 이 모듈의 lint 분석 앞에 실행됩니다.
 */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "org.jetbrains.kotlin.jvm")
            apply(plugin = "com.android.lint")

            extensions.configure<JavaPluginExtension> {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
            }
            extensions.configure<KotlinJvmProjectExtension> {
                compilerOptions.jvmTarget.set(JvmTarget.JVM_17)
            }

            extensions.configure<Lint> {
                abortOnError = true
            }
            configureAndroidApiCheck()
            configureWarningsAsErrors()

            dependencies {
                "testImplementation"(libs.lib("junit"))
                "testImplementation"(libs.lib("truth"))
            }
        }
    }
}
