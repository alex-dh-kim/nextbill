# Codex 적대적 리뷰 대응표 (2026-10-24)

- 리뷰 대상: `docs/superpowers/specs/2026-10-23-nextbill-design.md`(S), `docs/superpowers/plans/2026-10-24-nextbill-v1.md`(P)
- 리뷰어: Codex (적대적 리뷰, 문서만 읽고 보고)
- 교차검증: Claude가 지적마다 문서 원문과 대조. 리뷰가 인용한 줄 번호는 모두 실제 문서와 일치했습니다.
- Alex 결정 (2026-10-24): ① 해지·복원은 **사용 구간 기록**(A안), ② **클라우드 자동 백업 끄고 기기 간 전송만 허용**(A안)
- 결과: 16건 중 수용 12 · 부분 수용 3 · 반박 1, 작은 정정 7건 모두 수용

아래의 "고친 위치"는 수정 후 문서 기준입니다.

## P1

| # | 지적 | 판정 | 근거 | 고친 위치 |
|---|---|---|---|---|
| 1 | 복원 시 해지 기간 결제를 소급하지 않는다는 규칙을 모델이 표현하지 못한다 | 수용 | 1/20 해지·4/20 복원이면 `cancelledAt=null`로 이력이 사라져 2·3·4월 결제가 다시 생깁니다. Alex가 A안(사용 구간)을 골랐습니다. 금액 수정은 과거 달도 새 값으로 계산하고(이력 없음), 삭제는 기록까지 지운다고 명시했습니다. | S §4.1 `ActivePeriod`·`periods`, §4.2 사용 구간·해지·복원·수정의 범위·삭제, §8 표 / P Task 3(상태 전이, PeriodsTest), Task 6(`validate(existing)`), Task 9(`active_periods`), Task 12(JSON `periods`) |
| 2 | 알림 수신 시 예약된 결제일을 잃어버려 다른 결제를 안내한다 | 수용 | 매일 결제 + 7일 전이면 11/16 수신 때 `nextBillingDate(11/16)`가 11/16을 돌려줘 "오늘 결제"로 잘못 안내합니다. 예약에 결제일·trigger를 싣고, 저장된 예약과 비교해 옛·중복 전달을 버립니다. PendingIntent 구분은 extras가 아니라 data URI로 합니다. | S §4.2 알림 시각 / P Task 5(`PlannedReminder`, `isStillValid`), Task 13(수신 상태 전이, 테스트 7종) |
| 3 | 저장 성공과 알림·위젯 갱신 성공이 결합되어 중복 저장·복구 불능이 가능하다 | 부분 수용 | 결합 문제와 동시 실행 문제는 사실입니다. 저장은 DB 반영으로 성공을 확정하고, 갱신은 앱 범위에서 직렬화·conflate·단계별 실패 격리로 실행합니다. **반박:** WorkManager 재검토는 필요 없습니다. 앱 시작 때 전체 갱신(ADR-4)이 프로세스 사망 후 복구 경로입니다. | S ADR-13 / P Task 8(`DerivedStateRefresher`, 실패 주입·직렬화 테스트), Task 17(앱 시작 refresh) |
| 4 | 전체 복원이 Room·DataStore 사이에서 원자적이지 않다 | 부분 수용 | 사실입니다. 검증한 스냅샷을 임시 파일에 먼저 쓰고 적용 후 지우며, 앱 시작 때 남아 있으면 다시 적용합니다(멱등 전체 교체라 한쪽 상태로 수렴). 알람은 먼저 취소하지 않고 reconcile이 정리합니다. **반박:** 내보내기의 동시 수정 일관성은 단일 사용자·단일 프로세스라 실익이 없어 다루지 않습니다. | S ADR-7 / P Task 7(`ImportStaging`), Task 8(가져오기 상태 전이, 실패 주입 테스트), Task 11(`FileImportStaging` 원자적 rename), Task 17(시작 시 복구) |
| 5 | Clock은 Flow가 아니어서 켜 둔 화면의 날짜가 바뀌지 않는다 | 수용 | 홈을 켠 채 자정을 넘기면 "오늘"이 그대로입니다. 날짜·시간·시간대 변경 브로드캐스트, 앱 복귀, 자정 타이머로 방출하는 `CurrentDateSource`를 두고 모든 화면이 구독합니다. | S §4.2 오늘 날짜, ADR-12 / P Task 7, Task 11(`AndroidCurrentDateSource` 테스트), Task 18·19·21(날짜 변경 테스트) |
| 6 | "금융 데이터는 기기 밖으로 나가지 않는다"와 자동 백업이 충돌한다 | 수용 | Auto Backup은 Google Drive로 업로드됩니다. Alex가 A안을 골랐습니다. API 31+ `cloud-backup` 전부 제외·`device-transfer`만 포함, API 28~30 `requireFlags="deviceToDeviceTransfer"`, API 26~27 백업 끔. 사용자가 직접 내보낸 파일은 사용자가 고른 위치에 저장된다고 명시했습니다. | S §2.2, ADR-8 / P Task 1 Step 3·5, Task 23(내보내기 부제), Task 25 Step 3 |
| 7 | 백업 검증이 DB 교체 전 안전 조건을 충분히 닫지 않는다 | 수용 | 파일 id를 버리고 새로 매겨 중복·음수·Int 충돌을 없앴습니다. 크기·건수·문자열 길이·값 범위·구간 불변식·지원 통화를 교체 전에 모두 검사합니다. 알람 식별은 Long을 보존하는 data URI입니다. 합계는 addExact이고, 한도 안에서 Long 범위를 넘지 않음을 계산으로 남깁니다. | P Global Constraints, Task 3(`SUPPORTED`), Task 4(KDoc 계산), Task 12(검증 순서·파라미터화 테스트), Task 13(data URI, `largeIdsDoNotCollide`) |
| 8 | `@Immutable`로 성능 검증을 통과시키라는 지침은 잘못된 검증이다 | 수용 | `@Immutable`은 개발자의 약속이지 검증 수단이 아닙니다. 자동 처방을 지웠고, 실제로 불변인 타입만 stability 설정 파일에 적습니다. 리포트는 안정성만 보여 준다고 명시했고, 측정하지 않은 성능 주장은 하지 않습니다. | S §4.3 / P Global Constraints, Task 1(`compose_compiler_config.conf`), Task 25 Step 1·README 6 |
| 9 | 삭제 완료 시 뒤로 가기가 두 경로에서 발생한다 | 수용 | `Gone`과 `Closed`가 모두 goBack을 불러 두 번 pop될 수 있었습니다. 끝 상태 `Closed` 하나로 합치고 `Navigator.pop(key)`를 멱등으로 만들었습니다. 저장 완료도 상태(`Saved(id)`)로 표현합니다. 저장 직후 권한 시트를 띄우던 흐름(Claude가 정한 것)은 화면이 닫혀 성립하지 않아, 저장 **전에** 묻도록 바꿨습니다. | P Global Constraints, Task 17(`pop`), Task 20(`EditorPhase`, 저장 흐름), Task 21(`Closed`) |

