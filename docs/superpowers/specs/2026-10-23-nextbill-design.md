# NextBill — 설계 문서 (v1)

- 작성일: 2026-10-23
- 상태: 검토 완료 · Alex 승인 대기
- 저장소: `/Users/alex/Develop/personal/nextbill` → GitHub `alex-dh-kim/nextbill` (public, 승인 후 생성)

> 표기 규칙: **[Alex 결정]**은 Alex가 직접 정한 것, **[설계자 판단]**은 Claude가 정했고 Alex가 검토할 것입니다. 표시가 없는 항목은 둘 중 하나에서 그대로 따라 나온 내용입니다.

---

## 1. 왜 만드는가

### 1.1 배경
Alex는 14년차 Android 개발자로, 다시 **Android 개발자**로 취업하려고 합니다. 2026-10 토스·당근 서류 검토(hiring-panel)에서 확인한 공백은 세 가지입니다.
- Jetpack Compose 실무 결과물이 없음
- 2025.03 이후 네이티브 Android 작업 비중이 줄어듦
- AI 활용이 "본인 설계"인지 "AI가 만든 결과"인지 구분되지 않음

NextBill은 이 공백을 **공개적으로 확인 가능한 결과물**로 메우기 위한 포트폴리오 앱입니다.

### 1.2 면접관이 알아차려야 할 것 [Alex 결정]
1. **AI를 잘 다루되, 바이브코딩이 아니라 설계가 먼저였다.** 설계 문서·결정 기록(ADR)이 코드보다 먼저 커밋되어 있고, AI를 통제하는 규칙과 검증 장치가 저장소에 드러나 있어야 합니다.
2. **트렌드에 뒤처지지 않는다.** Now in Android 최신 구조 수준의 모듈화, 클린 아키텍처, Hilt, Compose를 씁니다.
3. **불필요한 라이브러리를 쓰지 않는다.**
4. **객체지향과 함수형을 필요한 곳에 나눠 쓴다.** 역할과 책임은 객체지향으로 명확히 하고, 규칙과 데이터 흐름은 Kotlin 함수형으로 구현합니다.

### 1.3 성공 기준
- 면접관이 저장소 첫 화면(README)만 보고 위 4가지를 읽을 수 있습니다.
- Alex가 저장소의 모든 설계 결정과 핵심 코드를 **AI 없이 설명할 수 있습니다.** 이것이 안 되면 실패입니다.
- 이력서 기술 칸을 "Jetpack Compose: 개인 프로젝트로 구현(링크) · 실무 운영 경험 없음"으로 바꿀 수 있습니다.

### 1.4 기술적으로 동작해도 실패인 것 (anti-goals)
- Now in Android를 그대로 복사한 것처럼 보이는 것
- 라이브러리를 쌓아 올린 것
- Alex가 설명하지 못하는 코드가 섞인 것
- **전 직장(museLIVE/Quabble)의 설계·코드·문서·규칙 파일을 재현하거나 참고한 것.** 퇴직자 비밀유지서약서(2026-10-08 서명) 1조·5조의 "노하우", "시스템 설계", "업무상 성과물과 아이디어"와 충돌할 수 있습니다. AI 검증 체계도 백지에서 새로 작성합니다.

---

## 2. 제품

### 2.1 한 줄 정의
넷플릭스, 통신비, 보험료처럼 **반복되는 결제를 등록하면 다음 결제일을 알려 주고, 이번 달에 얼마가 나가는지 보여 주는** Android 앱입니다.

### 2.2 원칙
- **서버 없음 [Alex 결정].** 모든 데이터는 기기 안에만 저장합니다. "금융 데이터는 기기 밖으로 나가지 않는다"를 README와 ADR에 설계 원칙으로 씁니다.
- **로그인 없음, 네트워크 권한 없음 [설계자 판단].** `INTERNET` 권한을 선언하지 않는 것 자체를 원칙의 증거로 보여 줍니다.

