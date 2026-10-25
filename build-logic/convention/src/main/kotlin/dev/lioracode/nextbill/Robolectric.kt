package dev.lioracode.nextbill

import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.HasHostTests
import com.android.build.api.variant.HostTestBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register

/**
 * Robolectric 4.17은 SDK 35+ sandbox에 Java 21을 요구하고, 빌드 JDK는 17입니다(ADR-0011).
 * 모든 Android 모듈의 단위 테스트 리소스에 `robolectric.properties`(sdk=34)를 생성해 넣습니다.
 */
internal fun Project.configureRobolectricSdk() {
    val generate = tasks.register<GenerateRobolectricPropertiesTask>(
        "generateRobolectricProperties"
    ) {
        sdk.set(AndroidSdk.ROBOLECTRIC)
        outputDir.set(layout.buildDirectory.dir("generated/robolectric"))
    }
    extensions.getByType(AndroidComponentsExtension::class.java).onVariants { variant ->
        val unitTest = (variant as? HasHostTests)?.hostTests?.get(HostTestBuilder.UNIT_TEST_TYPE)
        unitTest?.sources?.resources?.addGeneratedSourceDirectory(
            generate,
            GenerateRobolectricPropertiesTask::outputDir
        )
    }
}

@CacheableTask
abstract class GenerateRobolectricPropertiesTask : DefaultTask() {
    @get:Input
    abstract val sdk: Property<Int>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        outputDir.get().file("robolectric.properties").asFile
            .writeText("sdk=${sdk.get()}\n")
    }
}
