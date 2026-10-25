# ADR-0008: 클라우드 자동 백업은 끄고 기기 간 직접 전송만 허용합니다

- 상태: 승인 (2026-10-24)
- 출처: 설계 문서 §2.2, §6 ADR-8 [Alex 결정, 2026-10-24]

## 맥락
Android 자동 백업은 앱 데이터를 Google Drive로 올립니다. 이는 "금융 데이터는 기기 밖으로 나가지 않는다"(ADR-0001)와 충돌합니다. 반면 기기를 바꿀 때 케이블·근거리 전송으로 데이터를 옮기는 것은 기기 밖 서버를 거치지 않습니다.

## 결정
- **API 31+:** `android:dataExtractionRules="@xml/data_extraction_rules"`. `<cloud-backup>`에서 `root`·`file`·`database`·`sharedpref`·`external`을 전부 제외하고, `<device-transfer>`에는 `database/nextbill.db`·`database/nextbill.db-wal`·`database/nextbill.db-shm`과 `file/datastore/settings.preferences_pb`만 포함합니다. Room의 기본 WAL 모드에서는 커밋된 쓰기가 checkpoint 전까지 `-wal` 파일에 있으므로 DB 파일과 함께 옮깁니다.
- **API 28~30:** `android:fullBackupContent="@xml/backup_rules"`. 같은 네 파일을 `requireFlags="deviceToDeviceTransfer"`로 포함합니다.
- **API 26~27:** 이 구분이 없어서 백업 자체를 끕니다. `android:allowBackup="@bool/allow_backup"`이고 `values/`는 false, `values-v28/`은 true입니다.
- 가져오기 임시 파일(`files/import-staging.json`)은 어디에도 포함하지 않습니다.

## 대안
- **자동 백업 켜기(기본값):** 데이터가 Google Drive로 올라갑니다.
- **모든 백업 끄기:** 원칙은 지키지만 기기를 바꿀 때 직접 전송도 막힙니다. 이 경우 사용자는 백업 파일(ADR-0007)로만 옮길 수 있습니다.

## 결과
- 빌드한 APK의 매니페스트에 `dataExtractionRules`·`fullBackupContent`가 있어야 합니다(`aapt2 dump xmltree`로 확인).
- 기기 간 전송으로 옮긴 DB는 첫 실행의 갱신이 OS 알람을 다시 겁니다(ADR-0004).
- 사용자가 내보낸 백업 파일을 클라우드 문서 앱에 저장하는 것은 사용자의 선택이며, README에 이 차이를 씁니다.