### 2.3 v1 기능 범위
| # | 기능 | 내용 |
|---|---|---|
| F1 | 구독 등록·수정 | 이름, 금액, 통화, 결제 주기, 첫 결제일(기준일), 카테고리, 색상, 메모, 알림 설정 |
| F2 | 결제 주기 | N일 / N주 / N개월 / N년 (N ≥ 1) |
| F3 | 다음 결제일 계산 | 기준일과 주기로 계산합니다. 월말 처리 규칙은 §4.2를 따릅니다 |
| F4 | 홈: 다가오는 결제 | 다음 결제일 순 목록, 오늘·내일·D-n 표시 |
| F5 | 홈: 합계 | **이번 달 결제 예정 합계**와 **월 환산 고정비**(연 12만 원 → 월 1만 원) |
| F6 | 해지 처리 | 활성 / 해지(보관) 상태. 해지한 구독은 목록에서 빠지고 "해지한 구독"에서 볼 수 있습니다 |
| F7 | 결제일 알림 | 구독별 켜기·끄기, 며칠 전(당일/1/3/7일), 알림 시각(기본 09:00) |
| F8 | 홈 화면 위젯 | Glance. 다가오는 결제 최대 3건과 이번 달 합계 |
| F9 | 백업·복원 | JSON 파일 내보내기·가져오기. 파일 선택은 SAF를 쓰므로 저장소 권한이 필요 없습니다 |
| F11 | 캘린더 [Alex 결정, 2026-10-23] | 월 단위 달력에 결제일을 점으로 표시하고 이번 달 건수·합계를 보여 줍니다. 날짜를 누르면 그날의 결제 목록이 나옵니다. 결제일 계산은 홈과 같은 도메인 함수(§4.2)를 재사용합니다 |
| F10 | 설정 | 기본 알림값, 테마(시스템/라이트/다크), 동적 색상 사용 여부, 기본 통화. 기본 알림값은 **새 구독의 초기값으로만 복사**하고 기존 구독은 바꾸지 않습니다 [설계자 판단] |

### 2.4 v1에서 하지 않는 것 [Alex 결정 + 설계자 판단]
- 기기 간 동기화와 서버 [Alex]. 다만 Repository 인터페이스 뒤에 동기화를 붙일 수 있는 확장 지점은 문서로 남깁니다.
- 자동 환율 변환 [설계자]. 통화가 여러 개면 **통화별 합계**를 따로 보여 주고 환산하지 않습니다.
- 서비스 로고 이미지 [설계자]. 이미지 수집 부담과 상표 문제 때문입니다. 대신 이름 첫 글자 + 색상 아바타를 씁니다.
- 무료 체험 종료 알림, 결제 수단 관리, 지출 통계 차트, 태블릿 전용 레이아웃: v2 후보입니다.
- Firebase, 분석, 광고, 크래시 리포팅 SDK.
- **Play 스토어 출시 [Alex 결정].** 공개 GitHub 저장소로만 공개합니다. 서명 키, 개인정보처리방침, Play 데이터 보안 양식, 릴리스 배포 파이프라인은 만들지 않습니다. 다만 **Play 정책을 지키는 수준으로 설계**해서, 나중에 출시를 결정해도 구조를 바꿀 필요가 없게 합니다(예: ADR-3).

### 2.4.1 범위와 일정 [Alex 결정]
일정 압박이 없고 개발 기간은 짧을 것으로 예상합니다. 그래서 **F1~F10 전체를 v1에 포함**하고 단계별로 덜어내지 않습니다. 구현 계획의 순서는 §11을 따릅니다.

### 2.5 언어 [설계자 판단]
한국어와 영어를 지원하고 기기 언어를 따릅니다. 화면 문구와 디자인 시안은 한국어 기준입니다.

---

## 3. 기술 기준

### 3.1 기준점: Now in Android (커밋 `2982e63`, 2026-10-20 확인)
| 항목 | NiA | NextBill |
|---|---|---|
| AGP / Kotlin / KSP | 9.3.2 / 2.3.0 / 2.3.4 | 같은 계열의 최신 안정판 |
| compileSdk / targetSdk | 37 / 36 | 같음 |
| minSdk | 24 | **26** [설계자 판단] — `java.time`을 desugaring 없이 쓰기 위해 |
| DI | Hilt 2.59 | Hilt |
| 내비게이션 | Navigation 3 (1.2.0), `feature:x:api`·`impl` 분리 | 같은 방식 |
| UI | Compose, Material 3 | Compose, Material 3, Glance(위젯) |
| DB | Room 2.8.x | Room |
| 설정 | DataStore(proto) | **Preferences DataStore** [설계자 판단] — protobuf 플러그인 제거 |
| 테스트 | JUnit4, Truth, Turbine, Robolectric, Roborazzi | 같은 구성 |
| 포맷 | Spotless + ktlint | 같음 |