## P2

| # | 지적 | 판정 | 근거 | 고친 위치 |
|---|---|---|---|---|
| 10 | 알람 지연·중복·수신기 수명에 대한 플랫폼 계약이 부족하다 | 수용 | 가장 중요한 지적입니다. "전부 취소 후 재예약"은 Doze로 늦어지고 있던 다른 구독의 알람을 지워 알림을 잃게 합니다. 예약 상태를 DB에 저장하고 바뀐 구독만 다시 예약합니다(reconcile, 늦어진 유효 알람 보존). 수신기는 `finally { finish() }`와 시간 상한을 둡니다. 위젯이 없으면 자정 알람을 걸지 않습니다. 09:00은 희망 시각(best effort)이라고 명시했습니다. | S ADR-3·4·6 / P Task 5(reconcile 규칙·테스트), Task 8(`leavesLateAlarmOfOtherSubscriptionUntouched`), Task 9(`scheduled_reminders`), Task 13, Task 14(`WidgetPresence`), Task 25 Step 4(Doze 확인) |
| 11 | 알림 권한 상태가 화면 세션 수명에 묶여 있다 | 수용 | 요청 이력을 DataStore에 저장합니다. 사용자 의도(`reminder.enabled`)와 수신 가능 여부(권한·앱 알림·채널)를 `NotificationDelivery`로 분리하고, 앱 복귀 때 다시 계산합니다. API 26~32의 앱·채널 차단도 다룹니다. | S ADR-5 / P Task 7·10(요청 이력), Task 16(`notificationDelivery` 표 테스트), Task 20, Task 21·23(상태 표시) |
| 12 | 홈 empty 상태와 통화 합계가 정상 데이터를 숨기거나 빈 목록 접근을 유도한다 | 수용 | 미래 시작 구독만 있으면 `dueTotals[0]`에서 앱이 죽고, 전부 해지해도 이번 달 결제가 있으면 ₩0으로 덮였습니다. 목록 빈 상태와 합계를 분리하고 `AmountSummary`(기본 통화 → 첫 통화 → 0)로 표시합니다. 위젯은 "외 1개 통화"를 붙입니다. | S §9 홈·위젯 / P Task 4(`summarize`), Task 18(경계 테스트 4종), Task 24 |
| 13 | Navigation 3 채택만 있고 상태 복원·ViewModel 수명 검증은 없다 | 부분 수용 | 입력값은 `SavedStateHandle`에 저장하고, 탭별 스택 복원·엔트리 ViewModel 해제·재생성 테스트와 사전 spike를 넣었습니다. **반박:** predictive back 취소·완료 테스트는 이 앱 규모에 비해 비용이 커서 넣지 않습니다. | P Task 17(spike, `StateRestorationTest`), Task 19·20(SavedStateHandle) |
| 14 | 최신 Android 샘플의 UI 품질·실기기 검증 기준이 부족하다 | 부분 수용 | 글꼴 200% 스크린샷, 48dp 터치 영역, 캘린더 접근성 설명, IME 처리는 자동 검증으로 넣었습니다. 가로 모드·TalkBack·기기 왕복은 API 26/33/36 직접 확인 점검표로 둡니다. **반박:** 기기 조합 자동화 매트릭스는 범위 밖입니다. | S §9.2 / P Task 15·18·19·20, Task 25 Step 4 |
| 15 | 빌드 호환성·의존성 규칙·라이선스 검사가 주장보다 좁다 | 수용 | NiA가 AGP 9 내장 Kotlin을 쓰고 Gradle 래퍼 9.7.1임을 확인했습니다("AGP 9.3은 Gradle 9.5.0 이상"이라는 원문은 찾지 못했지만 모순은 없음). 버전은 조합으로 고르고 근거를 ADR에 남깁니다. CI에 build-logic 테스트를 넣고, 모듈 규칙은 variant·compileOnly·runtimeOnly까지 검사하며 범위 밖을 명시했습니다. 금지 목록은 직접 의존 기준으로 정정하고 전이 의존성은 출처를 ADR에 적습니다. 라이선스는 직접 의존성 + 전이 의존성 묶음 고지로 범위를 명시했습니다. | S ADR-10·11 / P Global Constraints, Task 1 Step 2·7, Task 2(`isCheckedConfiguration`), Task 23, Task 25 Step 2 |
| 16 | 과도한 모듈 분리의 비용을 설명할 근거가 없다 | 반박 | NiA 수준의 모듈화를 보여 주는 것은 포트폴리오의 목적이고 Alex 결정입니다(S §1.2-2). 다만 "1인 앱에는 필요 이상이며 구조를 보여 주려는 선택"이라고 ADR-11과 README에 그대로 씁니다. "일반적으로 최적"이라는 주장은 하지 않습니다. | S ADR-11 ③ / P Task 25 README 4 |

