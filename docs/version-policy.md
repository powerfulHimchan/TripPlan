# Android 앱 버전 정책

앱은 실행할 때 백엔드에 현재 `versionCode`를 보냅니다. 강제 적용 시각이 지났고 현재 버전이 최소 버전보다 낮으면, 앱은 뒤로가기로 닫을 수 없는 업데이트 화면을 표시합니다.

Railway 백엔드 서비스에서 다음 환경변수를 사용합니다.

```text
ANDROID_MIN_VERSION_CODE=5
ANDROID_LATEST_VERSION_CODE=5
ANDROID_FORCE_UPDATE_AT=2026-10-01T00:00:00Z
ANDROID_STORE_URL=https://play.google.com/store/apps/details?id=com.powerfulhimchan.tripplan
ANDROID_UPDATE_MESSAGE=안정적인 여담 사용을 위해 앱을 업데이트해주세요.
```

- `ANDROID_MIN_VERSION_CODE`: 강제 시각 이후 사용 가능한 최소 버전
- `ANDROID_LATEST_VERSION_CODE`: 스토어에 배포된 최신 버전
- `ANDROID_FORCE_UPDATE_AT`: ISO-8601 UTC 시각. 비워두면 최소 버전 조건을 즉시 적용
- `ANDROID_STORE_URL`: 업데이트 버튼이 열 URL
- `ANDROID_UPDATE_MESSAGE`: 강제 업데이트 화면에 표시할 문구

안전한 배포 순서는 신규 APK 배포 → `LATEST` 변경 → 충분한 업데이트 시간 후 `MIN` 및 `FORCE_UPDATE_AT` 변경입니다. 버전 정책 API 호출이 실패하면 서버 장애로 사용자가 잠기지 않도록 앱은 정상 진입합니다.
