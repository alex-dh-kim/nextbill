plugins {
    alias(libs.plugins.nextbill.android.feature.impl)
}

android {
    namespace = "dev.lioracode.nextbill.feature.settings.impl"
}

dependencies {
    implementation(projects.feature.settings.api)
    implementation(projects.feature.archive.api)
    // 알림 권한 요청과 SAF 파일 선택(rememberLauncherForActivityResult).
    implementation(libs.androidx.activity.compose)
}

// LicensesCatalogTest: 버전 카탈로그의 직접 의존성이 licenses.json에 모두 있는지 검사합니다.
tasks.withType<Test>().configureEach {
    val catalog = rootProject.file("gradle/libs.versions.toml")
    inputs.file(catalog).withPropertyName("versionCatalog")
    systemProperty("nextbill.versionCatalog", catalog.absolutePath)
    val pretendard = rootProject.file("core/designsystem/LICENSE-Pretendard.txt")
    inputs.file(pretendard).withPropertyName("pretendardLicense")
    systemProperty("nextbill.pretendardLicense", pretendard.absolutePath)
    val licenses = file("src/main/res/raw/licenses.json")
    inputs.file(licenses).withPropertyName("licensesJson")
    systemProperty("nextbill.licensesJson", licenses.absolutePath)
}