## 작은 정정

| 지적 | 처리 |
|---|---|
| S "F1~F10" → F1~F11, 저장소 생성 대기 문구 | 고침 (S §2.4.1, 머리말) |
| P "두 개"인데 세 개, plugin 9/10개 | 고침 (P Global Constraints, Task 1은 "9개 + module-rules"로 표기) |
| 2026-11-15는 일요일 | 확인 결과 일요일. "(토)" → "(일)", 날짜 포맷 테스트로 고정 (P Task 16·19) |
| 주 단위 52회 근사 | 일 단위와 같은 365/7 기준으로 바꾸고 "환산"으로 표시 (S §4.2, P Task 4 `weeklyAndDailyAgree`) |
| 쉼표를 무조건 지우면 `1,5`가 15 | 세 자리 묶음 쉼표만 허용, 지수·부호 거부 (P Task 6) |
| 해지 당일 결제 포함과 "합계에서 제외" 문구 불일치 | "앞으로의 결제와 알림에서 빠져요"로 고침 (P Task 21·22) |
| 위젯 250×110dp에 3행이 들어가는지, Glance 제약 | 높이별 행 수(110dp 2행, 180dp 3행), 색 값만 재사용·시스템 글꼴 (P Task 24) |

## Codex가 다루지 않았지만 함께 고친 것
- 저장 전 권한 시트로 바꾸면서, 스위치에서 이미 물었으면 저장 때 다시 묻지 않는 규칙을 추가했습니다(P Task 20).
- 알림 권한 요청 이력은 기기 상태라 백업에 넣지 않습니다(P Task 12).

---

# 2차 리뷰 대응 (2026-10-24)

- 리뷰 대상: 1차 반영 후의 S(336줄), P(1295줄), 이 대응표
- 리뷰 원문: `docs/reviews/2026-10-24-adversarial-review-round2.md`
- 교차검증: 지적마다 원문과 대조했습니다. 인용한 줄 번호는 모두 실제 문서와 일치했습니다.
- Alex 결정 (2026-10-24, 2차): ③ 첫 결제 전 구독은 해지 대신 삭제만, ④ 해지한 구독은 읽기 전용 상세, ⑤ 한도는 구독 1,000건·구독당 사용 구간 50개
- 결과: 11건 중 수용 9 · 부분 수용 2 · 반박 0, 추가 정정 9건 중 수용 8 · 동의(이미 허용된 절충) 1
- 아래 "고친 위치"는 2차 수정 후 문서 기준입니다. 모두 **계획에 반영됨**이고, 구현·검증은 아직 하지 않았습니다.

## P1

