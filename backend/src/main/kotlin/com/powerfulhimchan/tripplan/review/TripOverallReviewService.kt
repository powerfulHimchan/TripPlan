package com.powerfulhimchan.tripplan.review

import com.powerfulhimchan.tripplan.trip.ItineraryItemRepository
import com.powerfulhimchan.tripplan.trip.TripRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@Service
class TripOverallReviewService(
    private val overallReviews: TripOverallReviewRepository,
    private val itineraryReviews: TripReviewRepository,
    private val photos: ReviewPhotoRepository,
    private val trips: TripRepository,
    private val items: ItineraryItemRepository,
) {
    @Transactional
    fun save(userId: String, tripId: UUID, request: SaveTripOverallReviewRequest): TripOverallReviewResponse {
        val trip = trips.findAccessible(tripId, userId)
            ?: throw EntityNotFoundException("여행을 찾을 수 없습니다.")
        require(LocalDate.now(ZoneId.of(trip.timezone)).isAfter(trip.endDate)) {
            "여행 종료 후에 전체 후기를 작성할 수 있습니다."
        }
        request.representativePhotoId?.let { representativePhoto(tripId, it) }
        val review = overallReviews.findByTripId(tripId)?.apply {
            rating = request.rating
            content = request.content.trim()
            representativePhotoId = request.representativePhotoId
            updatedAt = Instant.now()
        } ?: TripOverallReview(
            tripId = tripId,
            rating = request.rating,
            content = request.content.trim(),
            representativePhotoId = request.representativePhotoId,
        )
        return overallReviews.save(review).toResponse()
    }

    @Transactional(readOnly = true)
    fun get(userId: String, tripId: UUID): TripOverallReviewResponse? {
        trips.findAccessible(tripId, userId) ?: throw EntityNotFoundException("여행을 찾을 수 없습니다.")
        return overallReviews.findByTripId(tripId)?.toResponse()
    }

    @Transactional(readOnly = true)
    fun getAll(userId: String): List<TripOverallReviewResponse> {
        val tripIds = trips.findAllAccessible(userId).map { it.id }
        if (tripIds.isEmpty()) return emptyList()
        val reviews = overallReviews.findAllByTripIdIn(tripIds)
        if (reviews.isEmpty()) return emptyList()

        val photoIds = reviews.mapNotNull { it.representativePhotoId }
        val photosById = photos.findAllById(photoIds).associateBy { it.id }
        val itineraryReviewsById = itineraryReviews.findAllById(photosById.values.map { it.reviewId })
            .associateBy { it.id }
        val itemsById = items.findAllById(itineraryReviewsById.values.map { it.itemId })
            .associateBy { it.id }

        return reviews.map { review ->
            val representative = review.representativePhotoId?.let { photoId ->
                val photo = photosById[photoId] ?: return@let null
                val itineraryReview = itineraryReviewsById[photo.reviewId] ?: return@let null
                val item = itemsById[itineraryReview.itemId]
                    ?.takeIf { it.tripId == review.tripId }
                    ?: return@let null
                RepresentativePhotoResponse(photo.id, item.id, photo.originalName, photo.contentType, photo.sizeBytes)
            }
            review.toResponse(representative)
        }
    }

    private fun TripOverallReview.toResponse(
        representative: RepresentativePhotoResponse? = representativePhotoId?.let { representativePhoto(tripId, it) },
    ) = TripOverallReviewResponse(
        id = id,
        tripId = tripId,
        rating = rating,
        content = content,
        representativePhoto = representative,
        updatedAt = updatedAt,
    )

    private fun representativePhoto(tripId: UUID, photoId: UUID): RepresentativePhotoResponse {
        val photo = photos.findById(photoId).orElseThrow { EntityNotFoundException("대표 사진을 찾을 수 없습니다.") }
        val review = itineraryReviews.findById(photo.reviewId)
            .orElseThrow { EntityNotFoundException("사진의 후기를 찾을 수 없습니다.") }
        val item = items.findById(review.itemId)
            .orElseThrow { EntityNotFoundException("사진의 일정을 찾을 수 없습니다.") }
        require(item.tripId == tripId) { "해당 여행에 등록된 사진만 대표 사진으로 선택할 수 있습니다." }
        return RepresentativePhotoResponse(photo.id, item.id, photo.originalName, photo.contentType, photo.sizeBytes)
    }
}
