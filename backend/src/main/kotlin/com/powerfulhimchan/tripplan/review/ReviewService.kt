package com.powerfulhimchan.tripplan.review

import com.powerfulhimchan.tripplan.trip.TripRepository
import com.powerfulhimchan.tripplan.trip.ItineraryItemRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.core.io.Resource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.*
import java.util.UUID

@Service
class ReviewService(
    private val reviews: TripReviewRepository,
    private val photos: ReviewPhotoRepository,
    private val photoStorage: ReviewPhotoStorage,
    private val trips: TripRepository,
    private val items: ItineraryItemRepository,
) {
    @Transactional
    fun save(userId: String, itemId: UUID, request: SaveReviewRequest): ReviewResponse {
        val item = items.findById(itemId).orElseThrow { EntityNotFoundException("일정을 찾을 수 없습니다.") }
        val trip = trips.findAccessible(item.tripId, userId) ?: throw EntityNotFoundException("여행을 찾을 수 없습니다.")
        val today = LocalDate.now(ZoneId.of(trip.timezone))
        require(!today.isBefore(trip.endDate)) { "여행 종료일 이후에 후기를 작성할 수 있습니다." }
        val review = reviews.findByItemId(itemId)?.apply {
            rating = request.rating
            content = request.content.trim()
            updatedAt = Instant.now()
        } ?: TripReview(itemId = itemId, rating = request.rating, content = request.content.trim())
        return reviews.save(review).toResponse()
    }

    @Transactional(readOnly = true)
    fun get(userId: String, itemId: UUID): ReviewResponse? {
        val item = items.findById(itemId).orElseThrow { EntityNotFoundException("일정을 찾을 수 없습니다.") }
        trips.findAccessible(item.tripId, userId) ?: throw EntityNotFoundException("여행을 찾을 수 없습니다.")
        return reviews.findByItemId(itemId)?.toResponse()
    }

    @Transactional
    fun addPhotos(userId: String, itemId: UUID, files: List<MultipartFile>): ReviewResponse {
        val review = accessibleReview(userId, itemId)
        require(files.isNotEmpty()) { "등록할 사진을 선택해주세요." }
        require(photos.countByReviewId(review.id) + files.size <= MAX_PHOTO_COUNT) {
            "후기 사진은 최대 ${MAX_PHOTO_COUNT}장까지 등록할 수 있습니다."
        }
        files.forEach(::validatePhoto)

        val stored = mutableListOf<ReviewPhoto>()
        try {
            files.forEach { file ->
                val photoId = UUID.randomUUID()
                val storedName = photoId.toString()
                photoStorage.store(storedName, file)
                stored += ReviewPhoto(
                    id = photoId,
                    reviewId = review.id,
                    storedName = storedName,
                    originalName = file.originalFilename?.takeLast(255)?.ifBlank { "photo" } ?: "photo",
                    contentType = file.contentType!!,
                    sizeBytes = file.size,
                )
            }
            photos.saveAll(stored)
        } catch (e: Exception) {
            stored.forEach { photoStorage.delete(it.storedName) }
            throw e
        }
        return review.toResponse()
    }

    @Transactional(readOnly = true)
    fun download(userId: String, itemId: UUID, photoId: UUID): ReviewPhotoDownload {
        val review = accessibleReview(userId, itemId)
        val photo = photos.findByIdAndReviewId(photoId, review.id)
            ?: throw EntityNotFoundException("후기 사진을 찾을 수 없습니다.")
        return ReviewPhotoDownload(photoStorage.load(photo.storedName), photo.originalName, photo.contentType, photo.sizeBytes)
    }

    @Transactional
    fun deletePhoto(userId: String, itemId: UUID, photoId: UUID): ReviewResponse {
        val review = accessibleReview(userId, itemId)
        val photo = photos.findByIdAndReviewId(photoId, review.id)
            ?: throw EntityNotFoundException("후기 사진을 찾을 수 없습니다.")
        photoStorage.delete(photo.storedName)
        photos.delete(photo)
        return review.toResponse()
    }

    private fun accessibleReview(userId: String, itemId: UUID): TripReview {
        val item = items.findById(itemId).orElseThrow { EntityNotFoundException("일정을 찾을 수 없습니다.") }
        trips.findAccessible(item.tripId, userId) ?: throw EntityNotFoundException("여행을 찾을 수 없습니다.")
        return reviews.findByItemId(itemId) ?: throw EntityNotFoundException("사진을 등록할 후기를 먼저 저장해주세요.")
    }

    private fun validatePhoto(file: MultipartFile) {
        require(!file.isEmpty) { "빈 사진 파일은 등록할 수 없습니다." }
        require(file.size <= MAX_PHOTO_SIZE) { "사진 한 장은 최대 5MB까지 등록할 수 있습니다." }
        require(file.contentType in ALLOWED_CONTENT_TYPES) { "JPG, PNG, WEBP, HEIC 사진만 등록할 수 있습니다." }
    }

    private fun TripReview.toResponse() = ReviewResponse(
        id, itemId, rating, content, updatedAt,
        photos.findAllByReviewIdOrderByCreatedAt(id).map { ReviewPhotoResponse(it.id, it.originalName, it.contentType, it.sizeBytes) },
    )

    companion object {
        const val MAX_PHOTO_COUNT = 5L
        const val MAX_PHOTO_SIZE = 5L * 1024 * 1024
        val ALLOWED_CONTENT_TYPES = setOf("image/jpeg", "image/jpg", "image/png", "image/webp", "image/heic", "image/heif")
    }
}

data class ReviewPhotoDownload(
    val resource: Resource,
    val originalName: String,
    val contentType: String,
    val sizeBytes: Long,
)
