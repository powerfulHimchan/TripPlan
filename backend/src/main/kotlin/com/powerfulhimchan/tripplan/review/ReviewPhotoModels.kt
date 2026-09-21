package com.powerfulhimchan.tripplan.review

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "review_photos")
class ReviewPhoto(
    @Id val id: UUID = UUID.randomUUID(),
    @Column(name = "review_id", nullable = false) val reviewId: UUID,
    @Column(name = "stored_name", nullable = false, unique = true, length = 80) val storedName: String,
    @Column(name = "original_name", nullable = false, length = 255) val originalName: String,
    @Column(name = "content_type", nullable = false, length = 80) val contentType: String,
    @Column(name = "size_bytes", nullable = false) val sizeBytes: Long,
    @Column(name = "created_at", nullable = false) val createdAt: Instant = Instant.now(),
) {
    protected constructor() : this(reviewId = UUID.randomUUID(), storedName = "", originalName = "", contentType = "", sizeBytes = 0)
}

interface ReviewPhotoRepository : org.springframework.data.jpa.repository.JpaRepository<ReviewPhoto, UUID> {
    fun findAllByReviewIdOrderByCreatedAt(reviewId: UUID): List<ReviewPhoto>
    fun findAllByReviewIdInOrderByCreatedAt(reviewIds: Collection<UUID>): List<ReviewPhoto>
    fun findByIdAndReviewId(id: UUID, reviewId: UUID): ReviewPhoto?
    fun countByReviewId(reviewId: UUID): Long
}

data class ReviewPhotoResponse(
    val id: UUID,
    val originalName: String,
    val contentType: String,
    val sizeBytes: Long,
)
