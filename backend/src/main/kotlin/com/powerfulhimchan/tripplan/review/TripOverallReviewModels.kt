package com.powerfulhimchan.tripplan.review

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "trip_reviews")
class TripOverallReview(
    @Id val id: UUID = UUID.randomUUID(),
    @Column(name = "trip_id", nullable = false, unique = true) val tripId: UUID,
    @Column(nullable = false) var rating: Int,
    @Column(nullable = false, columnDefinition = "TEXT") var content: String,
    @Column(name = "representative_photo_id") var representativePhotoId: UUID? = null,
    @Column(name = "created_at", nullable = false) val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false) var updatedAt: Instant = Instant.now(),
) {
    protected constructor() : this(tripId = UUID.randomUUID(), rating = 1, content = "")
}

interface TripOverallReviewRepository : org.springframework.data.jpa.repository.JpaRepository<TripOverallReview, UUID> {
    fun findByTripId(tripId: UUID): TripOverallReview?
    fun findAllByTripIdIn(tripIds: Collection<UUID>): List<TripOverallReview>
}

data class SaveTripOverallReviewRequest(
    @field:Min(1) @field:Max(5) val rating: Int,
    @field:Size(max = 5000) val content: String,
    val representativePhotoId: UUID? = null,
)

data class RepresentativePhotoResponse(
    val id: UUID,
    val itemId: UUID,
    val originalName: String,
    val contentType: String,
    val sizeBytes: Long,
)

data class TripOverallReviewResponse(
    val id: UUID,
    val tripId: UUID,
    val rating: Int,
    val content: String,
    val representativePhoto: RepresentativePhotoResponse?,
    val updatedAt: Instant,
)
