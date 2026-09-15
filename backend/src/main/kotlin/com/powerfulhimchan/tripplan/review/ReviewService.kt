package com.powerfulhimchan.tripplan.review

import com.powerfulhimchan.tripplan.trip.TripRepository
import com.powerfulhimchan.tripplan.trip.ItineraryItemRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.*
import java.util.UUID

@Service
class ReviewService(
    private val reviews: TripReviewRepository,
    private val trips: TripRepository,
    private val items: ItineraryItemRepository,
) {
    @Transactional
    fun save(userId: String, itemId: UUID, request: SaveReviewRequest): ReviewResponse {
        val item = items.findById(itemId).orElseThrow { EntityNotFoundException("일정을 찾을 수 없습니다.") }
        val trip = trips.findByIdAndUserId(item.tripId, userId) ?: throw EntityNotFoundException("여행을 찾을 수 없습니다.")
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
        trips.findByIdAndUserId(item.tripId, userId) ?: throw EntityNotFoundException("여행을 찾을 수 없습니다.")
        return reviews.findByItemId(itemId)?.toResponse()
    }

    private fun TripReview.toResponse() = ReviewResponse(id, itemId, rating, content, updatedAt)
}
