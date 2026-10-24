# NextBill 설계·구현 계획 적대적 리뷰

검토일: 2026-10-24. 대상: `specs/2026-10-23-nextbill-design.md`(이하 S), `plans/2026-10-24-nextbill-v1.md`(이하 P). 위치는 검토 시점의 줄 번호다. 구현 코드가 아직 없으므로 아래는 문서에서 확인한 결함과 미정 계약이며, 런타임에서 확인된 버그라는 뜻은 아니다.

## 판정

**수정 후 착수 권고.** Compose·Navigation 3·Hilt·Room·UDF·Clock 주입·정수 금액 선택은 타당하다. 하지만 현재 계획을 그대로 구현하면 결제 이력, 알림 대상, 복원 실패, 날짜 변경에서 제품 계약이 깨질 수 있다. 기술 목록의 최신성보다 이 계약을 닫는 일이 우선이다.

P1 = 구현 착수 전에 수정할 기능·데이터·핵심 주장 문제, P2 = 해당 영역 구현 전에 명시할 품질·검증 문제. P0에 해당하는 실제 운영 사고 증거는 없다.

## P1 — 먼저 수정할 사항 (9건)

### 1. 복원 시 해지 기간 결제를 소급하지 않는다는 규칙을 모델이 표현하지 못한다

- 근거: S:153–155, P:210, 221–226, 460–461.
- 반례: 매월 15일 구독을 1/20 해지하고 4/20 복원한다. `ACTIVE + cancelledAt=null + 원래 anchorDate`만 남으면 캘린더의 2/15·3/15·4/15 결제가 모두 다시 생긴다. `nextBillingDate(today)`만 검사하는 P:245 테스트는 이 문제를 못 잡는다.
- 수정: 활성 구간/중단 구간을 저장하거나, 복원 시 새 구독 구간을 만들고 이전 기록을 보존한다. 과거 이력을 보존하지 않는 단순 예측 도구로 정할 수도 있지만, 그러면 소급 금지 문구와 과거 캘린더의 의미를 명시적으로 변경해야 한다. 금액·기준일 수정과 삭제가 과거 달을 재작성하는지도 함께 정한다.
- 수용 기준: 해지→수개월 후 복원→과거 달 조회, 재해지→재복원, 금액 수정 전후 과거 합계 테스트. 선택한 의미가 S·P·UI 문구·백업 스키마에 일치한다.

### 2. 알림 수신 시 예약된 결제일을 잃어버려 다른 결제를 안내한다

- 근거: P:317, 330, 623–627.
- 반례: 매일 결제 + 7일 전 알림이면 11/16에 발송할 알림 대상은 11/23이다. 수신기가 `nextBillingDate(11/16)`를 다시 구하면 11/16으로 바뀌어 “오늘 결제”라고 안내한다. 지연 발송 판정도 원래 billingDate 없이 할 수 없다.
- 수정: 예약 식별자에 subscriptionId와 occurrence(billingDate), 예약 revision 또는 trigger 식별값을 둔다. PendingIntent extras는 동일성 비교에 쓰이지 않는 점을 고려한다. 수신 시 현재 데이터와 대조해 삭제·수정·해지로 무효화된 예약은 버리고, 실제 예약 대상일로 문구와 지연 정책을 계산한다. DB에 예약 메타데이터를 두는 대안도 가능하다.
- 수용 기준: 매일+7일 전, 수정 직후 옛 브로드캐스트, 다음 결제일까지 지연, 중복 전달에서 잘못된 날짜/중복 알림이 없다.

### 3. 저장 성공과 알림·위젯 갱신 성공이 결합되어 중복 저장·복구 불능이 가능하다

