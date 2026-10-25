# NextBill 문서 추가 리뷰 — 3차

검토일: 2026-10-24. 현재 작업 트리의 S=`docs/superpowers/specs/2026-10-23-nextbill-design.md`(345줄), P=`docs/superpowers/plans/2026-10-24-nextbill-v1.md`(1398줄), `docs/reviews/2026-10-24-review-response.md`의 2차 대응을 확인했다. 미커밋 변경도 포함했다. 줄 번호는 이 시점 기준이며, 구현 코드의 실행 결과가 아닌 문서 리뷰다.

## 판정

**이전 지적은 대부분 반영됐다. 추가 P1 3건, P2 1건을 보완한 뒤 구현할 것을 권고한다.**

REAPPLY, 동일 잠금 안의 수신 처리, removeIfMatches, 미래 시작 구독 정책, 데이터 한도, stability allowlist, 읽기 전용 상세, 위젯 최소 크기, 저장 실패 상태는 적절한 개선이다. 이미 명시한 절충(WorkManager 미사용, 드문 저장 직후 종료의 중복 저장 위험)을 이번 리뷰에서 다시 필수 변경으로 요구하지 않는다.

## P1

### R3-1. 구독 1,000건을 각각 OS 알람으로 등록하면 지원 Android의 동시 알람 한도에 걸린다

**근거 위치:** P:241의 MAX_SUBSCRIPTIONS=1,000, P:400–416의 구독별 Schedule, P:582–583, P:918–919의 별도 자정 알람.