| # | 지적 | 판정 | 근거·결정 | 고친 위치 |
|---|---|---|---|---|
| R2-1 | 재부팅 후 DB 예약은 남고 OS 알람은 사라져 reconcile이 재등록을 생략 | 수용 | AlarmManager 문서에 따르면 재부팅하면 등록된 알람이 지워집니다. 강제 종료도 알람을 지우고 알려 주는 브로드캐스트가 없습니다. 상태 3단계 대신 `ReconcileMode.REAPPLY`를 두었습니다. 프로세스 시작 후 첫 갱신(앱 시작·BOOT·MY_PACKAGE_REPLACED)은 DB의 유효한 예약을 OS에 전부 다시 등록합니다. 같은 PendingIntent는 기존 알람을 대체하므로 다시 적용해도 알람은 하나입니다. 시각이 지났지만 결제일 전인 예약은 다시 등록해 곧바로 울리고, 결제일이 지난 것은 버립니다. PendingIntent 존재 여부는 쓰지 않습니다. 기기 간 전송으로 가져온 DB도 첫 실행의 REAPPLY로 재등록됩니다. | S ADR-4, §8 / P Task 5(`ReconcileMode`, 테스트 2개), Task 8(`reapplyRestoresAlarmsLostByOs`, `diffDoesNotTouchUnchangedAlarm`), Task 13(`RescheduleHandler.handle(action)`, `bootReappliesLostAlarms`), Task 17 |
| R2-2 | 수신기 처리와 reconcile이 같은 임계 구역에 있지 않음 | 수용 | 수신 처리를 `DataCoordinator.exclusive` 안으로 옮기고, `removeIfMatches(occurrence)`로 받은 occurrence만 소비합니다. 전달 보장은 "최소 1회"로 정했습니다(새 ADR-14). notify 뒤 소비 전에 죽으면 REAPPLY로 최대 한 번 더 오고, 구독별 고정 tag라 알림은 대체됩니다. 수신 처리는 위젯 갱신을 기다리지 않습니다. | S ADR-14, §8 / P Global Constraints, Task 7(`removeIfMatches`), Task 9(DAO), Task 13(상태 전이, 동시성 테스트 3개) |
| R2-3 | 가져오기 복구 완료 전 접근·쓰기 차단 없음 | 수용 | `AppStartup`(`Recovering → Ready \| RecoveryFailed`)을 두었습니다. 시작 화면은 Ready까지 유지하고, 실패하면 앱 전체를 복구 화면("다시 시도"·"가져오기 취소")으로 바꿔 편집 진입을 막습니다. 가져오기 적용과 저장·해지·수신은 같은 잠금을 씁니다. 적용은 한 번 다시 시도합니다. 끝나면 `dataGeneration`을 올려 모든 탭 스택을 루트로 되돌리고, 사라진 id로 저장하면 `Missing`입니다. 취소 시 일부만 바뀌었을 수 있다는 점은 확인 문구에 적습니다. WorkManager는 쓰지 않습니다. | S ADR-7, §8 / P Task 8(시작 상태 전이, 가져오기 전이, 테스트 7개), Task 17(`RecoveryScreen`, `resetToRoots`), Task 23 |
| R2-4 | 미래 시작 구독의 해지 불가 | 수용 | 10/24에 anchor 12/01 구독을 해지하면 `require`가 실패하는 것을 확인했습니다. **Alex 결정 ③:** 첫 결제 전(구간 1개, anchor > 오늘)이면 해지 대신 삭제만 제공합니다. 시스템 날짜를 되돌려 마지막 구간이 시작 전이면 그 구간을 지우고 직전 해지 상태로 돌아갑니다. 정상 액션에서 예외가 새지 않도록 UseCase가 `NOT_STARTED`를 돌려줍니다. | S §4.2, §9-3 / P Task 3(`isBeforeFirstBilling`, 상태 전이, 테스트 2개), Task 8(`ActionError`), Task 21(`BeforeFirstBilling`) |
| R2-5 | 앱이 만든 데이터를 자기 백업 코덱이 거부할 수 있음 | 수용 | **Alex 결정 ⑤:** 구독 1,000건, 구독당 구간 50개, 첫 결제일 1900~2200을 저장 경로(`validate`·저장·복원)와 가져오기에 같이 적용합니다. 파일 크기 한도는 1,000,000자에서 8,000,000자로 올렸고, 이스케이프가 가장 긴 메모로 채운 최대 상태의 왕복 테스트로 보장합니다. 내보내기 실패는 성공으로 표시하지 않습니다. | S §4.2 한도, ADR-7, §8 / P Task 3(상수), Task 6(`ANCHOR_OUT_OF_RANGE`), Task 8(`saveRejectsAtLimit`, `restoreAtPeriodLimit`), Task 12(`largestValidStateRoundTrips`), Task 20(`yearRange`), Task 23 |
| R2-6 | OS 알람과 예약 DB 사이 실패 복구 미정의 | 부분 수용 | 문제는 사실입니다. `Pending/Applied/Consumed` 상태 대신 두 규칙으로 닫았습니다. ① 예약은 DB에 먼저 기록한 뒤 OS에 등록합니다(취소는 OS 먼저, DB 나중). ② 다음 프로세스 시작의 REAPPLY가 DB 기준으로 OS를 다시 맞춥니다. 같은 프로세스 안에서 OS 등록만 실패한 경우에는 다음 프로세스 시작까지 그 알람이 없을 수 있습니다. 이 잔여 손실을 ADR-3(best effort)에 맞춰 허용합니다. `CancellationException`은 삼키지 않습니다. | S ADR-4, ADR-13 / P Global Constraints, Task 8(`SyncRemindersUseCase` 적용 순서, `recordIsWrittenBeforeOsSchedule`, `cancellationIsNotSwallowed`) |

## P2

| # | 지적 | 판정 | 근거·결정 | 고친 위치 |
|---|---|---|---|---|
| R2-7 | `core.model.*` 와일드카드 안정성 선언 | 수용 | 제가 넣은 와일드카드입니다. 필드가 모두 불변 값인 타입만 한 줄씩 적고, `Subscription`·`UserSettings`는 빼서 UI 모델이 필요한 값만 복사하게 했습니다. 리포트는 "추론"과 "설정으로 선언"을 나눠 기록합니다. | P Task 1, Task 25 Step 1 |
| R2-8 | 캘린더의 해지 구독 결제를 누르면 상세가 즉시 닫힘 | 수용 | **Alex 결정 ④:** 해지한 구독은 읽기 전용 상세(해지일 표시, "다시 사용"·"삭제", 편집 없음)로 보여 줍니다. `Closed`는 삭제됐거나 이 화면에서 해지·삭제를 마쳤을 때만입니다. 해지 목록 행도 이 화면으로 갑니다. | S §9-3·4 / P Task 19, Task 21(`DetailMode`, 테스트 4개), Task 22 |
| R2-9 | 작은 화면에서 캘린더 7열·여백·48dp 동시 충족 불가 | 수용 | 7×48=336에 여백 40을 더하면 376dp라 360dp 기기에서 깨집니다. 창 폭 376dp 미만이면 여백을 12dp로 줄이고 카드 안쪽 여백을 0으로 해서 360dp에서 48dp를 확보합니다. 360dp 미만은 좁아지는 것을 허용하되 이웃 칸과 터치 영역이 겹치지 않게 합니다. 폭·높이를 모두 테스트합니다. | S §9.2 / P Task 19 |
| R2-10 | 2×2 레이아웃으로 줄일 provider 설정 없음 | 수용 | `minResizeWidth/Height 110dp`를 추가했습니다. 크기 판단은 dp(`LocalSize`)로만 하고, API 26/33/36 런처에서 축소·확대를 직접 확인합니다. | S §9-6 / P Task 24, Task 25 Step 4 |
| R2-11 | 실제 프로세스 복원·저장 실패 계약 부족 | 부분 수용 | 같은 `SavedStateHandle` 객체를 재사용하는 테스트는 직렬화를 검증하지 못한다는 지적이 맞습니다. JSON만 꺼내 새 handle로 복원하는 테스트와 `adb shell am kill` 직접 확인으로 나눴습니다. 저장·해지·삭제의 DB 예외는 메시지, `busy` 해제, 입력 보존, 재시도로 정의했습니다. **허용한 위험:** DB 저장 직후 `Saved` 전에 프로세스가 죽는 구간은 막지 않습니다. 저장 상태가 백그라운드 전환 때만 보존되어 실제로 겪을 가능성이 매우 낮기 때문이며, ADR-13과 README에 적습니다. | S ADR-13, §8 / P Task 8(`SaveError`), Task 20(`EditorMessage`, 테스트 4개), Task 21, Task 25 |