- 근거: P:456–467, 627–628, 667, 781.
- 반례: 새 구독 DB 삽입 후 위젯 갱신이 예외를 내면 저장 UseCase가 성공을 반환하지 못한다. draft id가 여전히 0인 상태로 다시 저장하면 중복 삽입될 수 있다. 동시에 실행된 앱 시작 재예약과 편집 재예약이 오래된 스냅샷으로 서로 덮어쓸 수도 있다. 위젯 갱신 실패 후 다음 자정 예약을 못 하면 갱신 체인이 끊긴다.
- 수정: 데이터 커밋 성공을 UI에 확정하고 파생 상태 갱신의 실패·재시도를 별도 취급한다. reconcile을 직렬화하고 최신 데이터 기준으로 수행한다. 프로세스 종료 후에도 복구할 dirty/revision 상태 또는 재실행 계약을 둔다. 기존 도구만으로 충족 못 하면 WorkManager 직접 사용 금지를 ADR에서 재검토한다. 새 라이브러리 도입 자체가 필수라는 뜻은 아니다.
- 수용 기준: DB 성공 후 scheduler/widget 예외, 저장 연타, save와 import 경합, 오래된 reconcile이 늦게 끝나는 경우, 재시작 후 복구 테스트. UI의 저장 성공이 부가 작업 성공에 종속되지 않는다.

### 4. 전체 복원이 Room·DataStore 사이에서 원자적이지 않다

- 근거: P:464–465, 515, 990.
- 반례: 기존 알림 취소→DB 교체 성공→DataStore 쓰기 실패/프로세스 종료이면 새 구독·옛 설정·취소된 알림 상태가 남는다. Room의 `@Transaction replaceAll`은 DataStore까지 보호하지 않는다. 내보내기도 구독과 설정을 따로 읽으므로 동시 수정과 일관된 스냅샷 계약이 없다.
- 수정: 같은 저장소의 트랜잭션으로 복원 가능한 데이터 배치를 만들거나, 영속 import journal/스테이징과 재시작 복구 절차를 둔다. 단순 try/catch 롤백만으로 프로세스 종료까지 해결됐다고 쓰지 않는다. import/export와 편집을 직렬화하고 실패 시 사용자에게 보여 줄 결과도 정의한다.
- 수용 기준: 각 단계 실패 주입 및 재시작 시 데이터가 전부 이전 상태이거나 전부 새 상태로 수렴한다. DB 교체 실패 때 기존 예약이 영구히 사라지지 않는다.

### 5. Clock은 Flow가 아니어서 켜 둔 화면의 날짜가 바뀌지 않는다

- 근거: P:557, 824, 851–864. 자정 갱신 P:667은 위젯만 갱신한다.
- 반례: 홈을 23:59부터 열어 두면 자정 이후에도 “오늘”, 이번 달 합계, 캘린더 today가 유지된다. 백그라운드 복귀 시에도 Room/설정 값이 같으면 재계산을 보장하지 못한다. 매번 zone을 조회하는 Clock만으로는 방출 트리거가 생기지 않는다.
- 수정: Clock과 별도로 날짜/시간대 변경을 방출하는 테스트 가능한 시간 상태 소스를 둔다. 현지 자정, foreground 복귀, 시간·시간대 변경 시 재계산하고 UI에서 lifecycle-aware 수집한다. 모든 화면용 분 단위 polling은 필요 없다.
- 수용 기준: 열린 화면에서 월말 자정 통과, 복귀 시 날짜 변경, 시간대 이동으로 날짜가 전날/다음 날이 되는 테스트.

### 6. “금융 데이터는 기기 밖으로 나가지 않는다”와 자동 백업이 충돌한다