정확한 버전은 구현 시작 시점의 최신 안정판으로 `gradle/libs.versions.toml`에 고정합니다. alpha와 rc는 Glance처럼 대안이 없는 경우만 허용하고, 그때는 ADR에 이유를 남깁니다.

### 3.2 넣지 않는 라이브러리 [설계자 판단]
Retrofit, OkHttp, Coil, Firebase, protobuf, accompanist, kotlinx-datetime, Timber, 이미지·차트 라이브러리.
- 권한 요청은 `ActivityResultContracts`로 직접 구현합니다.
- 새 라이브러리를 추가하려면 ADR이 필요합니다(§7 규칙).

### 3.3 들어가는 라이브러리 (전체 목록)
- 빌드·주입: Kotlin, Coroutines, Hilt
- AndroidX: Compose BOM(Material 3), Navigation 3, Lifecycle(ViewModel·Compose), Room, DataStore Preferences, Glance, Core·Activity·SplashScreen
- 직렬화: kotlinx-serialization-json (백업 파일용)
- 테스트: JUnit4, Truth, Turbine, Robolectric, Roborazzi, Compose UI Test

---

## 4. 도메인

### 4.1 모델 (`core:model`, 순수 Kotlin)
```kotlin
@JvmInline value class SubscriptionId(val value: Long)

data class Money(val minor: Long, val currency: CurrencyCode) // 최소 단위 정수(원, 센트). 부동소수점 금지

@JvmInline value class CurrencyCode(val value: String)        // ISO 4217

sealed interface BillingCycle {
    val interval: Int                                         // ≥ 1
    data class Days(override val interval: Int) : BillingCycle
    data class Weeks(override val interval: Int) : BillingCycle
    data class Months(override val interval: Int) : BillingCycle
    data class Years(override val interval: Int) : BillingCycle
}

data class Reminder(val enabled: Boolean, val daysBefore: Int, val time: LocalTime)

enum class SubscriptionStatus { ACTIVE, CANCELLED }

data class Subscription(
    val id: SubscriptionId,
    val name: String,
    val price: Money,
    val cycle: BillingCycle,
    val anchorDate: LocalDate,      // 첫 결제일 = 결제일 계산의 기준
    val category: Category,
    val colorKey: ColorKey,
    val memo: String,
    val reminder: Reminder,
    val status: SubscriptionStatus,
    val cancelledAt: LocalDate?,
)
```
위 코드는 모양을 보여 주기 위한 스케치입니다. 이름과 세부 필드는 구현하는 사람이 정합니다. 다만 **불변, value class, 정수 금액, sealed 주기**라는 원칙은 유지해야 합니다.

