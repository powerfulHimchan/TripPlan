# 여담

> 여행을 계획하고, 함께 담다

이메일로 가입해 여행을 작성하고, 일정별 Push 알림·계획별 후기·회원 간 공유를 이용하는 Android + Kotlin Backend 앱입니다.

## 구성

- `backend`: Kotlin, Spring Boot, PostgreSQL, Flyway, Firebase Admin SDK
- `android`: Kotlin, Jetpack Compose, Retrofit, Firebase Cloud Messaging
- `docs/api.md`: 주요 API와 호출 예시

## 핵심 기능

1. 이메일/비밀번호 회원가입 및 JWT 로그인
2. 여행 생성 및 목록 조회
3. 여행에 시간순 일정 추가
4. 시작·종료 시각 기반 전체 타임라인과 예정·진행 중·완료 상태 표시
5. 일정마다 Push 알림 On/Off 및 알림 시각 설정
6. 서버 스케줄러가 도래한 일정을 FCM으로 전송
7. 종료된 여행의 각 계획에 별점·후기와 사진 저장(장당 5MB, 최대 5장)
8. 가입된 회원 초대, 수락/거절 및 공동 편집

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
export JWT_SECRET=32바이트_이상의_운영용_랜덤_문자열
export REVIEW_UPLOAD_DIR=/영속_볼륨/tripplan/reviews
```

### 3. Android 실행

1. Android Studio에서 `android` 폴더를 엽니다.
2. Firebase Console에서 Android 앱 패키지 `com.powerfulhimchan.tripplan`을 등록합니다.
3. 받은 `google-services.json`을 `android/app/`에 둡니다.
4. 에뮬레이터에서는 Backend 주소가 기본값 `http://10.0.2.2:8080/`입니다.
5. 실제 폰에서는 `BuildConfig.API_BASE_URL`을 PC의 같은 Wi-Fi IP 또는 운영 API 주소로 변경합니다.

## Railway 배포

백엔드는 `backend/Dockerfile`로 컨테이너 빌드할 수 있습니다.

1. Railway 프로젝트에서 이 GitHub 저장소를 연결합니다.
2. 백엔드 서비스의 Root Directory를 `/backend`로 지정합니다.
3. 같은 프로젝트에 PostgreSQL 서비스를 추가합니다.
4. 백엔드 서비스에 아래 변수를 설정합니다.

```text
DB_URL=jdbc:postgresql://${{Postgres.PGHOST}}:${{Postgres.PGPORT}}/${{Postgres.PGDATABASE}}
DB_USERNAME=${{Postgres.PGUSER}}
DB_PASSWORD=${{Postgres.PGPASSWORD}}
JWT_SECRET=32바이트 이상의 운영용 랜덤 문자열
REVIEW_STORAGE=s3
AWS_S3_ENDPOINT=${{yeodam-review-photos.ENDPOINT}}
AWS_REGION=${{yeodam-review-photos.REGION}}
AWS_S3_BUCKET=${{yeodam-review-photos.BUCKET}}
AWS_S3_PREFIX=reviews
AWS_ACCESS_KEY_ID=${{yeodam-review-photos.ACCESS_KEY_ID}}
AWS_SECRET_ACCESS_KEY=${{yeodam-review-photos.SECRET_ACCESS_KEY}}
```

5. 같은 프로젝트에 `yeodam-review-photos` Storage Bucket을 추가하고 위 Variable Reference를 연결합니다.
6. Networking에서 Railway HTTPS 도메인을 생성합니다.

Railway가 제공하는 `PORT` 환경변수는 Spring Boot가 자동으로 사용합니다. Push를 실제 전송하기 전까지 `FCM_ENABLED`는 `false`로 둡니다.

## MVP 전제

- 비밀번호는 BCrypt로 저장하고 API 인증은 Bearer JWT를 사용합니다.
- 후기 사진은 로컬 개발에서는 `uploads/reviews`, Railway 운영 환경에서는 S3 호환 Storage Bucket에 저장합니다.
- 운영 전 Push 재시도/실패 큐, 이메일 인증, 비밀번호 재설정 기능을 추가해야 합니다.
- 일정 알림은 서버 기준 1분 간격으로 확인하며 중복 발송 방지 시각을 DB에 기록합니다.

## 테스트

```bash
cd backend
gradle test
```
