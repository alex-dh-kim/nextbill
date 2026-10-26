# NextBill — AI 작업 규칙

오프라인 Android 구독 결제 관리 앱입니다. 기준 문서가 이 파일보다 우선합니다.
- 설계 문서: `docs/superpowers/specs/2026-10-23-nextbill-design.md`
- 구현 계획서: `docs/superpowers/plans/2026-10-24-nextbill-v1.md`
- 리뷰 대응: `docs/reviews/2026-10-24-review-response.md`
- 결정 기록: `docs/adr/` (형식: 맥락 → 결정 → 대안 → 결과)

## 모듈 의존 규칙 (`./gradlew checkModuleRules`가 강제)
- `core:model`은 프로젝트 의존 0개, `core:domain`은 `core:model`만 의존합니다. 둘 다 Android 의존성 0개인 JVM 모듈입니다.
- `feature:*:impl`은 다른 feature의 `impl`을 의존하지 않습니다. 화면 이동은 상대 feature의 `api`(NavKey)로만 합니다.
- `core:database`·`core:datastore`는 `core:data`만 의존할 수 있습니다.
- `feature:*`는 `core:data`를 의존하지 않습니다. Repository 인터페이스는 `core:domain`에 있고, 조립은 `app`이 합니다.
- Android 모듈은 AGP 내장 Kotlin을 씁니다. `org.jetbrains.kotlin.android`를 적용하지 않습니다.
- `nextbill.hilt`는 JVM 모듈에 적용하지 않습니다. JVM 모듈의 `@Inject`는 `javax.inject` + `dagger-compiler`(KSP)로 합니다.

## 객체지향과 함수형의 분배 (설계 문서 §4.3)
| 영역 | 방식 |
|---|---|
| 도메인 규칙(결제일, 합계, 환산) | 함수형: 부수효과 없는 최상위 함수 또는 `object`, 입력·출력은 불변 값, 결제일은 `generateSequence`로 지연 생성 |
| 경계(저장, 알림 예약, 위젯 갱신, 시간) | 객체지향: 인터페이스 + 구현, Hilt로 조립 |
| 화면 상태 | 함수형: Room `Flow` → `combine`·`map` → 불변 sealed `UiState`. ViewModel은 계산을 도메인 함수에 위임 |
| 사용자 액션 | ViewModel의 의도별 메서드 → UseCase. 검증 결과는 직접 정의한 sealed 결과 타입 |

## 코드 규칙
- 새 직접 의존성은 ADR 먼저 씁니다. 허용 목록은 설계 문서 §3.3 + 계획서 Global Constraints입니다.
- 금액은 최소 단위 `Long`입니다. `Double`/`Float`로 계산하지 않고, 합산은 `Math.addExact`로 합니다.
- 시간은 주입한 `Clock`과 `CurrentDateSource`에서만 얻습니다. `LocalDate.now()`·`System.currentTimeMillis()`를 인자 없이 부르지 않습니다.
- 도메인 규칙을 바꿀 때는 표 기반 테스트를 먼저 고치고 실패를 확인한 뒤 구현합니다.
- 화면 결과는 `UiState`로 표현합니다. 일회성 이벤트(Channel)로 화면을 닫거나 결과를 알리지 않습니다.
- 성능 개선은 release 빌드 측정 근거(trace, benchmark)와 함께만 주장합니다. `@Immutable`/`@Stable`을 리포트 통과용으로 붙이지 않습니다.
- Room 버전을 올리는 변경은 `MigrationTestHelper` 테스트와 함께 합니다.
- `runCatching` 대신 `CancellationException`을 다시 던지는 `runCatchingNonCancellation`을 씁니다.
- JVM 모듈(`core:model`·`core:domain`)의 NewApi는 Lint가 아니라 `checkAndroidApi`(`lintDebug`에서 실행)가 막습니다. 지우지 않습니다(ADR-0002).
- 알림 전달에 "최소 1회"·"정확히 1회" 같은 보장 표현을 쓰지 않습니다(ADR-0014).
- 문자열 키는 `values/`(영어)와 `values-ko/`(한국어)에 모두 둡니다.

## 빌드·테스트
```bash
./gradlew -p build-logic test
./gradlew spotlessCheck checkModuleRules assembleDebug lintDebug
./gradlew testDebugUnitTest :core:model:test :core:domain:test :core:testing:test
./gradlew verifyRoborazziDebug     # 기준 이미지는 로컬에서 recordRoborazziDebug로 찍음
```
- JDK 17을 씁니다.
- Kotlin 컴파일러 경고(main·test)와 Android Lint 경고는 빌드 실패입니다. 경고를 남길 때는 그 자리에 `@Suppress`·`@OptIn`·`tools:ignore`를 이유와 함께 답니다.
- spotless가 `NoClassDefFoundError`(ktlint `EditorConfigDefaults`)로 실패하면 코드 문제가 아니라 daemon 문제입니다. `./gradlew --stop` 후 다시 실행합니다.
- CI는 두지 않습니다. 커밋 지점마다 위 명령 전체를 로컬에서 실행합니다(ADR-0011).
- `.claude/settings.json` hook: `.kt`/`.kts` 수정 후 그 파일만 `spotlessApply`, 작업 종료 시 `scripts/test-changed.sh`가 바뀐 모듈의 테스트를 실행합니다.

## 커밋
- 커밋 메시지는 영어로, 무엇보다 의도를 씁니다.
- `Co-Authored-By`·"Generated with Claude Code" 같은 attribution 줄을 넣지 않습니다.
- 커밋은 계획서 "커밋 계획" 표의 지점에서만 합니다. 그 사이에는 `git add`로 스테이징만 합니다.
- 계획과 다른 결정(버전 변경, ADR 수정)은 그 커밋 메시지 본문에 한 줄로 적습니다.