- 근거: S:46–47, 224; P:107, 826, 991.
- Android Auto Backup은 사용자 데이터를 Google Drive로 업로드한다. 앱의 INTERNET 권한 부재는 이를 막지 않는다. SAF 내보내기도 사용자가 클라우드 문서 제공자를 고를 수 있다. [공식 Auto Backup 문서](https://developer.android.com/identity/data/autobackup)
- 수정: 엄격한 기기 내 보관을 유지한다면 cloud backup을 제외하고 device transfer 정책도 별도 명시한다. 자동 백업을 유지한다면 “앱 서버로 전송하지 않음, OS 백업 및 사용자가 선택한 내보내기 제외”처럼 실제 동작으로 원칙·화면·README를 정정한다. DB만 자동 복원할 때 설정이 사라지는 차이도 설명한다.
- 수용 기준: API 26–30 백업 규칙 및 31+ cloud/device-transfer 규칙을 검증하고 수동 백업과 구분한 데이터 흐름을 문서화한다.

### 7. 백업 검증이 DB 교체 전 안전 조건을 충분히 닫지 않는다

- 근거: P:368, 591–600, 623.
- 검증 공백: 중복/0/음수 id, `Long.toInt()` 충돌 id, `CANCELLED`인데 cancelledAt 없음, ACTIVE에 취소일 존재, interval>365, 잘못된 daysBefore/time/settings, schemaVersion 0·음수, 처리 가능한 날짜 범위, 지나치게 큰 파일/메모/구독 수. “Currency.getInstance 성공”도 지원 통화 정책이나 유효한 소수 자릿수를 보장하는 계약과 같지 않다.
- 결과: 유효 JSON이지만 교체 도중 insert 실패, 화면 편집 불가능한 데이터, 알람 식별 충돌, 과도한 메모리 사용이 생길 수 있다. 구독별 금액 상한만으로 구독 수 무제한 합산의 Long 안전성까지 입증되지 않는다.
- 수정: DTO→검증된 snapshot 단계에서 전체 스키마·관계 불변식·파일 크기·건수·문자열·날짜 범위를 검사한다. 지원 통화와 통화 자릿수를 명시하고 알람 식별에는 Long을 보존하는 URI 등의 전략을 쓴다. 검증 완료 전 취소/교체 금지.
- 수용 기준: 위 잘못된 값마다 기존 DB·설정·예약이 바뀌지 않는다. 경계 합산은 checked arithmetic 또는 명시적 한도로 보호한다.

### 8. `@Immutable`로 성능 검증을 통과시키라는 지침은 잘못된 검증이다

- 근거: S:168–173, P:818–820, 1044–1049.
- `@Immutable`은 객체를 불변으로 만드는 기능이 아니라 개발자가 보장하는 계약이다. Kotlin `List`는 실제 변경 불가능함을 보장하지 않는다. 리포트가 unstable이면 애너테이션을 붙이라는 절차는 전제 검증을 생략한다. 또한 stability 리포트로 Sequence 효율이나 Default dispatcher의 성능 효과를 확인할 수 없다.
- 수정: P:1049의 자동 처방을 삭제한다. 깊은 불변성·컬렉션 소유권을 입증한 모델에만 애너테이션을 쓴다. Kotlin 2.0.20부터 기본인 strong skipping을 반영하고 stable 여부와 실제 성능 측정을 분리한다. 성능 개선을 README에 쓰려면 release/profileable 빌드에서 시작/스크롤 등 대표 시나리오의 trace 또는 benchmark 근거를 확보한다. 미측정이면 “설계 의도”로만 쓴다.
- 수용 기준: 변경 가능한 backing list가 외부에 노출되지 않음, 컴파일러 리포트와 측정 결과 구분, 비교 없는 속도 개선 주장 없음.
- 근거: [안정성 계약](https://developer.android.com/develop/ui/compose/performance/stability/fix), [strong skipping](https://developer.android.com/develop/ui/compose/performance/stability/strongskipping).

### 9. 삭제 완료 시 뒤로 가기가 두 경로에서 발생한다

- 근거: P:937–941. `Gone` 관측과 삭제 성공 `Closed`가 모두 goBack을 호출한다.
- 반례: 삭제가 Room Flow에 null을 방출하고 UseCase가 Closed도 보내면 상세 화면과 그 아래 화면까지 두 번 pop할 수 있다. 정확한 발생 여부는 collector 구현에 달렸지만 현재 계약에는 방지 규칙이 없다. 저장 성공 Channel도 화면 lifecycle 변경 중 처리 보장이 없다.
- 수정: 삭제/해지/외부 삭제를 하나의 terminal UI state로 수렴시키고 entry 단위 idempotent navigation 소비를 정한다. 저장 성공도 saved id와 상태로 표현하거나 이벤트 손실·중복의 처리 계약을 명시한다. 새 구독 저장 직후 권한 시트를 띄우는 흐름과 Saved로 닫는 흐름의 순서도 확정한다.
- 수용 기준: Gone/Closed 순서 양방향에서 pop 1회, 저장 중 회전·background·재생성에서도 저장 중복/성공 처리 누락 없음.
- 근거: [Android UI events 가이드](https://developer.android.com/topic/architecture/ui-layer/events)는 ViewModel보다 짧게 살아 있는 UI에 대한 Channel/stream 이벤트 전달·처리 보장을 경고한다.

## P2 — 영역별 계획을 보완할 사항 (7건)

### 10. 알람 지연·중복·수신기 수명에 대한 플랫폼 계약이 부족하다

- 근거: S:219–222, P:621–644, 664–667.
- setWindow는 Doze/배터리 절약 중 지정한 1시간/15분 창 안 전달을 보장하지 않는다. 따라서 09:00는 희망 시각이고 위젯 자정 갱신도 best effort다. 매번 모든 알람을 취소·재예약하면 이미 due 상태인 다른 알림을 건너뛸 수 있다. goAsync는 무제한 실행 허가가 아니다.
- 수정: 허용 지연·누락/중복 정책, occurrence별 delivered 상태, 모든 경로의 `PendingResult.finish()` finally 처리, 짧은 실행 시간과 예외 처리, 프로세스 사망 후 복구를 정의한다. 알림 한 건 수신마다 전체 예약을 갈아엎기보다 대상 occurrence를 진행시키고 별도 reconcile을 둔다. 위젯이 없을 때 자정 wakeup을 유지할 필요도 검토한다.
- 검증: Doze 강제 진입/복귀, 여러 구독 동시 due, 날짜 넘긴 지연, 재부팅, widget 갱신 실패 후 다음 예약. [알람 전달 제약](https://developer.android.com/develop/background-work/services/alarms/schedule), [PendingResult](https://developer.android.com/reference/android/content/BroadcastReceiver.PendingResult).

### 11. 알림 권한 상태가 화면 세션 수명에 묶여 있다

- 근거: P:905, 910.
- rememberSaveable은 editor를 닫고 새로 여는 세션까지 요청 이력을 보존하는 저장소가 아니다. rationale=false 하나로 최초 요청/재요청 불가를 구분할 수 없다. API 26–32의 앱 전체 차단, 채널 차단, 설정에서 허용 후 복귀도 빠져 있다.
- 수정: 구독 reminder.enabled(사용자 의도)와 실제 delivery capability(런타임 권한·앱·채널)를 분리한다. OS 결과/지속 요청 이력/설정 복귀 재확인을 조합한다. “Denied이면 무조건 영구 거부”로 단정하지 않는다.
- 검증: 최초 요청, dismiss, 거부 후 editor 재진입, 반복 거부, 설정에서 켜고 복귀, 채널만 차단, API 26/32/33+ 행렬. [공식 권한 동작](https://developer.android.com/guide/topics/ui/notifiers/notification-permission).

### 12. 홈 empty 상태와 통화 합계가 정상 데이터를 숨기거나 빈 목록 접근을 유도한다

- 근거: P:280, 824–832, 1016.
- 활성 구독이 0이어도 이번 달 해지 전 결제 합계는 남는다. 현재 Empty는 이를 ₩0으로 덮는다. 반대로 활성 구독은 있지만 첫 결제일이 다음 달이면 dueTotals는 빈 리스트인데 UI는 dueTotals[0]을 요구한다. 위젯에서 다른 통화를 조용히 생략하면 전체 합계처럼 오해할 수 있다.
- 수정: 목록 empty와 요약 empty를 분리하고 통화별 0 표시/기본 통화 정책을 정한다. 위젯은 “KRW 합계” 또는 “외 1개 통화” 등 부분 합계임을 알린다.
- 검증: 전부 해지했으나 이번 달 과거 결제 존재, 전부 미래 시작, USD만 존재, 다중 통화, 0으로 반올림되는 월 환산.

### 13. Navigation 3 채택만 있고 상태 복원·ViewModel 수명 검증은 없다

- 근거: P:778–795, 895–910.
- NavKey 직렬화만으로 편집 draft, 탭별 스택, 스크롤/선택일, entry-scoped ViewModel 복원까지 증명되지 않는다. draft는 ViewModel에만 있으면 process death로 사라진다.
- 수정: saveable back stack, entry saveable state와 ViewModelStore 소유권/정리, SavedStateHandle 또는 saveable draft, lifecycle-aware 상태 수집을 명시한다. 동일 detail 중복 진입 시 entry identity도 정한다. 최신 라이브러리 버전에 맞는 API를 작은 spike로 검증한다.
- 검증: 입력 중 Activity 재생성과 실제 프로세스 재생성 구분, 탭별 stack 복원, entry pop 후 ViewModel 해제, predictive back 취소/완료. 단순 Navigator JVM 테스트로 대체하지 않는다.

### 14. 최신 Android 샘플의 UI 품질·실기기 검증 기준이 부족하다

- 근거: S:68, P:107, 131, 715, 922, 1051.
- 태블릿 전용 화면을 v2로 미뤄도 resize·가로 모드·IME·시스템 바·큰 글꼴 대응은 필요하다. target 36에서는 큰 화면 resizability 변화도 고려해야 한다. Roborazzi 이미지 일치만으로 터치·접근성·시스템 연동을 검증할 수 없다.
- 수정: 최소 API 26/33/36 대표 기기, compact/넓은 창, 200% 글꼴, edge-to-edge/IME insets, TalkBack 달력 날짜·선택·결제 건수, 48dp 터치 영역, 색 이외의 구분 기준을 정의한다. 등록→수정→해지→복원→백업 왕복의 연결된 기기 smoke test를 둔다. 모든 조합을 자동화할 필요는 없지만 최소 자동 경로와 수동 체크는 분리한다.
- 참고: [orientation/resizability](https://developer.android.com/develop/ui/compose/layouts/adaptive/app-orientation-aspect-ratio-resizability).

### 15. 빌드 호환성·의존성 규칙·라이선스 검사가 주장보다 좁다

- 근거: P:16–22, 86–103, 131, 151–158, 992, 1052.
- AGP/Kotlin/KSP/Hilt를 각각 최신으로 고르는 대신 검증된 조합이 필요하다. AGP 9의 built-in Kotlin 처리와 Gradle wrapper 호환 버전을 명시해야 한다. AGP 9.3 공식 요구 Gradle은 9.5.0, JDK는 17이다. 따라서 현재 JDK 17 자체는 오류가 아니다.
- `checkModuleRules`는 api/implementation 프로젝트 edge만 보므로 외부 Android 라이브러리, compileOnly/runtimeOnly 및 variant 설정을 통한 우회까지 강제하지 않는다. 루트 CI 명령은 included build인 build-logic 테스트 실행도 명시하지 않는다.
- 카탈로그 group 검사는 전이 런타임 의존성과 실제 artifact별 라이선스/NOTICE를 보장하지 않는다. Preferences DataStore의 `.preferences_pb`와 내부 protobuf 계열 의존, Glance의 WorkManager 등은 직접 도입과 구분해야 한다. 금지 문자열 검색 결과가 0이어야 한다는 기준은 실제 runtime graph와 충돌할 수 있다.
- 수정: 호환성 표와 작은 전체 스택 빌드 spike, wrapper/checksum 고정, Android built-in Kotlin과 순수 JVM Kotlin 플러그인 구분, included build 테스트 CI 추가. 실제 releaseRuntimeClasspath를 기준으로 직접/전이 허용 목록과 라이선스 전문·NOTICE를 관리한다. 모듈 규칙 검사 범위를 정확히 쓰거나 우회 테스트를 추가한다.
- 참고: [built-in Kotlin](https://developer.android.com/build/migrate-to-built-in-kotlin), [AGP 9.3 호환성](https://developer.android.com/build/releases/agp-9-3-0-release-notes).

### 16. 과도한 모듈 분리의 비용을 설명할 근거가 없다

- 근거: S:179–209, 227; P:40–64.
- app+core 11개+feature 12개+widget으로 제품 모듈만 약 25개다. “앱 크기에 맞게 줄임”이라는 주장만으로 1인 오프라인 앱의 모든 feature api/impl 분리를 설명하기 어렵다. 최신 권장사항이 모든 샘플에 이 개수를 요구하는 것은 아니다.
- 수정: 각 분리에 실제 접근 제어·빌드 격리·교체 가능성 중 무엇이 필요한지 ADR로 설명한다. route contract를 얇은 navigation 영역에 모으거나 작은 feature의 api/impl을 합치는 대안과 비교한다. 포트폴리오 학습 목적이라 유지할 수 있으나 “일반적으로 최적”이라는 주장은 피한다. 도메인 인터페이스/유스케이스/함수 경계의 실익을 README에서 구체 사례로 설명한다.

## 작은 정정 (심각도 건수에서 제외)

- S:73 F1~F10 → F1~F11. S:5 저장소 생성 대기 문구와 현재 상태 정합성 확인.
- P:18 “두 개”인데 허용 추가 항목은 세 개. Task 1 plugin 9개/10개 표기 통일.
- P:865의 2026-11-15는 토요일이 아니라 일요일. 예시를 날짜 포맷 테스트에서 생성한다.
- S:156 주 단위 연 환산 52회는 일 단위 365일과 다른 근사다. 오류로 단정할 것은 아니지만 “추정 환산” 문구로 설명한다.
- P:368 쉼표를 무조건 지우면 `1,5`가 15가 된다. 지원 locale/입력 문법을 고정하고 exponent 표기(BigDecimal이 허용) 및 잘못된 grouping 입력을 테스트한다.
- S:153의 해지 당일 결제 포함과 P:942·966의 “합계에서 제외” 문구를 맞춘다.
- 위젯 250×110dp에 헤더·3행·푸터가 실제로 들어가는지 크기별 검증이 필요하다. Glance는 별도 UI 제약이 있으므로 Compose 폰트/컴포넌트의 그대로 재사용을 전제하지 않는다.

## 최신성 확인 결과와 유지할 결정

2026-10-24 조회한 [Navigation 3 공식 릴리스](https://developer.android.com/jetpack/androidx/releases/navigation3)는 stable 1.2.0을 표시한다. [NiA 지정 커밋의 카탈로그](https://github.com/android/nowinandroid/blob/2982e63/gradle/libs.versions.toml)도 AGP 9.3.2·Kotlin 2.3.0·Hilt 2.59·Nav3 1.2.0을 확인할 수 있다. 이 버전들을 허구/구식이라고 지적하지 않는다. 단, NiA 카탈로그에는 prerelease 항목도 있고 앱 전체에 복사할 호환성 보증서가 아니다. 최신 안정판을 선택한 뒤 실제 조합의 빌드·기기 동작을 검증해야 한다.

유지 권고: 기준일에서 n번째 결제일 계산, 월말 보정, 통화별 합계, Long minor와 명시적 반올림, Clock/dispatcher 주입, Room을 진실의 원천으로 하는 UDF, 네트워크 없는 범위, 필요 없는 이미지/통신 SDK 제외, exact alarm 미사용. Baseline Profile·Macrobenchmark는 유용한 후속 후보지만 기능 계약이 깨진 상태에서 먼저 추가할 필수 장식은 아니다.

## Claude에게 전달할 수정 요청

위 리뷰를 검토하고 **설계 문서와 구현 계획을 먼저 수정해 주세요. 아직 앱 구현은 시작하지 마세요.** 각 지적을 수용/부분 수용/반박으로 표시하고 근거와 수정한 위치를 남겨 주세요. 반박에는 구체적인 반례 해소 또는 공식 문서 근거가 필요합니다.

1. P1 1~9의 모델·데이터 흐름·UI 계약을 먼저 닫고, 인터페이스·백업 형식·태스크·테스트를 함께 갱신합니다.
2. P2 10~16은 각 영역의 수용 기준으로 반영합니다. 새 라이브러리/모듈은 필요성과 대안을 ADR로 비교합니다.
3. 코드 모양을 그대로 재현하는 테스트 대신 실패 주입·시간 경과·동시성·프로세스 재생성·다중 통화 반례를 검증합니다.
4. 특히 알림의 예약 occurrence, 복원의 원자성/재시작 복구, 해지 기간 보존, 저장 성공과 파생 갱신의 분리를 의사코드와 상태 전이로 설명합니다.
5. @Immutable 강제 통과와 미측정 성능 주장을 삭제하고, 최신 버전 채택과 최신 Android 동작 대응을 별도로 검증합니다.
6. 결과물은 수정된 두 문서와 지적별 대응표입니다. 기존 Alex 결정의 의미를 바꾸어야 한다면 변경 제안을 명시하고 기존 결정을 조용히 덮어쓰지 마세요.