### 4.2 결제일 규칙 (`core:domain`, 순수 함수) — 이 앱의 핵심 테스트 대상
- **n번째 결제일 = 기준일 + (주기 × n)** 으로 계산합니다. 직전 결제일에 주기를 더해 나가는 방식은 쓰지 않습니다. 예를 들어 1/31 기준 월 결제가 2/28 → 3/28로 밀려 버리는 누적 오차를 막기 위해서입니다.
- **월말 보정:** 해당 월에 그 날짜가 없으면 그 달의 마지막 날로 맞춥니다. 1/31 기준이면 2/28(윤년은 2/29) → 3/31 → 4/30입니다. 연 주기에서 2/29 기준이면 평년에는 2/28입니다.
- **다음 결제일:** `today` 이상인 가장 이른 결제일입니다. 결제일이 오늘이면 다음 결제일은 오늘입니다.
- **해지 [설계자 판단]:** `cancelledAt`은 사용자가 해지 처리한 날짜입니다. 이 날짜 **이전·당일의 결제는 이미 발생한 것**으로 보고 이번 달 합계에 남깁니다. 이후 결제는 생성하지 않으며, 다음 결제일도 알림도 없습니다.
- **복원 [설계자 판단]:** `cancelledAt`을 지우고 원래 기준일(`anchorDate`)과 주기로 다시 계산합니다. 다음 결제일은 늘 그렇듯 `today` 이상인 가장 이른 날입니다. 해지 기간 동안의 결제는 소급해서 만들지 않습니다.
- **이번 달 결제 예정 합계:** 이번 달(1일~말일, 기기 시간대) 안에 들어가는 모든 결제일의 금액 합계입니다. 주 주기는 한 달에 4~5회입니다. 이미 지난 날짜도 이번 달이면 포함합니다. 통화별로 따로 합산합니다.
- **월 환산 고정비:** 일 주기 `금액 × 365 / 12 / N`, 주 주기 `금액 × 52 / 12 / N`, 월 주기 `금액 / N`, 연 주기 `금액 / 12 / N`. 정수 최소 단위로 계산하고 반올림 규칙(HALF_EVEN)을 한 곳에서 정의합니다.
- **알림 시각:** `다음 결제일 − daysBefore`의 `reminder.time`(기기 시간대 기준)입니다.
- 시간은 `java.time.Clock`을 주입받아 계산합니다. 테스트에서 오늘 날짜를 고정할 수 있게 하기 위해서입니다.

### 4.3 객체지향과 함수형의 분배 [Alex 방향 + 설계자 구체화]
| 영역 | 방식 | 구현 |
|---|---|---|
| 도메인 규칙 (결제일, 합계, 환산) | **함수형** | 부수효과 없는 최상위 함수 또는 `object`. 입력은 불변 값, 출력은 새 값입니다. 결제일은 `generateSequence`로 지연 생성합니다 |
| 경계 (저장, 알림 예약, 위젯 갱신, 시간) | **객체지향** | `SubscriptionRepository`, `ReminderScheduler`, `WidgetUpdater`, `Clock` 인터페이스와 구현. Hilt로 조립합니다 |
| 화면 상태 | **함수형** | Room `Flow` → `combine`·`map` → 불변 `UiState`(sealed). ViewModel은 상태를 소유만 하고 계산은 도메인 함수에 위임합니다 |
| 사용자 액션 | **객체지향 + 함수형** | ViewModel의 의도별 메서드가 UseCase를 호출합니다. 유효성 검사는 `Either`류 sealed 결과 타입으로 반환합니다(별도 라이브러리 없이 직접 정의) |

**성능 근거 (README에 쓸 내용):** 함수형으로 썼다고 자동으로 빨라지지는 않습니다. 이 앱에서 실제로 효과가 있는 지점은 세 가지입니다.
1. 불변·안정(stable) UI 모델로 Compose가 recomposition을 생략할 수 있게 합니다.
2. Sequence 지연 계산으로 필요한 결제일만 생성합니다.
3. 무거운 계산을 `Dispatchers.Default`로 보냅니다.

이 효과는 Compose 컴파일러 리포트(stability)로 확인합니다.

---

## 5. 아키텍처

### 5.1 모듈
```
app                              Application, MainActivity, Navigation 3 조립, Hilt 진입점
build-logic/convention           convention plugin
core:model        (JVM)          §4.1 모델
core:domain       (JVM)          §4.2 규칙, UseCase
core:data                        Repository 구현, 백업 직렬화
core:database                    Room (Entity, DAO, 마이그레이션, 스키마 export)
core:datastore                   설정 (Preferences DataStore)
core:notifications               알림 채널, ReminderScheduler 구현, BroadcastReceiver
core:designsystem                테마, 색상·타이포 토큰 (Stitch 시안 기준), 기본 컴포넌트
core:ui                          도메인 모델을 쓰는 공통 Compose UI (금액 표시, 아바타 등)
core:navigation                  NavKey 공통 타입
core:testing                     테스트용 가짜 구현, 고정 Clock, 테스트 규칙
feature:home:api|impl            F4·F5 홈
feature:calendar:api|impl        F11 캘린더
feature:editor:api|impl          F1·F2·F7 등록·수정
feature:detail:api|impl          구독 상세, 해지·삭제
feature:archive:api|impl         F6 해지한 구독
feature:settings:api|impl        F9·F10
widget                           F8 Glance 위젯
```

