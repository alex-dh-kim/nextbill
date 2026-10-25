# NextBill 문서 리뷰 — 4차

검토일: 2026-10-25. S=`docs/superpowers/specs/2026-10-23-nextbill-design.md`(345줄), P=`docs/superpowers/plans/2026-10-24-nextbill-v1.md`(1478줄), 대응표의 3차 반영을 검토했다. 미커밋 변경 포함, 줄 번호는 이 시점 기준이다. 아직 앱 코드 실행/검증 결과가 아닌 문서의 반례 검토다.

## 판정

단일 OS 알람, 쓰기 admission, journal 최종화, best effort 문구는 이전보다 개선됐다. 다만 **P1 4건, P2 1건**이 남아 있다. 아래 항목은 새 의사코드에서 직접 도출되는 동작 문제다. 기능을 더 늘리기보다 이 계약을 수정하고 해당 경계 테스트를 구현할 것을 권고한다.

## P1

### R4-1. 기한이 지난 earliest 알람은 sync할 때마다 `now`로 다시 등록돼 지연이 누적될 수 있다

**위치:** P:403, 643–653, 722–725.

`nextAlarmAt`은 과거 trigger를 현재 now로 바꾼다. 그 값을 메모리 applied와 비교하므로, 미처리 occurrence가 하나라도 있으면 시간이 흐른 뒤의 모든 sync가 서로 다른 desired를 만든다.

예: 원래 trigger=09:00, 아직 OS가 전달하지 않은 상태 → 09:20 sync는 09:20으로 재등록 → 09:40 sync는 09:40으로 재등록 → 10:00에도 재등록. 전혀 다른 구독을 저장하거나 다른 refresh가 실행될 때도 기존 setWindow 창을 다시 시작할 수 있다. “지연 중인 다른 예약을 보존”한다는 초기 요구가 DB 행에만 적용되고 실제 OS 알람에는 적용되지 않는다.