## 추가 정정

| 지적 | 처리 |
|---|---|
| #3 "앱 시작으로 복구"는 R2-1/2/6이 있어야 성립 | 동의. REAPPLY와 DB 우선 기록으로 성립하게 했습니다(R2-1·R2-6). |
| #4 "단일 사용자·단일 프로세스라 동시 수정 없음"은 근거 부족 | 동의하고 1차 반박 문구를 철회합니다. 수신기·화면·앱 시작 작업이 한 프로세스 안에서도 겹칩니다. `DataCoordinator`로 막습니다(R2-3). 내보내기 전체를 원자화하지는 않습니다. |
| #13 predictive back 수동 smoke | 수용. P Task 25 Step 4, S §9.2 |
| #15 전이 의존성 라이선스를 Apache-2.0 한 줄로 취급 | 수용. 해석된 아티팩트의 POM 라이선스를 확인해 같은 라이선스끼리만 묶습니다(P Task 23, Task 25 Step 2). |
| "나중에"도 요청 이력을 기록 | 수용, 실제 버그. OS 대화상자를 띄웠을 때만 기록합니다(S ADR-5, P Task 16·20 `onPermissionLater`/`onPermissionRequestResult`). |
| 수신기가 위젯 갱신까지 기다려 8초 설명과 불일치 | 수용. 수신 처리는 sync까지만 하고 위젯은 `request()`로 넘깁니다. 시간 초과 시 다음 REAPPLY로 복구된다고 고쳤습니다(P Task 13). |
| 알림 id `Long.hashCode()` 충돌 | 수용. `tag = "subscription-{id}"`, `id = 1`(P Task 13, 테스트 `distinctSubscriptionsGetDistinctNotifications`) |
| `git diff`만 리뷰하면 새 파일이 빠짐 | 수용. 새 파일 목록을 함께 보고, 묶음 커밋 직전에 `git diff --cached` 전체를 다시 리뷰합니다(P 커밋 계획). |
| 모듈 수 유지·WorkManager 미도입 | 리뷰가 선택 가능한 절충으로 인정했습니다. 변경 없음. |

---

# 3차 리뷰 대응 (2026-10-24)

- 리뷰 대상: 2차 반영 후의 S(345줄), P(1398줄), 이 대응표
- 리뷰 원문: `docs/reviews/2026-10-24-adversarial-review-round3.md`
- 교차검증: 지적마다 원문과 대조했습니다. R3-1은 Android 12 AOSP 소스를 직접 받아 확인했습니다.
  - `apex/jobscheduler/service/java/com/android/server/alarm/AlarmManagerService.java`(android12-release)
    - 547행 `private static final int DEFAULT_MAX_ALARMS_PER_UID = 500;`
    - 2028–2034행: 한도에 닿으면 `throw new IllegalStateException(errorMsg);`
    - 790–793행: 원격 설정으로 500보다 낮출 수 없음
- Alex 결정 (2026-10-24, 3차): ⑥ 한 번에 4건 이상이면 묶음 알림 하나, 3건 이하는 각각, 결제일이 지난 알림은 갱신 때 지움
- 결과: 4건 모두 수용. 리뷰 밖에서 같은 종류의 한도 1건을 추가로 찾았습니다(아래 R3-extra).
- 2차의 R2-1·R2-6 대응(구독마다 OS 알람을 걸고 `ReconcileMode.REAPPLY`로 다시 거는 방식)은 아래 R3-1로 **대체**됩니다. 모두 **계획에 반영됨**이고, 구현·검증은 아직 하지 않았습니다.

