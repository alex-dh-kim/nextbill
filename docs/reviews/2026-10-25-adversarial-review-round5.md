# NextBill 문서 리뷰 — 5차

2026-10-25. 대상: 설계 문서 S(345줄), 구현 계획 P(1526줄), 대응표의 4차 반영. 현재 작업 트리 기준이며 앱 코드는 아직 검증하지 않았다. P 경로는 `docs/superpowers/plans/2026-10-24-nextbill-v1.md`다.

## 판정

**4차 지적 5건은 문서상 반영됐다. 추가로 P1 1건, P2 1건이 남아 있다.** 두 항목 모두 기존 동시성 계약의 작은 수정으로 해결할 수 있다. 새 프레임워크나 모듈을 더 도입할 필요는 없으며, 아래 기대 동작과 경합 테스트를 계획에 반영한 뒤 구현으로 넘어가는 것이 적절하다.

확인한 개선: 논리 예정 시각으로 알람 비교, 전체 활성 알림 기준 묶음, import/startup의 파생 작업 실패 격리, 영속 dataset revision, 수신기 timeout 밖 NonCancellable 제거. 이전 요구를 이번에 다시 나열하지 않는다.

## P1 — R5-1. 잠금 밖의 NotReady 후속 취소가 복구 완료 후 새 알람을 지울 수 있다

**위치:** P:619–625, 666–672, 678–690, 698–712.

DeliverDueRemindersUseCase가 `writeWhenReady`에서 NotReady를 받은 뒤, coordinator 잠금 밖에서 `alarm.cancel(), applied=None`을 실행한다. 상태를 확인한 순간과 실제 취소 순간 사이에 복구가 완료될 수 있다.

성립하는 실행 순서:

1. state=RecoveryFailed에서 수신기가 NotReady를 반환받는다. 아직 후속 cancel은 실행되지 않았다.
2. 사용자가 재시도하고 recovery가 coordinator 잠금을 획득한다.
3. 복구 성공 → Ready → sync가 정상 다음 알람을 등록한다.
4. 이전 수신기의 NotReady 후속 분기가 늦게 실행되어 새 알람을 취소하고 applied=None으로 만든다.

최종 상태는 **Ready·예약 DB 있음·실제 OS 알람 없음**이다. 후속 refresh가 반드시 그 뒤에 실행된다는 순서 보장이 없으므로 자동 복구를 전제할 수 없다. alarmLock으로 cancel만 감싸도 실행 순서 자체는 같아 여전히 성립한다.

**수정 방향:** 복구 실패 상태 확인과 retry 알람 취소를 같은 coordinator 임계 구역에서 수행한다. 또는 NotReady 후속 처리에서 coordinator를 다시 얻고 현재 상태/세대를 재검증해, 여전히 실패 상태일 때만 alarmLock 안에서 취소한다. lock 순서는 기존 sync와 동일하게 coordinator → alarmLock으로 고정한다. 정상 복구 뒤의 알람을 옛 결과로 취소하지 않는다.

**수용 테스트:** NotReady 반환 직후 barrier로 수신기를 멈춘다 → retryRecovery를 완료해 다음 알람을 등록한다 → 수신기를 재개한다 → state=Ready이고 정상 알람이 유지된다. 반대로 RecoveryFailed가 계속되면 retry 알람을 취소해 반복 실행을 막는다. cancel 실패 시 applied를 성공한 것처럼 None으로 바꾸지 않는지도 확인한다.

## P2 — R5-2. 만료 알림 정리와 새 묶음 게시가 직렬화되지 않아 새 항목이 사라질 수 있다

**위치:** P:577–581, 681–685, 694, 991–994.

deliver의 `shown → planShown → show`는 coordinator 안에서 실행한다. 반면 refresher의 `poster.pruneExpired`는 coordinator.write가 끝난 다음 실행한다. ReminderPoster에는 이 두 read-modify-write 흐름을 공유 잠금으로 보호한다는 계약이 없다.

반례:

1. prune이 Summary [만료 A, 유효 B]를 읽고 [B]로 갱신할 준비를 한다.
2. deliver가 새 C를 받아 Summary [B,C]를 게시하고 C의 DB 예약을 소비한다.
3. 오래 걸린 prune이 기존 스냅샷 [B]를 다시 게시해 C를 지운다.

이후 C는 소비됐으므로 정상 재시도 대상도 아니다. 이 문제는 OS의 알림 50개 한도와 별개이며, 항상 활성 3개 이하라는 검사만으로는 잡히지 않는다.

**수정 방향:** poster의 표시 상태를 바꾸는 흐름을 하나의 직렬 실행 경로로 모은다. 예를 들어 prune을 짧은 coordinator.write 안으로 옮기거나, poster가 조회·병합·게시/정리를 원자적인 API로 제공하게 한다. 개별 `shown()`과 `show()`에 각각 잠금을 거는 것만으로는 그 사이의 경합을 막지 못한다. 라이브러리 추가는 필요 없다.

**수용 테스트:** prune이 옛 Summary를 읽은 뒤 barrier로 멈춤 → deliver가 C 게시/소비 → prune 재개. C가 사라지지 않고 만료 A만 제거되어야 한다. 동일 시나리오를 Summary에서 Single로 바뀌는 경계에서도 확인한다.

## 검토 범위와 Claude 전달 요청

위 두 순서를 작은 상태 모델로 실행해 Ready인데 알람이 없는 결과와, C를 소비했는데 표시에서 사라지는 결과를 확인했다. 이는 문서 의사코드의 interleaving 검증이며 Android 앱 실행 테스트를 대신하지 않는다.

Claude는 R5-1/R5-2의 임계 구역·lock 순서·추가 테스트를 구현 계획과 대응표에 반영해 주세요. 이후에는 문서 전체에 추상화를 더 늘리기보다 해당 실패 주입 테스트를 먼저 작성하고 구현으로 검증하는 것을 권고합니다. “문서에 반영”과 “테스트 통과/실기기 검증”은 계속 구분해 주세요.

원본 S/P 문서는 이번 리뷰에서 변경하지 않았다.
