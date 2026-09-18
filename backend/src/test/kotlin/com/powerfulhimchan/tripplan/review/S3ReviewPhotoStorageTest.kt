package com.powerfulhimchan.tripplan.review

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.mock.web.MockMultipartFile
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.PutObjectRequest

class S3ReviewPhotoStorageTest {
    @Test
    fun `후기 사진을 S3 호환 객체로 저장한다`() {
        val s3 = mock(S3Client::class.java)
        val storage = S3ReviewPhotoStorage(s3, "yeodam-review-photos", "/reviews/")
        val file = MockMultipartFile("files", "jeju.jpg", "image/jpeg", "photo-data".toByteArray())

        storage.store("photo-id", file)

        val request = ArgumentCaptor.forClass(PutObjectRequest::class.java)
        verify(s3).putObject(request.capture(), org.mockito.ArgumentMatchers.any(RequestBody::class.java))
        assertThat(request.value.bucket()).isEqualTo("yeodam-review-photos")
        assertThat(request.value.key()).isEqualTo("reviews/photo-id")
        assertThat(request.value.contentType()).isEqualTo("image/jpeg")
        assertThat(request.value.serverSideEncryption()).isNull()
    }

    @Test
    fun `Railway Storage Bucket endpoint로 S3 client를 생성한다`() {
        val client = S3ReviewPhotoConfig().reviewPhotoS3Client("auto", "https://t3.storageapi.dev")

        client.close()
    }
}
