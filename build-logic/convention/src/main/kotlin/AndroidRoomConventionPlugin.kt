import androidx.room.gradle.RoomExtension
import dev.lioracode.nextbill.lib
import dev.lioracode.nextbill.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/** Room + KSP. 스키마는 버전마다 schemas/에 내보내 마이그레이션 테스트의 기준으로 씁니다. */
class AndroidRoomConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "androidx.room")
            apply(plugin = "com.google.devtools.ksp")

            extensions.configure<RoomExtension> {
                schemaDirectory("$projectDir/schemas")
            }

            dependencies {
                "implementation"(libs.lib("room-runtime"))
                "ksp"(libs.lib("room-compiler"))
                "testImplementation"(libs.lib("room-testing"))
            }
        }
    }
}
