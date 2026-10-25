import dev.lioracode.nextbill.isCheckedConfiguration
import dev.lioracode.nextbill.violations
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register

/** 루트 프로젝트에 적용해 `checkModuleRules` 태스크를 등록합니다. */
class ModuleRulesPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        require(target == target.rootProject) {
            "nextbill.module-rules must be applied to the root project"
        }
        target.tasks.register<CheckModuleRulesTask>("checkModuleRules") {
            group = "verification"
            description = "Checks module dependency rules (design spec 5.2)."
            // 구성 시점에 의존 관계를 모아 입력으로 넘깁니다. 태스크 실행 중에는 Project를 만지지 않습니다.
            edges.set(
                target.provider {
                    target.subprojects.associate { sub ->
                        sub.path to
                            sub.configurations
                                .filter { isCheckedConfiguration(it.name) }
                                .flatMap { it.dependencies.withType(ProjectDependency::class.java) }
                                .map { it.path }
                                .toSortedSet()
                                .joinToString(",")
                    }
                }
            )
        }
    }
}

abstract class CheckModuleRulesTask : DefaultTask() {
    /** 모듈 경로 -> 쉼표로 이은 의존 프로젝트 경로들. */
    @get:Input
    abstract val edges: MapProperty<String, String>

    @TaskAction
    fun check() {
        val parsed = edges.get().mapValues { (_, v) ->
            v.split(",").filter { it.isNotEmpty() }.toSet()
        }
        val found = violations(parsed)
        if (found.isNotEmpty()) {
            throw GradleException(
                "Module dependency rule violations:\n" + found.joinToString("\n") { "  - $it" }
            )
        }
    }
}
