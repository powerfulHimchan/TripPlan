package com.powerfulhimchan.tripplan.review

import jakarta.persistence.*
import jakarta.validation.constraints.*
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "trip_reviews")
class TripReview(
    @Id val id: UUID = UUID.randomUUID(),
    @Column(nullable = false, unique = true) val tripId: UUID,
    @Column(nullable = false) var rating: Int,
    @Column(nullable = false, columnDefinition = "TEXT") var content: String,
    @Column(nullable = false) val createdAt: Instant = Instant.now(),
    @Column(nullable = false) var updatedAt: Instant = Instant.now(),
) {
    protected constructor() : this(tripId = UUID.randomUUID(), rating = 1, content = "")
}

interface TripReviewRepository : org.springframework.data.jpa.repository.JpaRepository<TripReview, UUID> {
    fun findByTripId(tripId: UUID): TripReview?
}

data class SaveReviewRequest(
    @field:Min(1) @field:Max(5) val rating: Int,
    @field:NotBlank @field:Size(max = 5000) val content: String,
)

data class ReviewResponse(val id: UUID, val tripId: UUID, val rating: Int, val content: String, val updatedAt: Instant)

