# TripPlan

여행 일정 작성, 일정별 Push 알림 설정, 여행 종료 후 후기를 기록하는 Android + Kotlin Backend MVP입니다.

## 구성

- `backend`: Kotlin, Spring Boot, PostgreSQL, Flyway, Firebase Admin SDK
- `android`: Kotlin, Jetpack Compose, Retrofit, Firebase Cloud Messaging
- `docs/api.md`: 주요 API와 호출 예시

## 핵심 기능

1. 여행 생성 및 목록 조회
2. 여행에 시간순 일정 추가
3. 일정마다 Push 알림 On/Off 및 알림 시각 설정
4. 서버 스케줄러가 도래한 일정을 FCM으로 전송
5. 종료된 여행에 별점과 후기 저장

## 빠른 실행

### 1. PostgreSQL 실행

```bash
docker compose up -d db
```

### 2. Backend 실행

JDK 21과 Gradle 8.10+ 환경에서:

```bash
cd backend
gradle bootRun
```

Firebase 키가 없으면 Push는 서버 로그로 출력됩니다. 실제 전송은 아래 환경 변수를 지정합니다.

```bash
export GOOGLE_APPLICATION_CREDENTIALS=/absolute/path/firebase-service-account.json
export FCM_ENABLED=true
```

### 3. Android 실행

1. Android Studio에서 `android` 폴더를 엽니다.
2. Firebase Console에서 Android 앱 패키지 `com.powerfulhimchan.tripplan`을 등록합니다.
3. 받은 `google-services.json`을 `android/app/`에 둡니다.
4. 에뮬레이터에서는 Backend 주소가 기본값 `http://10.0.2.2:8080/`입니다.
5. 실제 폰에서는 `BuildConfig.API_BASE_URL`을 PC의 같은 Wi-Fi IP 또는 운영 API 주소로 변경합니다.

## MVP 전제

- 인증 전 단계라 Android 앱은 임시 사용자 ID `demo-user`를 사용합니다.
- 운영 전 OAuth/JWT 인증, Push 재시도/실패 큐, 시간대 검증, 이미지 업로드 기능을 추가해야 합니다.
- 일정 알림은 서버 기준 1분 간격으로 확인하며 중복 발송 방지 시각을 DB에 기록합니다.

## 테스트

```bash
cd backend
gradle test
```