지원 범위에 포함되는 Android 12 AOSP 구현은 UID당 동시 알람 기본 상한을 **500개**로 두고, 상한에 도달한 추가 등록에 `IllegalStateException`을 던진다. 이는 모든 기기의 정확한 숫자가 항상 500이라는 주장이 아니라, 현재 지원 범위에 실제 반례가 있다는 뜻이다. [AOSP AlarmManagerService 원문](https://android.googlesource.com/platform/frameworks/base/+/refs/heads/android12-release/apex/jobscheduler/service/java/com/android/server/alarm/AlarmManagerService.java)의 `DEFAULT_MAX_ALARMS_PER_UID`와 `Maximum limit of concurrent alarms` 분기에서 확인했다.

**반례:** 미래 결제가 있고 알림이 켜진 구독 1,000건을 가져오면 현재 계획은 서로 다른 PendingIntent를 1,000개 만든다. 자정 알람도 같은 앱의 예산을 쓴다. sync 전체 예외를 삼키면 UI 저장은 성공해도 뒤쪽 구독 예약이 빠진다. REAPPLY를 반복해도 구조적인 상한 문제는 해결되지 않는다.

**수정 방향:**

- 구독별 occurrence는 DB에서 유지하되 OS에는 가장 이른 실행 시점용 알람 하나 또는 작은 제한된 묶음만 등록하는 방식을 검토한다.
- 수신 시 due occurrence들을 처리하고 다음 실행 시점을 등록한다. 많은 항목은 receiver 제한 시간을 고려해 batch 크기·후속 처리도 정한다.
- 구독 수나 알림 가능 개수를 줄이는 대안도 가능하나, 이는 기존 한도 결정의 변경 제안으로 명시한다. 1,000건을 등록 가능하다고 하면서 일부 알림만 조용히 누락시키지 않는다.

**수용 테스트:** 알람 한도를 500으로 둔 fake에 알림 구독 1,000건+위젯을 적용해도 OS 등록 한도를 넘지 않고 모든 due occurrence가 처리 대상으로 남는다. 한 번의 등록 실패가 다른 구독의 취소·다음 처리까지 영구히 막지 않는다.

### R3-2. 공유 Mutex는 배타 실행만 보장하며 RecoveryFailed 이후 작업 금지는 보장하지 않는다

**근거 위치:** P:581, 609–610, 617–621, 628–640, 674, 856–872, 1046–1048.

복구 화면은 새 UI 조작을 막지만 이미 생성되거나 잠금을 기다리는 작업은 남는다. 쓰기 UseCase와 ReminderHandler는 `exclusive` 진입 후 startup/generation을 재확인하지 않는다.

**반례:** import가 잠금을 보유한 동안 save가 대기한다 → DB 교체는 성공하지만 설정 교체가 두 번 실패한다 → startup은 RecoveryFailed가 되고 잠금이 풀린다 → 대기하던 save가 DB를 수정한다 → 사용자가 복구 재시도를 누르면 staging 재적용으로 그 save가 사라진다. 같은 문제가 시작 복구 실패 뒤 대기하던 수신기·refresher에도 적용된다. 현재 `writesWaitWhileImportApplies` 테스트는 import 성공 경로만 본다.

Application이 start를 비동기로 시작하므로 receiver가 startup recovery보다 먼저 잠금을 잡는 순서도 막아야 한다. 동일 Mutex를 공유한다는 것만으로 recovery가 항상 먼저 실행되지는 않는다.

**수정 방향:** 외부 작업의 admission(실행 허가)을 잠금 안에서 Ready·dataGeneration과 함께 검사한다. Recovering/RecoveryFailed이면 쓰기를 거부하거나 복구 성공 뒤 재검증하며, 이전 generation의 편집/수신 작업은 무효화한다. AppStartup 내부 복구 경로는 별도로 둬 같은 비재진입 Mutex를 다시 획득하거나 Ready를 기다리는 교착을 만들지 않는다. 설정 쓰기도 포함 범위를 명시한다.

**수용 테스트:**

1. import 2회 실패 중 대기한 save는 성공으로 반환하지 않으며 DB를 바꾸지 않는다.
2. RecoveryFailed 뒤 receiver/refresher가 부분 교체 상태로 알림을 게시하지 않는다.
3. cold start에서 receiver를 recovery보다 먼저 실행시켜도 이전 데이터 처리가 허용되지 않는다.
4. 성공한 import 뒤 대기 작업은 새 generation에서 유효한지 재검증한다.

### R3-3. staging 생성·삭제 실패가 복구 상태 전이에 포함되지 않아 성공한 변경이 나중에 덮일 수 있다

**근거 위치:** P:627–638, 762, 1325–1335.

의사코드에서 RecoveryFailed로 전환하는 경로는 `apply` 두 번 실패뿐이다. `staging.write`, `staging.clear`의 실패·취소는 정의되지 않았다. 특히 clear 실패를 무시하면 안 된다. `File.delete()`/`renameTo()`의 false 반환 처리도 계약에 없다.

**반례:** staging write 성공 → DB·설정 교체 성공 → staging clear 실패 또는 clear 직전 coroutine 취소 → startup은 여전히 Ready, generation 갱신도 못 하고 함수 종료 → 사용자가 계속 편집 → 다음 프로세스 시작에서 남아 있는 staging을 재적용 → 사후 편집 손실. apply 성공으로 끝났어도 journal 최종화에 실패하면 R2-3 문제가 다시 생긴다.

**수정 방향:** staging 생성부터 최종화까지 하나의 import 상태 머신으로 다룬다. clear가 확인되기 전에는 성공/정상 편집 허용 상태로 돌아가지 않는다. write 전 실패는 이전 상태로 안전하게 종료하고, journal이 남았거나 적용 여부가 불명확한 실패·취소는 복구 상태로 전환한다. `clear()`와 `write()`는 filesystem 성공 여부를 확인하고 실패를 호출자에게 알린다. 반드시 지킬 cleanup/state transition과 CancellationException 전파 순서도 정한다.

**수용 테스트:** `write` 실패, rename 실패, apply 성공 후 clear 실패, clear 전 취소, clear 후 generation 갱신 전 취소. 각 경우 UI/잠금/파일/데이터 상태를 확인하고, 이후 편집이 다음 재시작에서 조용히 사라지지 않아야 한다. 성공 메시지는 journal 최종화가 확인된 뒤에만 나온다.

## P2

### R3-4. “최소 1회 보장”과 “최대 한 번 더”는 현재 선택한 best effort 정책과 일치하지 않는다

**근거 위치:** S:231–242(ADR-3·4·13·14), P:31–32, 611–612, 863–872, 891; 대응표 R2-2·R2-6.

현재 계획은 다음을 의도적으로 허용한다.

- OS 등록 실패 후 다음 프로세스 시작 전까지 재시도가 없을 수 있음.
- 권한 차단 또는 결제일 경과 시 알림 없이 occurrence를 소비함.
- 수신기 timeout 후 다음 프로세스가 시작되지 않으면 복구가 실행되지 않음.

따라서 모든 occurrence에 대한 **at-least-once delivery(최소 한 번 전달)**는 성립하지 않는다. 이를 달성하려고 추가 인프라를 넣으라는 요구는 아니다. 허용한 절충에 맞춰 보장 문구를 낮추면 된다.

또한 notify→consume 사이에 프로세스가 반복 종료되면 같은 occurrence가 두 번 이상 재게시될 수 있다. 고정 tag·id는 활성 알림 카드 중복을 막지만 게시 횟수를 2회 이하로 제한하지 않는다. `crashAfterNotifyRedeliversAtMostOnce`라는 테스트 이름도 과도한 보장이다. 단 한 번의 crash를 주입해서 두 번 게시됐다는 것은 전체 생애의 상한 증명이 아니다.

**수정 방향:** “best effort 전달, 미소비 occurrence는 복구 시 재시도될 수 있음, 중복 게시 가능, 동일 구독의 활성 알림은 대체”로 계약을 기술한다. 필요한 경우 “수신 성공·권한 허용·유효 결제일·복구 재실행 조건하의 재시도”를 별도로 쓴다. 과거 trigger를 REAPPLY하는 경우도 setWindow이므로 “곧바로 울림” 대신 “다시 전달 대상이 되며 OS 제약으로 지연될 수 있음”으로 고친다.

**수용 테스트:** notify 뒤 consume 전 종료를 연속 3회 주입해도 데이터 전이가 깨지지 않고 활성 tag는 하나임을 확인한다. 미전달을 허용하는 조건과 재시도 기회를 README/ADR/테스트 이름에 같은 의미로 쓴다.

## 이번에 다시 요구하지 않는 사항

- WorkManager 도입, 모듈 수 축소, strict exactly-once 처리는 필수 요구가 아니다.
- 부분 적용 후 “가져오기 취소”로 혼합 상태를 수용하는 UX는 문서가 명시한 절충으로 취급한다. 다만 이때 “항상 전부 이전/전부 새것”이라는 S ADR-7 문구는 취소 예외를 붙여 맞춰야 한다.
- 저장 commit 직후 UI 성공 전 종료의 중복 위험은 이미 허용된 절충으로 기록돼 있다. 대응표가 이 내용이 S ADR-13·README 계획에도 들어갔다고 썼지만 실제 S ADR-13에는 없다. 대응표와 본문의 위치/설명을 맞추고 발생 가능성이 “매우 낮음”이라는 미측정 확률은 단정하지 않는다.

## Claude 전달 요청

R3-1~R3-4를 검토해 설계·구현 계획·대응표를 함께 갱신해 주세요. 특히 R3-1은 지원 Android 플랫폼의 실제 한도이므로 단순 fake 테스트나 일반 예외 격리로 닫지 말고 OS 등록 개수 설계를 맞춰 주세요. R3-2·R3-3은 **실패한 import 뒤에도 살아 있는 기존 작업**과 **journal 최종화 실패**를 직접 실패 주입하는 테스트로 계약을 보여 주세요. R3-4는 새 기능을 추가하기보다 현재 선택한 best effort 정책에 맞게 보장 문구를 정정하면 됩니다.

원본 문서를 이번 리뷰에서 직접 수정하지 않았으며, 구현/테스트를 실행했다는 판정도 하지 않습니다.
