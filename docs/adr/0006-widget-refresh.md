# ADR-0006: 위젯은 데이터 변경 때와 자정에 한 번 갱신합니다

- 상태: 승인 (2026-10-23)
- 출처: 설계 문서 §6 ADR-6 [설계자 판단]

## 맥락
위젯의 D-n과 이번 달 합계는 데이터가 바뀌지 않아도 날짜에 따라 바뀝니다. 위젯 갱신 때문에 라이브러리를 늘리고 싶지 않습니다.

## 결정
- 데이터 변경 시 `updateAll`을 부릅니다.
- 자정 날짜 변경 시 1회 갱신합니다. 자정 갱신은 **AlarmManager 비정확 알람**으로 예약하고(best effort), 위젯이 하나도 없으면 예약하지 않습니다.
- WorkManager는 직접 쓰지 않습니다. Glance가 내부적으로 쓰는 것은 허용합니다.

## 대안
- **WorkManager 주기 작업:** 직접 의존성이 하나 늘어납니다. 알림 예약(ADR-0003)과 같은 구조를 재사용하면 필요 없습니다.
- **분 단위 폴링:** 배터리를 쓰고, 자정 한 번이면 충분합니다.

## 결과
- 앱이 거는 OS 알람은 알림 1개 + 위젯 자정 1개로 항상 2개 이하입니다(ADR-0003).
- 자정 갱신이 늦어질 수 있습니다(비정확 알람).
- **전이 의존성 출처:** `androidx.glance:glance-appwidget`이 `androidx.work:work-runtime-ktx`를 runtime 의존성으로 가져옵니다(glance-appwidget 1.2.0 POM에서 확인). 앱 코드는 WorkManager API를 직접 부르지 않습니다.
- **전이 권한 [2026-10-25, Task 17]:** 합쳐진 앱 manifest에 `WAKE_LOCK`, `ACCESS_NETWORK_STATE`, `FOREGROUND_SERVICE`가 들어옵니다. 출처는 모두 `androidx.work:work-runtime:2.7.1`의 manifest입니다(Glance → WorkManager, `app/build/outputs/logs/manifest-merger-debug-report.txt`에서 확인). WorkManager가 내부 동작에 쓸 수 있어 `tools:node="remove"`로 지우지 않습니다. `INTERNET` 권한은 여전히 없습니다(ADR-0001).
