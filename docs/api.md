# 여담 API

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
  "endsAt": "2027-05-01T11:00:00Z",
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

## 계획별 후기 사진 등록

먼저 후기 본문을 저장한 뒤 사진을 등록합니다.

`POST /api/v1/items/{itemId}/review/photos`

- Content-Type: `multipart/form-data`
- 폼 필드명: `files`
- 허용 형식: JPG, PNG, WEBP, HEIC
- 제한: 사진 한 장당 5MB 이하, 후기당 총 5장 이하

```bash
curl -X POST "$API_URL/api/v1/items/$ITEM_ID/review/photos" \
  -H "Authorization: Bearer $ACCESS_TOKEN" \
  -F "files=@photo1.jpg" \
  -F "files=@photo2.jpg"
```

사진 원본 조회와 삭제:

- `GET /api/v1/items/{itemId}/review/photos/{photoId}/content`
- `DELETE /api/v1/items/{itemId}/review/photos/{photoId}`

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
