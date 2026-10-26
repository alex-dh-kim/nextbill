# NextBill v1 직접 확인 점검표

설계 문서 §9.2와 계획서 Task 25 Step 3·4의 항목입니다. 날짜는 확인한 날, "자동"은 단위 테스트로 대신한 항목입니다.

- 기기: Android 에뮬레이터 `e2e_pixel9_api36` (API 36, Pixel 런처) 한 대
- 최소 API 확인(2026-10-26, 최종 리뷰 C1 수정 뒤): 에뮬레이터 `nb_api26` (API 26, `system-images;android-26;google_apis;arm64-v8a`, pixel_5). 알림 경로만 확인했습니다(아래 "API 26 알림" 행).
- **확인하지 못한 환경:** API 33 에뮬레이터는 확인하지 않았습니다. API 35·36 동작은 Robolectric(SDK 34)이 다루지 못하므로 `e2e_pixel9_api36` 확인이 유일한 근거입니다.
- 최종 리뷰에서 API 26–33에서는 알림이 하나도 예약되지 않는 버그(C1: `LocalDate.ofInstant`는 API 34부터)가 나왔습니다. API 36 확인만으로는 드러나지 않았습니다. 이제 `lintDebug`가 JVM 모듈의 API 34+ 호출을 `checkAndroidApi`로 막습니다.

## 권한·백업 (Step 3)

| 항목 | 결과 | 근거 |
|---|---|---|
| `INTERNET`·`SCHEDULE_EXACT_ALARM`·`USE_EXACT_ALARM` 없음 | 통과 (2026-10-26) | `aapt2 dump permissions app-release-unsigned.apk` 결과: `RECEIVE_BOOT_COMPLETED`, `POST_NOTIFICATIONS`, `WAKE_LOCK`, `ACCESS_NETWORK_STATE`, `FOREGROUND_SERVICE`(뒤 셋은 Glance → WorkManager 전이, ADR-0006) |
| 클라우드 백업 대상 파일 0개 | 부분 확인 | 규칙 파일은 클라우드 백업의 모든 도메인을 제외합니다(`data_extraction_rules.xml`). `bmgr backupnow`는 GMS 전송이 계정 없이 실패해 로컬 전송으로 바꿔 실행했고, 결과는 "Transport rejected package"였으며 전송 저장소(`/data/cache/backup`)에 이 앱 파일이 없었습니다. 업로드 0건을 전송 쪽 로그로 직접 보지는 못했습니다 |

## 직접 확인 (Step 4)

| 항목 | 결과 | 근거 |
|---|---|---|
| 강제 종료(`am force-stop`) 후 실행 시 알람 재등록, 이 앱 알람 2개 이하 | 통과 (2026-10-26) | `dumpsys alarm`: `REMINDER`(RTC_WAKEUP, 창 1시간) 1개 + `MIDNIGHT`(RTC_WAKEUP, 창 15분) 1개 |
| 알림 시각 경과 후 알림 도착 | 통과 (2026-10-26) | 기기 시간을 알림 시각 5분 뒤로 옮김 → "iCloud+ 내일 결제 · $2.99 · 10월 30일" 게시, 다음 알람이 다음 occurrence로 다시 걸림 (`docs/screenshots/notification.png`) |
| API 26 알림: 구독 저장 → 알람 예약 → 시각 경과 후 알림 도착 | 통과 (2026-10-26, C1 수정 뒤) | `nb_api26`에서 UI로 "OldApiTest"(₩9,900, 매월 26일, 하루 전 9:00) 저장 → `dumpsys alarm`에 `*walarm*:dev.lioracode.nextbill.REMINDER`(RTC_WAKEUP, when=2026-11-25 09:00:00, 창 1시간). `adb root` + `adb shell date 112509052026.00` 뒤 25초 안에 `dumpsys notification`에 `tag=subscription-1`, channel=payment_reminders 게시("OldApiTest payment tomorrow · ₩9,900 · Nov 26", `docs/screenshots/notification-api26.png`), 다음 `REMINDER`는 2026-12-25 09:00으로 다시 걸림. logcat에 `NoSuchMethodError` 0건 |
| 재부팅(`adb reboot`) 후 알람 재등록 | 통과 (2026-10-26) | 부팅 후 `dumpsys alarm`에 `REMINDER`·`MIDNIGHT` 2개 |
| Doze 강제 중 알림 시각 경과 → 해제 시 도착 | 통과 (2026-10-26) | `deviceidle force-idle` 중 0건, `unforce` 후 40초 안에 1건 |
| TalkBack용 접근성 트리 | 버그 발견 후 수정 (2026-10-26) | 홈 "구독 추가" 버튼이 `uiautomator dump`에서 이름 없는 버튼(`NAF="true"`)이었습니다. `contentDescription`을 붙이고 `HomeScreenAccessibilityTest`를 추가했습니다. 수정 후 홈·캘린더·설정·편집 화면 모두 `NAF` 0개. TalkBack을 켜고 등록 → 저장을 끝까지 듣는 확인은 하지 않았습니다 |
| 저장 직후 위젯 변경, 기기 날짜 변경 시 D-n 변경, 가로 2칸까지 줄였다 늘리기 | 통과 (2026-10-26, Task 24) | `docs/design/widget-actual.png`. 세로 크기 조절은 런처가 드래그를 받지 않아 단위 테스트(250×110 높이 계산)로만 확인 |
| 백업 왕복(내보내기 → `pm clear` → 가져오기) | 통과 (Task 23) | 구독 2건과 합계 ₩77,000 복원 |
| 권한 최초 요청 → 거부 → 다시 요청, 설정에서 허용 후 복귀 | 통과 (Task 20) | API 36에서만 |
| 입력 중 프로세스 종료(`am kill`) 후 복원 | 통과 (Task 17·20) | 탭 스택과 편집 입력 유지 |
| 화면 회전·탭 전환 | 통과 (Task 17) | |
| 360dp 창에서 캘린더 날짜 칸 48dp | 자동 | `CalendarScreenTest` (레이아웃 크기 단언, 320dp 음성 대조) |
| 같은 시각 알림 5건 → 묶음 1개 / 하루 1건씩 4건 → 4번째에 묶음 | 자동 | `DeliverDueRemindersUseCaseTest`, `ReminderPostTest`, `ReminderNotifierTest` |
| 가져오기 후 편집 화면 → 프로세스 재생성 → 저장 | 자동 | `editorOpenedAfterImportSavesAfterRecreation`, `preImportEditorRestoredAfterDeathIsStale` |
| 키보드와 입력란, edge-to-edge 상단 여백 | 통과 (Task 18·20) | 상단 여백 이중 적용을 Task 18에서 발견·수정 |
| predictive back 제스처 취소·완료 | 확인 안 함 | |

## 전체 검증 (Step 6)

`./gradlew -p build-logic test` → 16개 통과(ModuleRulesTest 8 + AndroidApiCheckTest 8). `./gradlew clean spotlessCheck checkModuleRules assembleDebug lintDebug testDebugUnitTest :core:model:test :core:domain:test :core:testing:test verifyRoborazziDebug` → 결과는 README "테스트" 절에 적습니다.