### 5.2 의존 규칙 (빌드가 강제)
- `core:model`과 `core:domain`은 **Android 의존성 0개**(JVM 모듈)입니다. `core:domain`은 `core:model`만 의존합니다.
- `feature:*:impl`은 다른 feature의 `impl`을 의존하지 않습니다. 화면 이동은 상대 feature의 `api`(NavKey)로만 합니다.
- `core:data`만 `core:database`·`core:datastore`를 압니다. feature는 Repository 인터페이스(`core:domain`에 정의)만 압니다.
- 이 규칙은 convention plugin과 모듈 의존 검사(Gradle 태스크 또는 테스트)로 강제하고, `CLAUDE.md`에도 같은 규칙을 씁니다.

### 5.3 convention plugin [설계자 판단]
`android.application`, `android.application.compose`, `android.library`, `android.library.compose`, `android.feature.api`, `android.feature.impl`, `android.room`, `hilt`, `jvm.library`, `android.test`. 약 10개이며, 필요해질 때만 추가합니다.

---

## 6. Android 플랫폼 결정 (ADR 대상)

| # | 결정 [설계자 판단] | 이유 |
|---|---|---|
| ADR-1 | 서버·네트워크 없음, `INTERNET` 권한 미선언 | 개인 금융 데이터의 기기 내 보관 원칙 [Alex] |
| ADR-2 | minSdk 26 | `java.time`을 desugaring 없이 사용 |
| ADR-3 | **정확한 알람(exact alarm)을 쓰지 않음.** `AlarmManager.setWindow`(1시간 창) 또는 `setAndAllowWhileIdle`을 씁니다 | 결제 알림은 분 단위 정확도가 필요 없습니다. Android 14 이후 `SCHEDULE_EXACT_ALARM`은 기본 거부이고, `USE_EXACT_ALARM`은 Play 정책상 알람·캘린더 앱에만 허용됩니다. 출시하지 않더라도(§2.4) 출시할 수 있는 앱의 제약 안에서 설계합니다 |
| ADR-4 | 알림 재예약 트리거: `BOOT_COMPLETED`, `TIME_SET`, `TIMEZONE_CHANGED`, `MY_PACKAGE_REPLACED`, 앱 시작 시, 구독 저장·삭제 시 | 재부팅과 시간 변경 뒤에도 알림이 유지되게 하기 위해서입니다. 예약 로직은 멱등합니다(전부 취소 후 재예약) |
| ADR-5 | Android 13+ `POST_NOTIFICATIONS`: 첫 알림을 켜는 순간에 요청합니다. 거부되면 설정 화면으로 안내하고 알림 끄기 상태를 표시합니다 | 앱 실행 직후 요청하면 거부율이 높습니다 |
| ADR-6 | 위젯 갱신: 데이터 변경 시 `updateAll`, 자정 날짜 변경 시 1회. 자정 갱신은 **AlarmManager 비정확 알람**으로 예약하고, WorkManager는 직접 쓰지 않습니다 | D-n과 이번 달 합계가 날짜에 따라 바뀝니다. 알림 예약과 같은 구조를 재사용해 라이브러리를 늘리지 않기 위해서입니다 |
| ADR-7 | 백업 형식: 버전 필드가 있는 JSON(`schemaVersion`). **구독과 설정을 모두 포함**합니다. 가져오기는 **전체 교체**하고, 실행 전 확인 대화상자를 띄웁니다 | 병합은 충돌 규칙이 필요해 v1 범위를 넘습니다. 설정을 넣지 않으면 기기를 바꿀 때 설정이 사라집니다 |
| ADR-8 | Android 자동 백업은 켜 두되 DB만 포함합니다 | 기기 교체 시 기본 복원을 위해서입니다 |
| ADR-9 | 금액은 최소 단위 `Long` | 부동소수점 오차를 막기 위해서입니다. 통화별 소수 자릿수는 `java.util.Currency`를 씁니다 |
| ADR-10 | 오픈소스 라이선스 화면은 **직접 관리하는 목록**(`res/raw`)으로 만듭니다. Google oss-licenses 플러그인은 쓰지 않습니다 | 플러그인이 Play 서비스 의존성을 추가하기 때문입니다. 대신 라이브러리 목록과 버전 카탈로그가 어긋나지 않는지 테스트로 검사합니다 |
| ADR-11 | **Now in Android와 다르게 가는 지점**을 명시합니다. ① Repository 인터페이스를 `core:domain`에 둡니다(NiA는 `core:data`에 두고 domain이 data를 의존). 의존성 역전으로 도메인이 바깥을 모르게 하기 위해서입니다. ② UseCase는 여러 Repository를 조합하거나 규칙을 적용할 때만 만듭니다. 단순 위임용 UseCase는 만들지 않습니다. ③ 모듈과 convention plugin을 앱 크기에 맞게 줄였습니다. ④ 네트워크·동기화 계층이 없습니다 | "NiA 복사본"이 아니라 NiA를 이해하고 판단해서 고른 구조라는 것을 보여 주기 위해서입니다(§1.4). README에 NiA와의 비교표를 둡니다 |