| # | 지적 | 판정 | 근거·결정 | 고친 위치 |
|---|---|---|---|---|
| R3-1 | 구독 1,000건마다 OS 알람을 걸면 앱당 동시 알람 한도(500)에 걸림 | 수용 | 위 AOSP 원문으로 확인했습니다. 1,000건 한도를 정할 때 제가 이 상한을 확인하지 않았습니다. 구독별 occurrence는 DB에만 기록하고, OS에는 **가장 이른 알림 시각의 알람 하나**만 겁니다(위젯 자정 알람을 합쳐 앱 전체 2개 이하). 알람이 울리면 시각이 지난 기록을 한 번에 처리하고 다음 알람을 겁니다. OS에 건 시각은 메모리에 두어, 새 프로세스의 첫 sync는 항상 다시 겁니다(재부팅·강제 종료·기기 전송 복구). 같은 프로세스에서는 가장 이른 시각이 바뀔 때만 다시 겁니다. 등록이 실패하면 다음 sync가 다시 시도하므로, 2차에서 허용했던 "같은 프로세스 안 등록 실패 손실"도 줄었습니다. 구독 한도 1,000건(Alex 결정 ⑤)은 유지합니다. | S ADR-3·4, §4.2 알림 시각, §8 / P Global Constraints, Task 5(`ReminderRecordAction`, `nextAlarmAt`, `dueReminders`, `thousandSubscriptionsNeedOneAlarm`), Task 7(`ReminderAlarm`), Task 8(sync 의사코드, `thousandRemindersUseOneOsAlarm`, `firstSyncAfterProcessStartAlwaysSetsAlarm`), Task 13(PendingIntent 하나, `setsOneInexactWindowAlarm`), Task 14, Task 25 Step 4 |
| R3-2 | 공유 Mutex는 배타만 보장하고 RecoveryFailed 이후 작업 금지는 보장하지 않음 | 수용 | 실패한 가져오기 동안 잠금을 기다리던 저장이 잠금이 풀린 뒤 DB를 바꾸는 예가 실제로 가능합니다. `DataCoordinator.write`가 **잠금을 얻은 뒤** 준비 상태(`state`)와 데이터 세대(`generation`)를 검사해 `NotReady`·`Stale`로 거부합니다. 화면은 열 때의 세대를 쓰기에 넘깁니다. 알림 처리는 `writeWhenReady`로 복구가 끝날 때까지 기다리고, 복구가 실패하면 아무것도 소비하지 않습니다. 복구·가져오기는 검사 없는 `recovery` 블록을 써서 재진입·교착을 피합니다. 설정 쓰기도 `UpdateSettingsUseCase`로 같은 관문을 지납니다. | S ADR-7, §8 / P Task 8(`Admission`, `DataCoordinator`, 관문 테스트 7개), Task 20–23(generation 전달, `NOT_READY`) |
| R3-3 | staging 생성·삭제 실패가 상태 전이에 없음 | 수용 | 적용은 성공했는데 삭제가 실패하면 그 뒤의 편집이 다음 시작에서 덮이는 경로를 확인했습니다. `write`·`clear`는 `renameTo`·`delete`의 false를 `IOException`으로 알립니다. 가져오기는 staging 쓰기부터 적용·삭제·세대 갱신·sync까지 `NonCancellable`로 실행합니다. 쓰기 실패는 아무것도 바꾸지 않은 `StagingFailed`, 적용 2회 실패나 삭제 실패는 `RecoveryFailed`입니다. `Ok`는 삭제와 세대 갱신이 끝난 뒤에만 돌려줍니다. 시작 복구·취소도 같은 규칙입니다. | S ADR-7, §8 / P Task 7(`ImportStaging` 계약), Task 8(가져오기·시작 의사코드, 마무리 테스트 8개), Task 11(`FileImportStaging`, 실패 테스트 2개), Task 23(`ImportStagingFailed`) |
| R3-4 | "최소 1회"와 "최대 한 번 더"는 best effort 정책과 불일치 | 수용 | 등록 실패, 권한 차단, 결제일 경과, 시간 초과 뒤 재시작 없음은 모두 미전달을 허용하므로 "최소 1회"는 성립하지 않습니다. 게시 뒤 소비 전에 반복해서 끊기면 재게시 횟수의 상한도 없습니다. ADR-14를 "best effort, 소비 전 occurrence는 다시 게시될 수 있음, 횟수 상한 없음, 같은 구독의 활성 알림은 대체"로 고쳤습니다. 테스트는 `repeatedInterruptionAfterPostKeepsOneActiveNotification`(3회 연속 주입)으로 바꿨고, "곧바로 울림"도 지웠습니다. | S ADR-14 / P Global Constraints, Task 8, Task 13 |
| R3-extra | (리뷰 밖, Claude 발견) 앱당 표시 알림 50개 한도 | 수용 | R3-1을 검증하면서 같은 종류의 한도를 확인했습니다. Android 12 AOSP `services/core/java/com/android/server/notification/NotificationManagerService.java` 347행 `static final int MAX_PACKAGE_NOTIFICATIONS = 50;`, 6593–6597행에서 한도에 닿으면 `"Not showing more."`를 남기고 `return false`합니다. 같은 파일의 초당 5회 제한(348행)은 6572–6574행 조건상 기존 알림을 갱신할 때만 적용되어 해당하지 않습니다. **Alex 결정 ⑥:** 4건 이상이면 묶음 알림 하나로 보여 주고, 결제일이 지난 알림은 갱신 때 지웁니다. | S ADR-14, §8 / P Task 5(`groupForPosting`), Task 7(`ReminderPoster.pruneExpired`), Task 8, Task 13(묶음 문구), Task 25 Step 4 |

## 함께 정정한 불일치

| 지적 | 처리 |
|---|---|
| S ADR-7 "항상 전부 이전/전부 새것"과 "가져오기 취소"의 일부 교체 절충이 충돌 | ADR-7에 취소 예외와 확인 문구를 명시했습니다. |
| 2차 대응표는 저장 직후 종료의 중복 위험을 ADR-13에 적었다고 했으나 실제 본문에 없음 | 맞는 지적입니다. 이번에 S ADR-13 본문에 넣었습니다. "가능성이 매우 낮다"는 측정 근거가 없어 지우고 "발생 빈도는 측정하지 않았음"으로 바꿨습니다(S ADR-13, P Task 8). |

---

# 4차 리뷰 대응 (2026-10-25)

