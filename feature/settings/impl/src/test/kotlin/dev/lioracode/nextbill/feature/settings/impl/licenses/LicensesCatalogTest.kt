package dev.lioracode.nextbill.feature.settings.impl.licenses

import com.google.common.truth.Truth.assertThat
import dev.lioracode.nextbill.feature.settings.impl.realLicenseCatalog
import java.io.File
import org.junit.Test

/**
 * ADR-10: 버전 카탈로그 `[libraries]`의 직접 의존성이 모두 `licenses.json`에 있어야 합니다.
 * 앱에 들어가지 않는 항목(테스트 전용·빌드 도구)은 아래 목록에 이유와 함께 적습니다.
 */
class LicensesCatalogTest {
    private val excluded = mapOf(
        // 테스트 전용
        "androidx.compose.ui:ui-test-junit4" to "test",
        "androidx.compose.ui:ui-test-manifest" to "test (debug only)",
        "androidx.glance:glance-appwidget-testing" to "test",
        "androidx.room:room-testing" to "test",
        "junit:junit" to "test",
        "org.jetbrains.kotlinx:kotlinx-coroutines-test" to "test",
        "org.robolectric:robolectric" to "test",
        "io.github.takahirom.roborazzi:roborazzi" to "test",
        "io.github.takahirom.roborazzi:roborazzi-compose" to "test",
        "com.google.truth:truth" to "test",
        "app.cash.turbine:turbine" to "test",
        // 빌드 도구(애너테이션 프로세서·Gradle 플러그인·debug 도구·버전 정렬용 BOM)
        "androidx.compose:compose-bom" to "build: BOM (no code)",
        "androidx.compose.ui:ui-tooling" to "build: debug only",
        "com.google.dagger:dagger-compiler" to "build: KSP",
        "com.google.dagger:hilt-compiler" to "build: KSP",
        "androidx.room:room-compiler" to "build: KSP",
        "org.jetbrains.kotlin:kotlin-metadata-jvm" to "build: KSP",
        "com.android.tools.build:gradle-api" to "build: plugin",
        "org.jetbrains.kotlin:compose-compiler-gradle-plugin" to "build: plugin",
        "org.jetbrains.kotlin:kotlin-gradle-plugin" to "build: plugin",
        "com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin" to "build: plugin",
        "androidx.room:room-gradle-plugin" to "build: plugin",
        "io.github.takahirom.roborazzi:roborazzi-gradle-plugin" to "build: plugin"
    )

    private val catalog = realLicenseCatalog()

    private fun catalogLibraries(): Set<String> {
        val path = checkNotNull(System.getProperty("nextbill.versionCatalog")) {
            "nextbill.versionCatalog system property is not set"
        }
        val toml = File(path).readText()
        val section = toml.substringAfter("[libraries]").substringBefore("\n[")
        val groupName = Regex("""group\s*=\s*"([^"]+)"\s*,\s*name\s*=\s*"([^"]+)"""")
        val module = Regex("""module\s*=\s*"([^":]+):([^"]+)"""")
        val entries = section.lines().filter { it.contains('=') && !it.trimStart().startsWith("#") }
        // 형식을 모르는 줄이 있으면 조용히 빠뜨리지 않고 실패합니다.
        val unparsed = entries.filter { groupName.find(it) == null && module.find(it) == null }
        check(unparsed.isEmpty()) { "Unparsed [libraries] lines: $unparsed" }
        return entries.map { line ->
            val (g, n) = (groupName.find(line) ?: module.find(line))!!.destructured
            "$g:$n"
        }.toSet()
    }

    @Test
    fun everyDirectLibraryHasLicense() {
        val libraries = catalogLibraries()
        assertThat(libraries).isNotEmpty()
        val listed = catalog.entries.flatMap { it.artifact.lines() }.toSet()
        val missing = libraries - excluded.keys - listed
        assertThat(missing).isEmpty()
    }

    @Test
    fun exclusionsAreStillInCatalog() {
        // 카탈로그에서 빠진 항목이 제외 목록에 남아 있지 않게 합니다.
        assertThat(catalogLibraries()).containsAtLeastElementsIn(excluded.keys)
    }

    @Test
    fun everyLicenseHasFullText() {
        val used = catalog.entries.map { it.license }.toSet()
        assertThat(catalog.texts.keys).containsAtLeastElementsIn(used)
        catalog.texts.values.forEach { assertThat(it.length).isGreaterThan(1_000) }
    }

    @Test
    fun pretendardAndMaterialSymbolsAreListed() {
        val pretendard = catalog.entries.single { it.name == "Pretendard" }
        assertThat(pretendard.license).isEqualTo("OFL-1.1")
        val path = checkNotNull(System.getProperty("nextbill.pretendardLicense"))
        assertThat(catalog.texts.getValue("OFL-1.1")).isEqualTo(File(path).readText().trim('\n'))
        assertThat(catalog.entries.single { it.name == "Material Symbols" }.license)
            .isEqualTo("Apache-2.0")
    }
}
