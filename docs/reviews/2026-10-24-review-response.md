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