- 리뷰 대상: 3차 반영 후의 S(345줄), P(1478줄), 이 대응표
- 리뷰 원문: `docs/reviews/2026-10-25-adversarial-review-round4.md`
- 교차검증: 지적마다 인용된 P 줄과 대조했고 모두 일치했습니다. R4-4 반례 B는 P Task 17의 `rememberNavBackStack`(프로세스 재생성 후 복원)과 Task 9의 `autoGenerate` id로 성립 조건을 확인했습니다. `BroadcastReceiver.goAsync` 문서는 정확한 원문을 찾지 못했습니다(R4-5의 결론은 P의 의사코드만으로 성립합니다).
- Alex 결정 (2026-10-25, 4차): ⑥ 수정 — 묶음 기준을 "한 번의 처리에서 4건 이상"에서 "화면에 남은 알림과 새 알림을 합쳐 4건 이상"으로 바꿈. 사용자가 지운 알림은 되살리지 않음
- 결과: 5건 모두 수용. R4-1을 고치면 드러나는 문제 1건을 추가로 찾았습니다(R4-extra). 모두 **계획에 반영됨**이고, 구현·검증은 아직 하지 않았습니다. 실기기 동작이나 시간은 측정하지 않았습니다.

| # | 지적 | 판정 | 근거·결정 | 고친 위치 |
|---|---|---|---|---|
| R4-1 | 지난 earliest 알람을 sync마다 now로 다시 걸어 지연이 누적됨 | 수용 | `nextAlarmAt`이 지난 시각을 now로 바꾸고 그 값을 applied와 비교하므로 맞습니다. 3차의 `leavesLateRecordOfOtherSubscriptionUntouched`는 `alarm.nextAt == now`를 기대값으로 고정해 이 동작을 정답으로 박아 두었습니다. 비교는 논리 예정 시각(`earliestTrigger`, now로 바꾸지 않음)으로 하고, OS에 넘길 때만 `maxOf(trigger, now)`를 씁니다. 새 프로세스(Unknown), 등록 실패(Unknown), 재시도 알람(Retry)은 어떤 `At`과도 달라 다시 겁니다. | S ADR-3·4, §8 / P Task 5(`earliestTrigger`), Task 8(sync 의사코드, `overdueAlarmIsNotRestartedByLaterSyncs`, `newProcessRearmsOverdueAtNow`, 기존 테스트 2개 기대값 수정) |
| R4-2 | 한 배치 기준 묶음으로는 누적 활성 알림 50개 한도를 못 막음 | 수용 | 한 건씩 따로 오면 매번 Single이 생기고, `pruneExpired`는 결제일이 오늘·미래인 알림을 지우지 않습니다. 3차의 Fake에도 한도가 없었습니다. **Alex 결정 ⑥ 수정:** `planShown`이 화면에 떠 있는 항목(`poster.shown()`, 사용자가 지운 것은 없음)과 새 항목을 합쳐 `groupForPosting`합니다. 그래서 화면의 앱 알림은 항상 Single 3개 이하 또는 Summary 1개이고, 전체 내역은 홈 화면에서 봅니다. `show`는 바뀐 것만 게시하고, 항목이 늘어난 묶음만 알림음을 냅니다. `FakeReminderPoster`는 활성 50개에서 새 tag를 조용히 거부하고 `rejected`에 기록합니다. | S ADR-14, §8 / P Task 5(`ShownReminder`, `planShown`, 테스트 6개), Task 7(`ReminderPoster`, Fake), Task 8(`fiftyOneDeliveredOneAtATimeStayWithinCap` 등 3개), Task 13(extras, `show` 차이 반영, 테스트 4개), Task 25 |
| R4-3 | import/startup에서 sync 예외가 데이터 처리 결과로 전파됨 | 수용 | 가져오기는 `sync()`를 감싸지 않고 불러, 데이터 교체 뒤 알람 실패가 `Ok`를 막고 `ImportError`로도 매핑되지 않습니다. 시작 경로도 같습니다. 처리기 없는 `launch`의 예외는 Android에서 기본적으로 앱을 종료시킵니다(제 보충). sync는 알람 등록 예외를 스스로 잡아 applied를 Unknown으로 둡니다. 가져오기·시작·취소에서 데이터가 확정된 뒤의 sync는 `runCatchingNonCancellation`으로 감쌉니다. `start()`는 취소 외의 예외를 내보내지 않습니다. | S ADR-13, §8 / P Task 8(의사코드, `alarmFailureAfterImportIsStillOk`, `startupAlarmFailureStaysReady`, `startupStorageExceptionDoesNotEscape`) |
| R4-4 | 메모리 generation과 SavedStateHandle generation의 수명이 다름 | 수용 | 반례 A·B 모두 성립합니다. B에서 기존 구독 수정은 새 id 때문에 `Missing`이 되지만, 새 구독 입력(id=0)은 막히지 않습니다. 세대를 Room `dataset_meta`에 영속 저장하고, `replaceAll`과 같은 트랜잭션에서 +1합니다. "가져오기 취소"는 `bumpRevision()`으로 먼저 올린 뒤 staging을 지웁니다. `start()`가 처음 이 값을 읽습니다. DB 버전 1이라 마이그레이션은 없습니다. | S ADR-7, §8 / P Task 7(`revision`, `bumpRevision`), Task 8(`setGeneration`, 재생성 테스트 3개), Task 9(`DatasetMetaEntity`, `replaceAllBumpsRevisionAtomically`), Task 11(`revisionPersistsAcrossReopen`), Task 20, Task 25 |
| R4-5 | 8초 timeout의 finally 안에 무제한 NonCancellable sync | 수용 | `finally`의 `NonCancellable` sync는 시간 제한 밖이고, "DB 몇 번"은 구독마다 `repo.get`·행마다 소비하는 의사코드와 맞지 않았습니다. 구독 조회 1번(`getAll`)과 소비 1번(`removeIfMatches(due)`, 한 트랜잭션)으로 바꿨습니다. 수신 직후 `onAlarmFired`가 같은 알람을 15분 뒤로 먼저 걸어 둡니다. 그래서 처리 끝의 sync는 `NonCancellable` 없이 시간 제한 안에서 돌고, 끊겨도 재시도 알람이 남은 기록을 이어 처리합니다. 수신기의 `finally`에는 `pending.finish()`만 둡니다. 복구 실패(NotReady)일 때는 알람을 취소해 15분 재시도가 반복되지 않게 했습니다. 가져오기의 `NonCancellable` 마무리는 리뷰 요청대로 유지합니다. | S ADR-3, §8 / P Task 7(Fake `callDelay`), Task 8(처리 의사코드, `slowThousandDueUsesOneReadAndOneConsume`, `cancelledDeliveryLeavesRetryAlarm`), Task 9(`removeAllIfMatches`), Task 13(수신기, `receiverFinishesWhenDeliverHangs`) |
| R4-extra | (리뷰 밖, Claude 발견) R4-1을 고치면 알람이 사라지는 경로 | 수용 | 3차의 `markAlarmConsumed()`는 잠금 **안**에 있었습니다. 수신기가 가져오기 잠금을 기다리다 취소되면 "알람이 울렸다"는 기록이 남지 않습니다. 그러면 가져오기 끝의 sync가 applied == 예정 시각으로 보고 다시 걸지 않아, OS 알람이 하나도 없게 됩니다. 지금은 R4-1의 now 치환이 우연히 이 경우를 덮고 있었습니다. 잠금 밖의 `onAlarmFired()`(재시도 알람 + applied = Retry, `synchronized(alarmLock)`로 sync와 직렬화)로 대체했습니다. | P Task 8(`onAlarmFired`, `firedAlarmArmsRetryBeforeWaitingForLock`), Task 13 |