이 계산은 문서 규칙을 작은 스크립트로 옮겨 세 시점 모두 setAgain=true임을 확인했다. 이는 실제 Android 실행 테스트는 아니다. [공식 알람 가이드](https://developer.android.com/develop/background-work/services/alarms)는 setWindow의 시작 시점과 지연 제약을 설명한다.

**수정:** 비교할 논리적 earliest trigger/예약 버전과 OS에 전달하는 실제 시각을 분리한다. 이미 등록돼 있고 아직 소비되지 않은 같은 overdue 알람은 재등록하지 않는다. 새 프로세스 Unknown·등록 실패·더 이른 새 occurrence·수신 후 재arm은 별도로 처리한다.

**수용 테스트:** trigger를 넘긴 뒤 clock을 전진시키며 무관한 저장과 refresh를 반복해도 setNext 호출이 늘지 않는다. 새 프로세스와 등록 실패 후 재시도, 더 이른 알림 추가는 정상 반영된다. `unchangedEarliestDoesNotTouchAlarm`은 미래 시각뿐 아니라 **과거 시각**에서도 검사한다.

### R4-2. 한 배치 4건 이상만 묶어서는 누적 활성 알림 50개 한도를 해결하지 못한다

**위치:** S ADR-14; P:409–410, 662–665, 674, 949–951.

현재 묶음 기준은 이번 invoke의 deliverable 개수뿐이다. 51개 구독이 서로 다른 시각에 한 건씩 처리되면 매번 Single 하나가 생긴다. 결제일이 모두 오늘이거나 미래라면 pruneExpired는 아무것도 지우지 않는다. 결과적으로 기존과 같은 활성 알림 한도 문제다.

Android 12 AOSP 원문을 다시 확인했다: `MAX_PACKAGE_NOTIFICATIONS = 50`(347행), 한도 초과 시 게시 거부(6594–6597행). [NotificationManagerService 원문](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android12-release/services/core/java/com/android/server/notification/NotificationManagerService.java). 웹 도구에서 원문 열기가 실패해 같은 공식 소스의 `?format=TEXT`를 직접 받아 해당 줄을 확인했다.

**수정:** 사용자 결정인 “한 배치 3건 이하 개별/4건 이상 묶음”을 유지하면서 **앱 전체 활성 알림**의 상한 관리도 정의한다. 기존 알림을 합치거나 안전한 개수만 남기는 등의 정책이 필요하다. 누락 없이 계속 보존하려면 표시용 알림과 앱 내 확인 가능한 내역의 범위를 구분한다. 한도 초과로 조용히 버리는 것을 정상 성공으로 처리하지 않는다.

**수용 테스트:** 같은 날 1건씩 51번 처리(서로 다른 구독), Single과 Summary 혼합, 사용자가 일부 알림을 직접 지운 경우. 전체 활성 알림 수가 정책 상한 안에 있고 새 알림이 조용히 거부되지 않는다. fake도 실제 상한을 모델링해야 한다.

### R4-3. import/startup에서 sync 예외가 데이터 처리 결과로 전파된다

**위치:** S ADR-13; P:687, 695–712, 1397–1417.

일반 저장은 파생 갱신 실패와 분리했지만 가져오기는 staging 삭제와 generation 갱신 뒤 `sync()`를 직접 호출한다. 알람 등록이 SecurityException/IllegalStateException 등으로 실패하면 **DB·설정 교체와 journal 정리는 끝났는데 import가 Ok를 반환하지 못한다.** 정의된 ImportError로도 매핑되지 않는다. 사용자가 다시 가져오기를 하면 불필요한 전체 교체가 반복된다.

startup도 state=Ready 뒤 sync가 실패하면 AppStartup.start 코루틴이 예외로 끝난다. SupervisorJob은 자식 실패를 형제에게 전파하지 않는 장치이지, 예외 자체를 처리하는 handler가 아니다. 구체적인 앱 종료 여부와 별개로 현재 계획은 이 예외 경로를 정의하지 않았다.

**수정:** 데이터 복구/교체의 성공 확정 지점과 알람 적용 결과를 분리한다. journal 최종화 후 sync 실패는 데이터 실패로 바꾸지 않으며, OS 알람의 applied 상태는 미적용으로 남겨 다음 시도에서 재등록한다. startup도 복구 성공을 유지하고 파생 작업의 실패를 명시적으로 격리한다. 취소 예외 정책은 별도로 유지한다.

**수용 테스트:** import에서 apply·clear 성공 후 alarm.setNext 실패 → 데이터 가져오기 성공, staging 없음, generation 증가, 다음 sync 재시도 가능. startup도 같은 알람 실패에서 RecoveryFailed로 오인하거나 처리되지 않은 예외를 남기지 않는다.

### R4-4. 메모리 generation과 SavedStateHandle generation의 수명이 다르다

**위치:** P:599–607, 632, 638–639, 1280–1281.

DataCoordinator generation은 영속 저장이 명시되지 않은 메모리 StateFlow다. editor는 그 값을 SavedStateHandle에 저장한다.

반례 A: import 성공 후 generation=1 → 새 editor를 열어 draft와 generation=1 저장 → 정상 background 후 프로세스 종료 → 새 프로세스의 generation은 초기값 0, staging 없음 → draft의 expectedGeneration=1 때문에 저장이 Stale/Missing으로 거부되고 화면이 닫힐 수 있다. 데이터가 교체되지 않았는데 입력 복원이 실패한다.

반례 B: import 완료로 staging은 지워졌지만 UI stack reset 전에 종료 → 이전 generation=0의 editor가 복원되고 새 프로세스도 0이면, 교체 전 화면을 최신 데이터에 대한 쓰기로 잘못 허용할 수 있다. 신규 draft(id=0)는 Missing id 검사로도 차단되지 않는다.

**수정:** generation을 데이터 교체와 연동되는 영속 dataset revision으로 만들거나, 복원된 화면의 데이터 정체성을 검증하는 명시적인 대안을 둔다. 단순히 새 프로세스에서 draft generation을 현재 값으로 덮으면 B를 놓친다. 영속 revision 갱신은 import journal의 재적용·최종화와 함께 설계한다.

**수용 테스트:** (1) import 후 새 editor 입력→프로세스 재생성→정상 저장, (2) import 후 stack reset 전 종료→교체 전 editor 차단, (3) staging recovery 후 이전 draft 차단. 같은 프로세스 안 generation 변경 테스트만으로 닫지 않는다.

## P2

### R4-5. 8초 timeout의 finally 안에 무제한 NonCancellable sync가 있다

**위치:** P:662–667, 956.

수신기는 withTimeout(8초)로 감싸지만 finally에서 NonCancellable sync를 완료할 때까지 기다린다. 따라서 finish()까지의 시간이 8초로 제한되지 않는다. “DB 몇 번”이라는 설명도 실제 의사코드는 due 최대 1,000건에 repo.get을 각각 호출하고, 소비도 행마다 한다. 모든 기기에서 8초를 넘긴다는 주장은 아니지만, 현재 제한을 보장하는 근거는 없다.

**수정:** 알림 처리를 위한 배치 조회·소비와 실행 시간 예산을 정하고, 수신기 마무리의 NonCancellable 범위는 짧고 제한된 작업으로 줄인다. 남은 occurrence는 다음 알람으로 이어 처리한다. 예외/timeout을 주입할 때 cleanup까지 무한히 기다리지 않는지 확인한다. [BroadcastReceiver.goAsync 문서](https://developer.android.com/reference/android/content/BroadcastReceiver#goAsync())의 실행 시간 제약은 코루틴 취소를 막는 것으로 해제되지 않는다.

**수용 테스트:** repo가 느린 1,000건 처리, sync 지연, 잠금 대기 후 timeout에서 PendingResult.finish 호출의 시간 경계와 미소비 데이터의 후속 처리를 검증한다. 장시간 import 전체를 임의로 짧게 잘라 journal 안전성을 깨라는 요구는 아니다.

## Claude 전달 요청

R4-1~R4-5를 검토해 두 문서와 대응표를 갱신해 주세요. 단일 OS 알람 선택, 사용자 결정인 알림 묶음 기준, 데이터 한도, 모듈 구성은 유지 가능한 방향으로 제안했습니다. 필수적으로 새 라이브러리를 도입하라는 요구는 아닙니다.

수정 뒤에는 추가 추상화를 계속 늘리기보다 위 반례의 기대 동작과 실패 주입 테스트를 명확히 하고 구현을 진행해 주세요. 문서만으로 실기기 성능이나 Android 동작이 검증됐다고 표현하지 마세요.
