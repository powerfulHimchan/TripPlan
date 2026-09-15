# TripPlan API

회원가입과 로그인을 제외한 요청에는 `Authorization: Bearer {accessToken}` 헤더를 사용합니다.

## 회원가입

`POST /api/v1/auth/register`

```json
{ "email": "user@example.com", "password": "password1" }
```

## 로그인

`POST /api/v1/auth/login`

```json
{ "email": "user@example.com", "password": "password1" }
```

## 여행 생성

`POST /api/v1/trips`

```json
{
  "title": "오사카 가족여행",
  "destination": "오사카",
  "startDate": "2027-05-01",
  "endDate": "2027-05-04",
  "timezone": "Asia/Tokyo"
}
```

## 일정 추가

`POST /api/v1/trips/{tripId}/items`

```json
{
  "title": "도톤보리 저녁",
  "place": "도톤보리",
  "memo": "예약번호 확인",
  "scheduledAt": "2027-05-01T09:00:00Z",
  "notificationEnabled": true,
  "notificationMinutesBefore": 30
}
```

## 일정 알림 변경

`PATCH /api/v1/items/{itemId}/notification`

```json
{ "enabled": false, "minutesBefore": 0 }
```

## 기기 토큰 등록

`POST /api/v1/devices`

```json
{ "token": "FCM_DEVICE_TOKEN" }
```

## 계획별 후기 작성 또는 수정

`PUT /api/v1/items/{itemId}/review`

```json
{ "rating": 5, "content": "아이와 함께한 첫 해외여행. 동선이 여유로워 좋았다." }
```

## 여행 공유 초대

`POST /api/v1/trips/{tripId}/invitations`

```json
{ "email": "friend@example.com" }
```

## 받은 초대

`GET /api/v1/invitations`

## 초대 수락 또는 거절

- `POST /api/v1/invitations/{invitationId}/accept`
- `POST /api/v1/invitations/{invitationId}/decline`

수락한 회원은 해당 여행과 계획을 조회하고 함께 수정할 수 있습니다.