---

# 5차 리뷰 대응 (2026-10-25)

- 리뷰 대상: 4차 반영 후의 S(345줄), P(1526줄), 이 대응표
- 리뷰 원문: `docs/reviews/2026-10-25-adversarial-review-round5.md`
- 교차검증: 인용된 P 줄(577–581, 615–625, 664–690, 694, 991–994)과 대조했고 모두 일치했습니다. 두 실행 순서 모두 문서의 의사코드에서 그대로 성립합니다.
- 결과: 2건 모두 수용. 모두 **계획에 반영됨**이고, 테스트 작성·통과와 실기기 확인은 아직 하지 않았습니다.

| # | 지적 | 판정 | 근거·결정 | 고친 위치 |
|---|---|---|---|---|
| R5-1 | 잠금 밖의 NotReady 후속 취소가 복구 뒤의 새 알람을 지울 수 있음 | 수용 | 4차의 `alarm.cancel(); applied = None`은 `writeWhenReady`가 잠금을 놓은 뒤에 실행되어, 그사이 끝난 복구의 알람을 지울 수 있습니다. 취소가 실패해도 None으로 기록하는 문제도 맞습니다. `writeWhenReady(onNotReady, block)`가 **잠금을 쥔 채** 상태를 검사하고, 실패 상태면 그 자리에서 `sync.cancelRetry()`를 부릅니다. 취소가 실패하면 applied = Unknown입니다. 잠금 순서는 coordinator Mutex → alarmLock 하나로 고정합니다. | S ADR-7, §8 / P Task 8(`writeWhenReady`, `cancelRetry`, 잠금 순서, 테스트 3개), Task 7(`failNextCancel`) |
| R5-2 | 만료 알림 정리와 새 묶음 게시가 직렬화되지 않음 | 수용 | refresher의 `pruneExpired`가 coordinator 잠금 밖에서 돌아, 옛 스냅샷으로 다시 게시하면 이미 소비된 새 항목이 사라집니다. 정리를 refresher의 `coordinator.write` 안으로 옮기고, `show`·`pruneExpired`는 그 잠금 안에서만 부른다는 규칙을 인터페이스에 적었습니다. `shown`·`show`를 따로 잠그지 않습니다. | S ADR-7, §8 / P Task 7(`ReminderPoster` 계약, `pruneGate`), Task 8(refresher 실행, 테스트 2개) |
| R5-extra | (리뷰 밖, Claude 발견) 묶음이 Single로 나뉠 때 이미 보였던 항목이 다시 알림음을 냄 | 수용 | 새 tag로 게시되므로 알림음이 다시 납니다. 이미 보였던 항목은 `setSilent(true)`로 게시하고 새 항목만 알림음을 냅니다. | P Task 7(`show` 계약), Task 8·13(테스트 각 1개) |

## 확인했지만 고치지 않은 경합

| 경합 | 판단 |
|---|---|
| 잠금 밖의 `onAlarmFired`가 복구 sync가 건 정상 알람을 15분 재시도 알람으로 덮음 | 알람이 사라지지 않습니다. 15분 뒤 처리가 끝의 sync에서 실제 시각으로 되돌리므로 최대 15분 지연입니다. 그대로 둡니다. |
| 앱이 화면 알림을 읽은 직후 사용자가 알림을 지움 → 다음 게시가 다시 띄움 | 사용자 동작은 앱 잠금으로 막을 수 없습니다. best effort 범위로 S ADR-14에 적었습니다. |
