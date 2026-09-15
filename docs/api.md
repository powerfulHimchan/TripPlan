# TripPlan API

MVP 인증 대용으로 모든 요청에 `X-User-Id` 헤더를 사용합니다.

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

## 후기 작성 또는 수정

`PUT /api/v1/trips/{tripId}/review`

```json
{ "rating": 5, "content": "아이와 함께한 첫 해외여행. 동선이 여유로워 좋았다." }
```

