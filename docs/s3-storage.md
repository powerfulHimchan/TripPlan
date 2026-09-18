# 후기 사진 S3 저장소

운영 환경에서는 후기 사진을 Railway 로컬 디스크 대신 Amazon S3에 저장합니다. Android API와 `review_photos.stored_name` 구조는 변경하지 않습니다.

## S3 버킷

- 권장 리전: `ap-northeast-2` (서울)
- Block all public access: 활성화
- 기본 암호화: SSE-S3
- 객체 키: `reviews/{photoId}`

## IAM 권한

Railway에서 사용할 IAM 사용자에게 후기 사진 경로만 허용합니다.

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": ["s3:PutObject", "s3:GetObject", "s3:DeleteObject"],
      "Resource": "arn:aws:s3:::YOUR_BUCKET_NAME/reviews/*"
    }
  ]
}
```

## Railway 환경변수

```text
REVIEW_STORAGE=s3
AWS_REGION=ap-northeast-2
AWS_S3_BUCKET=YOUR_BUCKET_NAME
AWS_S3_PREFIX=reviews
AWS_ACCESS_KEY_ID=YOUR_ACCESS_KEY
AWS_SECRET_ACCESS_KEY=YOUR_SECRET_KEY
```

`AWS_ACCESS_KEY_ID`와 `AWS_SECRET_ACCESS_KEY`는 GitHub에 저장하지 않고 Railway Variables에만 등록합니다. S3 저장을 사용하면 `REVIEW_UPLOAD_DIR`와 백엔드 사진용 Railway Volume은 필요하지 않습니다.

로컬 개발과 테스트는 `REVIEW_STORAGE`를 생략하거나 `local`로 설정하면 기존 파일 저장소를 사용합니다.
