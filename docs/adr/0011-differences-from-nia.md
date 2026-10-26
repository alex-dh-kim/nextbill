# ADR-0011: Now in Android와 다르게 가는 지점

- 상태: 승인 (2026-10-23), 1차 리뷰(2026-10-24) 반영
- 출처: 설계 문서 §3.1, §5, §6 ADR-11 [설계자 판단, ③은 Alex 결정 §1.2-2]

## 맥락
NextBill은 Now in Android(커밋 `2982e63`, 이하 NiA)의 모듈 구조를 기준점으로 삼습니다. 그대로 복사한 것처럼 보이면 실패입니다(설계 문서 §1.4). NiA를 이해하고 판단해서 고른 구조라는 것을 보여 주려면 다른 점과 그 이유를 적어야 합니다.

## 결정
1. **Repository 인터페이스를 `core:domain`에 둡니다.** NiA는 `core:data`에 두고 domain이 data를 의존합니다. 의존성 역전으로 도메인이 바깥을 모르게 하기 위해서입니다. `core:model`·`core:domain`은 Android 의존성 0개인 JVM 모듈입니다.
2. **UseCase는 여러 Repository를 조합하거나 규칙을 적용할 때만** 만듭니다. 단순 위임용 UseCase는 만들지 않습니다.
3. **모듈과 convention plugin을 앱 크기에 맞게 줄였습니다.** 다만 1인 오프라인 앱에 feature마다 api/impl을 나누는 것은 **필요 이상**이라는 점을 인정하고, 구조를 보여 주기 위한 선택이라고 적습니다.
4. **네트워크·동기화 계층이 없습니다**(ADR-0001).
5. **JVM 도메인 모듈에서 `@Inject`를 쓰려고 `javax.inject`와 `dagger-compiler`(KSP)를 씁니다.** Hilt 플러그인(`nextbill.hilt`)은 JVM 모듈에 적용하지 않습니다. 적용하면 빌드가 실패합니다.
   - `core:domain`은 `com.google.dagger:dagger`(Hilt와 같은 버전)를 `compileOnly`로 둡니다. KSP가 생성하는 `*_Factory`가 `dagger.internal.Factory`를 import하므로 컴파일에 필요합니다. 허용한 `dagger-compiler`의 런타임 짝이고, 앱 런타임에는 Hilt가 전이로 가져옵니다.

그 밖에 빌드 설정에서 NiA와 다른 점:
- minSdk 26, desugaring 없음(ADR-0002). NiA는 minSdk 24 + desugaring이고 Java 11 타깃입니다. NextBill은 Java 17 타깃입니다.
- 설정은 Preferences DataStore입니다. NiA는 proto DataStore + protobuf 플러그인입니다.
- protobuf를 직접 쓰지 않습니다. `releaseRuntimeClasspath`에 보이는 protobuf는 `androidx.datastore:datastore-preferences-external-protobuf:1.3.0`과 `androidx.glance:glance-appwidget-external-protobuf:1.2.0`, 즉 DataStore·Glance가 내장한 사본입니다(Task 25 `dependencyInsight`로 확인). 라이선스는 `licenses.json`에 BSD-3-Clause 개별 항목으로 둡니다.
- 의존 규칙은 `checkModuleRules` 태스크로 빌드가 강제합니다(Task 2).
- Robolectric sandbox SDK를 34로 고정합니다(`nextbill.android.library`·`.application`이 단위 테스트 리소스에 `robolectric.properties`를 생성). Robolectric 4.17은 SDK 35+에 Java 21을 요구하고, 빌드 JDK는 17로 정했기 때문입니다(Global Constraints). API 35/36 동작은 에뮬레이터 직접 확인으로 다룹니다(설계 문서 §9.2). NiA는 CI를 Java 21로 돌립니다.
- Spotless는 루트 `build.gradle.kts`에서 모든 프로젝트에 적용하고, 라이선스 헤더를 넣지 않습니다. NiA는 convention plugin마다 적용하고 Apache 헤더를 넣습니다.
- NiA의 Firebase, flavor, Jacoco, baseline profile, dependency-guard, 관리형 기기 설정은 옮기지 않았습니다.

## 대안
- **NiA 구조를 그대로 쓰기:** 복사본으로 보이고, domain이 data를 의존하는 구조는 도메인 규칙을 JVM 테스트로만 검증하려는 목표와 맞지 않습니다.
- **feature를 api/impl로 나누지 않기:** 1인 앱에는 더 단순하지만, NiA 최신 구조 수준의 모듈화를 보여 주려는 목적(설계 문서 §1.2-2)에 맞지 않습니다.

## 결과
- README에 NiA와의 비교표를 둡니다.
- 아래 버전 표가 `gradle/libs.versions.toml`의 근거입니다. 버전을 올릴 때는 하나씩이 아니라 함께 빌드되는 조합으로 올리고 이 표를 고칩니다.

## 버전 조합 (2026-10-25 확인)

NiA 카탈로그(`2982e63`)에서 설계 문서 §3.3 라이브러리만 옮긴 뒤, 각 저장소의 최신 안정판으로 올렸습니다. alpha·beta·rc는 쓰지 않았습니다. 이 조합으로 `./gradlew assembleDebug spotlessCheck lintDebug`가 성공하는 것을 확인했고, 임시 모듈로 `nextbill.jvm.library`(+ javax.inject·dagger-compiler KSP), `nextbill.android.room`(스키마 export), `nextbill.android.feature.api`(NavKey 직렬화), `nextbill.android.feature.impl`(Hilt ViewModel, Robolectric + Roborazzi 캡처)까지 빌드·테스트한 뒤 임시 모듈은 지웠습니다.

