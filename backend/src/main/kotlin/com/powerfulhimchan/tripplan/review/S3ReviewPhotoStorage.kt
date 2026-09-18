package com.powerfulhimchan.tripplan.review

import jakarta.persistence.EntityNotFoundException
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.stereotype.Component
import org.springframework.web.multipart.MultipartFile
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.S3Exception
import software.amazon.awssdk.services.s3.model.ServerSideEncryption

@Component
@ConditionalOnExpression("'\${REVIEW_STORAGE:local}' == 's3'")
class S3ReviewPhotoStorage(
    private val s3: S3Client,
    @Value("\${AWS_S3_BUCKET:}") private val bucket: String,
    @Value("\${AWS_S3_PREFIX:reviews}") prefix: String,
) : ReviewPhotoStorage {
    private val normalizedPrefix = prefix.trim().trim('/').takeIf(String::isNotEmpty)

    init {
        require(bucket.isNotBlank()) { "REVIEW_STORAGE=s3인 경우 AWS_S3_BUCKET을 설정해야 합니다." }
    }

    override fun store(storedName: String, file: MultipartFile) {
        val request = PutObjectRequest.builder()
            .bucket(bucket)
            .key(objectKey(storedName))
            .contentType(file.contentType)
            .contentLength(file.size)
            .serverSideEncryption(ServerSideEncryption.AES256)
            .build()
        file.inputStream.use { input ->
            s3.putObject(request, RequestBody.fromInputStream(input, file.size))
        }
    }

    override fun load(storedName: String): Resource = try {
        val bytes = s3.getObjectAsBytes(
            GetObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey(storedName))
                .build(),
        ).asByteArray()
        ByteArrayResource(bytes)
    } catch (e: NoSuchKeyException) {
        throw photoNotFound(e)
    } catch (e: S3Exception) {
        if (e.statusCode() == 404) throw photoNotFound(e)
        throw e
    }

    override fun delete(storedName: String) {
        s3.deleteObject(
            DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey(storedName))
                .build(),
        )
    }

    internal fun objectKey(storedName: String): String {
        require(storedName.isNotBlank() && '/' !in storedName && '\\' !in storedName) {
            "올바르지 않은 사진 객체 키입니다."
        }
        return normalizedPrefix?.let { "$it/$storedName" } ?: storedName
    }

    private fun photoNotFound(cause: Exception) =
        EntityNotFoundException("사진 파일을 찾을 수 없습니다.").apply { initCause(cause) }
}

@Configuration(proxyBeanMethods = false)
@ConditionalOnExpression("'\${REVIEW_STORAGE:local}' == 's3'")
class S3ReviewPhotoConfig {
    @Bean(destroyMethod = "close")
    fun reviewPhotoS3Client(@Value("\${AWS_REGION:ap-northeast-2}") region: String): S3Client =
        S3Client.builder()
            .region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build()
}