---

## 7. AI 개발 체계 (저장소에 공개) [Alex 방향 + 설계자 구체화]
목적은 "AI가 만들었지만, 기준과 판단은 사람이 정했다"는 것을 **파일로 보여 주는 것**입니다. 전 직장 자료는 참고하지 않습니다(§1.4).

- **`CLAUDE.md`(루트):** 모듈 의존 규칙(§5.2), 객체지향·함수형 분배(§4.3), 라이브러리 추가 금지 규칙(ADR 필요), 테스트 기준, 커밋 규칙을 담습니다. 짧고 검증 가능한 문장으로 씁니다.
- **`docs/adr/NNNN-*.md`:** §6의 결정과 이후 결정을 기록합니다. 형식은 맥락 → 결정 → 대안 → 결과입니다.
- **`docs/superpowers/`:** 이 설계 문서와 구현 계획서. 커밋 순서상 코드보다 먼저 들어갑니다.
- **`.claude/settings.json` Hooks:** Kotlin 파일 수정 후 Spotless 검사, 작업 종료 시 관련 모듈 단위 테스트를 실행합니다.
- **커밋:** 작은 단위로 나누고, 메시지에 의도를 씁니다. AI가 쓴 코드는 Alex가 리뷰한 뒤 커밋합니다. AI 사용은 숨기지 않습니다(README에 "AI와 어떻게 일했나" 섹션을 둡니다).
- **CI(GitHub Actions):** PR과 main 푸시마다 빌드, 단위 테스트, Spotless, 스크린샷 테스트 검증을 돌립니다.

---

## 8. 테스트 전략
| 대상 | 방식 | 기준 |
|---|---|---|
| `core:domain` 결제일·합계·환산 | JVM 단위 테스트(표 기반) | §4.2의 모든 규칙과 경계 사례: 1/31, 2/29, 윤년, 연 주기, 주 주기 월 4~5회, 해지, 오늘이 결제일 |
| Repository | 가짜 DAO, Turbine | Flow 방출 순서, 해지 반영 |
| Room | Robolectric 또는 계측 테스트 | 마이그레이션(스키마 export 기준) |
| ViewModel | 가짜 Repository, 고정 Clock | UiState 전이 |
| 화면 | Roborazzi 스크린샷 | 주요 화면 라이트·다크 |
| 알림 예약 | 가짜 AlarmManager 래퍼 | 트리거별 재예약, 멱등성 |
| 백업 | 왕복(내보내기 → 가져오기) 테스트 | 손실 없음, 버전 불일치 처리 |

커버리지 수치를 목표로 하지 않습니다. **도메인 규칙은 빠짐없이 테스트**하는 것이 기준입니다.

---

