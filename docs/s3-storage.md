# 후기 사진 Railway Storage Bucket 저장소

운영 환경에서는 후기 사진을 Railway 로컬 디스크 대신 S3 호환 Railway Storage Bucket에 저장합니다. Android API와 `review_photos.stored_name` 구조는 변경하지 않습니다.

## Storage Bucket

- 이름: `yeodam-review-photos`
- 백엔드와 동일한 리전 사용
- Bucket은 비공개로 유지하고 사진은 백엔드 API를 통해 제공
- 객체 키: `reviews/{photoId}`

## Railway 환경변수

```text
REVIEW_STORAGE=s3
AWS_S3_ENDPOINT=${{yeodam-review-photos.ENDPOINT}}
AWS_REGION=${{yeodam-review-photos.REGION}}
AWS_S3_BUCKET=${{yeodam-review-photos.BUCKET}}
AWS_S3_PREFIX=reviews
AWS_ACCESS_KEY_ID=${{yeodam-review-photos.ACCESS_KEY_ID}}
AWS_SECRET_ACCESS_KEY=${{yeodam-review-photos.SECRET_ACCESS_KEY}}
```

`BUCKET`은 S3 API용 실제 버킷명입니다. `RAILWAY_BUCKET_NAME`을 대신 사용하면 안 됩니다. 자격 증명은 Railway Variable Reference로 연결하고 GitHub에 저장하지 않습니다. Bucket 저장을 사용하면 `REVIEW_UPLOAD_DIR`와 백엔드 사진용 Railway Volume은 필요하지 않습니다.

Railway Storage Bucket은 S3의 `PutObject`, `GetObject`, `DeleteObject`를 지원하지만 서버 측 암호화 요청 헤더는 지원하지 않습니다. 따라서 애플리케이션은 `PutObject`에 별도 암호화 옵션을 지정하지 않습니다.

로컬 개발과 테스트는 `REVIEW_STORAGE`를 생략하거나 `local`로 설정하면 기존 파일 저장소를 사용합니다.
