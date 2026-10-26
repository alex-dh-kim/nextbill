package dev.lioracode.nextbill

import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

/**
 * Kotlin 컴파일러 경고를 오류로 다룹니다(main·test 모두). 경고를 남겨 둘 때는 그 자리에
 * `@Suppress`나 `@OptIn`을 이유와 함께 답니다.
 */
internal fun Project.configureWarningsAsErrors() {
    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions.allWarningsAsErrors.set(true)
    }
}
