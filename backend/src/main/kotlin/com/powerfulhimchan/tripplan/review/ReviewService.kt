package com.powerfulhimchan.tripplan.review

import com.powerfulhimchan.tripplan.trip.TripRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.*
import java.util.UUID

@Service
class ReviewService(private val reviews: TripReviewRepository, private val trips: TripRepository) {
    @Transactional
    fun save(userId: String, tripId: UUID, request: SaveReviewRequest): ReviewResponse {
        val trip = trips.findByIdAndUserId(tripId, userId) ?: throw EntityNotFoundException("여행을 찾을 수 없습니다.")
        val today = LocalDate.now(ZoneId.of(trip.timezone))
        require(!today.isBefore(trip.endDate)) { "여행 종료일 이후에 후기를 작성할 수 있습니다." }
        val review = reviews.findByTripId(tripId)?.apply {
            rating = request.rating
            content = request.content.trim()
            updatedAt = Instant.now()
        } ?: TripReview(tripId = tripId, rating = request.rating, content = request.content.trim())
        return reviews.save(review).toResponse()
    }

    @Transactional(readOnly = true)
    fun get(userId: String, tripId: UUID): ReviewResponse? {
        trips.findByIdAndUserId(tripId, userId) ?: throw EntityNotFoundException("여행을 찾을 수 없습니다.")
        return reviews.findByTripId(tripId)?.toResponse()
    }

    private fun TripReview.toResponse() = ReviewResponse(id, tripId, rating, content, updatedAt)
}