## 9. 화면 목록 (디자인 시안 대상)
1. **홈:** 이번 달 결제 예정 합계(통화별), 월 환산 고정비, 다가오는 결제 목록(D-n), 추가 버튼. 빈 상태 화면도 필요합니다.
2. **구독 등록·수정:** 이름, 금액·통화, 결제 주기(N + 단위), 첫 결제일, 카테고리, 색상, 알림(켜기·며칠 전·시각), 메모.
3. **구독 상세:** 다음 결제일들(다가오는 3회), 월 환산 금액, 수정·해지·삭제.
4. **해지한 구독:** 보관 목록, 복원·삭제.
5. **설정:** 기본 알림, 테마, 동적 색상, 기본 통화, 백업 내보내기·가져오기, 오픈소스 라이선스, 앱 정보.
6. **홈 화면 위젯:** 4×2 기준, 다가오는 결제 최대 3건과 이번 달 합계.
7. **알림 권한 안내:** 알림을 처음 켤 때의 설명 시트.

8. **캘린더 [Alex 결정]:** 월 이동, 결제일 점 표시(하루 최대 3개), 이번 달 건수·합계, 선택한 날짜의 결제 목록.

**내비게이션 [Alex 결정 + 설계자 판단]:** 하단 탭 3개(홈 / 캘린더 / 설정)를 둡니다. 설정이 탭이 되므로 상단 바의 톱니바퀴는 없앱니다. 등록·상세·해지한 구독은 탭 위에 쌓이는 화면입니다. 해지한 구독은 설정에서 들어갑니다.

### 9.1 Stitch 시안(`docs/design/stitch/`)과 다르게 구현하는 점 [설계자 판단, 2026-10-23 Alex 검토]
시안 코드를 기준으로 하되, 아래 항목만 다르게 구현합니다.
- **빼는 것 (설계 범위에 없는 기능):**
  - 홈: "내역보기 >", "다음 달 예상 고정비 변동" 카드, 정렬 전환 버튼
  - 상세: 이름 아래의 요금제 줄("스탠다드 멤버십")
  - 위젯: "지출 리포트·지난달 대비·목표액" 위젯, 새로고침 버튼, "+ 추가" 버튼
  - 해지한 구독: 필터 아이콘과 정렬 표시
- **정리하는 것:**
  - 등록 화면의 저장 버튼은 상단 "저장" 하나만 둡니다
  - 아바타 색은 선택한 색상을 따릅니다
  - 상세 화면의 "D-Day" 표기는 "오늘"로 통일합니다
  - 상단 바 톱니바퀴를 없앱니다(설정은 탭으로 이동)
- **글꼴:** 시안의 Plus Jakarta Sans에는 한글이 없어서, 시안의 한글은 대체 글꼴로 그려진 것입니다. 앱에서는 **Pretendard**(SIL OFL)를 `res/font`에 넣어 한글·영문·숫자에 모두 씁니다. 라이브러리는 추가하지 않습니다. 시안의 글꼴 크기·굵기·자간 토큰은 그대로 적용합니다.

디자인 원본은 Google Stitch에서 만듭니다 [Alex 결정]. 결과물은 `docs/design/`에 보관하고, 색상·타이포·간격 토큰을 `core:designsystem`으로 옮깁니다. Stitch 시안이 기준이고, 바꾼 부분은 목록으로 남깁니다. Material You 동적 색상을 켜면 시안 색 대신 시스템 색을 씁니다(설정에서 선택).

---

## 10. 구현하는 사람에게 맡기는 것
- 정확한 클래스명, 패키지 구조 세부, UseCase의 단위
- 화면 내부 컴포넌트 분할
- 카테고리 목록의 구체 항목(예: 영상, 음악, 통신, 보험, 클라우드, 기타)과 색상 팔레트 개수
- 반올림 유틸리티 위치, 테스트 이름 규칙
- CI 워크플로 세부(캐시, 매트릭스)

## 11. 구현 순서 원칙 [설계자 판단]
- Stitch 시안은 Alex가 `docs/design/stitch-prompts.md`로 따로 만듭니다. 그래서 **도메인 → 데이터 → 알림·위젯 로직**을 먼저 구현하고, 화면은 시안이 들어온 뒤 `core:designsystem` 토큰부터 만듭니다.
- 시안이 늦어지면 화면을 임시 토큰으로 먼저 만들고, Roborazzi 기준 이미지는 시안을 반영한 뒤에 찍습니다.

## 12. 열린 질문
- 없음. 출시 여부와 범위·일정은 2026-10-23 Alex가 답했습니다(§2.4, §2.4.1).