| 항목 | NiA `2982e63` | NextBill | 근거 |
|---|---|---|---|
| Gradle (wrapper) | 9.7.1 | 9.8.2 | services.gradle.org 현재 버전, `distributionSha256Sum` 고정 |
| JDK | 17 (CI는 21) | 17 | AGP 9.4 최소·기본 JDK 17 |
| AGP | 9.3.2 | 9.4.1 | AGP 9.4: 최소·기본 Gradle 9.6.0, JDK 17, 최대 API 37 |
| compileSdk / targetSdk / minSdk | 37 / 36 / 24 | 37 / 36 / 26 | SDK 패키지 `platforms;android-37.0` (API 37.0, ext 22) |
| SDK Build Tools | - | 37.0.0 (aapt2 확인용) | AGP 9.4 최소 36.0.0 |
| Kotlin (AGP 내장 Kotlin, compose·serialization 플러그인) | 2.3.0 | 2.4.21 | Kotlin Gradle plugin 최신 안정판 |
| KSP | 2.3.4 | 2.3.12 | 2.3.10부터 Kotlin 2.4.0 모듈 이름 대응, 2.3.12 최소 AGP 8.12.0 |
| Hilt / dagger-compiler / dagger(compileOnly) | 2.59 | 2.60.1 | 2.59부터 AGP 9 지원(Gradle 9.1+ 필요), 2.60 minSdk 23 |
| kotlin-metadata-jvm (Hilt KSP용) | Kotlin과 같음 | Kotlin과 같음 (2.4.21) | NiA와 같은 방식. Hilt가 Kotlin 2.4 메타데이터를 읽게 함 |
| Compose BOM | 2025.09.01 (`compose-bom-alpha`) | 2026.09.00 (`compose-bom`, 안정판) | |
| Activity | 1.9.3 | 1.13.0 | |
| Core / SplashScreen | 1.15.0 / 1.0.1 | 1.19.1 / 1.2.0 | |
| Lifecycle (+ viewmodel-navigation3, process) | 2.10.0 | 2.11.0 | |
| Navigation 3 | 1.2.0 | 1.2.0 | 최신 안정판이 같음 |
| hilt-lifecycle-viewmodel-compose | 1.3.0-alpha02 | 1.4.0 | |
| Room | 2.8.3 | 2.8.5 | |
| DataStore (Preferences) | 1.2.0 (proto) | 1.3.0 | |
| Glance (+ appwidget-testing) | - | 1.2.0 | NiA에 없음, 안정판 존재 |
| Coroutines | 1.10.1 | 1.11.0 | |
| kotlinx-serialization-json | 1.8.0 | 1.12.0 | |
| JUnit4 / Truth / Turbine | 4.13.2 / 1.4.4 / 1.2.0 | 4.13.2 / 1.4.5 / 1.2.1 | |
| Robolectric | 4.16 | 4.17 (sandbox SDK 34) | SDK 35+ sandbox는 Java 21 필요 |
| Roborazzi (+ compose, gradle plugin) | 1.56.0 | 1.77.0 | |
| Spotless / ktlint | 8.3.0 / 1.4.0 | 8.10.4 / 1.8.0 | |

빌드 전용(앱에 들어가지 않는) 의존성: AGP `gradle-api`, Kotlin·Compose 컴파일러 Gradle 플러그인, KSP·Room·Roborazzi Gradle 플러그인. 모두 위 라이브러리의 일부라 별도 ADR을 두지 않습니다.

확인한 문서:
- AGP 릴리스 노트와 호환 표: https://developer.android.com/build/releases/gradle-plugin
- AGP 9 내장 Kotlin: https://developer.android.com/build/migrate-to-built-in-kotlin
- Gradle 현재 버전·체크섬: https://services.gradle.org/versions/current
- KSP 릴리스: https://github.com/google/ksp/releases
- Dagger/Hilt 릴리스: https://github.com/google/dagger/releases
- AndroidX 버전: https://dl.google.com/android/maven2/ (각 artifact의 `maven-metadata.xml`)
- 그 밖의 라이브러리 버전: https://repo1.maven.org/maven2/ (각 artifact의 `maven-metadata.xml`)
- Glance → WorkManager 전이 의존성: https://dl.google.com/android/maven2/androidx/glance/glance-appwidget/1.2.0/glance-appwidget-1.2.0.pom

## CI를 두지 않음 [Alex 결정, 2026-10-25]

NiA는 GitHub Actions로 PR마다 검사합니다. NextBill은 1인 저장소이고 main에 직접 커밋하므로 CI를 두지 않습니다. 대신 계획서의 커밋 지점마다 `./gradlew -p build-logic test`와 `./gradlew spotlessCheck checkModuleRules assembleDebug lintDebug testDebugUnitTest :core:model:test :core:domain:test :core:testing:test verifyRoborazziDebug`를 로컬에서 실행하고, 결과를 커밋 전에 확인합니다. Roborazzi 기준 이미지는 로컬(macOS)에서 찍습니다.
