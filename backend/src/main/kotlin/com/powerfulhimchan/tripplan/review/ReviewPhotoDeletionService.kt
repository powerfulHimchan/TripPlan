package com.powerfulhimchan.tripplan.review

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "review_photo_deletions")
class ReviewPhotoDeletion(
    @Id val id: UUID = UUID.randomUUID(),
    @Column(name = "stored_name", nullable = false, unique = true, length = 80) val storedName: String,
    @Column(nullable = false) var attempts: Int = 0,
    @Column(name = "last_error", length = 500) var lastError: String? = null,
    @Column(name = "created_at", nullable = false) val createdAt: Instant = Instant.now(),
    @Column(name = "updated_at", nullable = false) var updatedAt: Instant = Instant.now(),
) {
    protected constructor() : this(storedName = "")
}

interface ReviewPhotoDeletionRepository : org.springframework.data.jpa.repository.JpaRepository<ReviewPhotoDeletion, UUID> {
    fun findTop100ByOrderByCreatedAtAsc(): List<ReviewPhotoDeletion>
}

data class ReviewPhotoDeletionRequested(val ids: List<UUID>)

@Service
class ReviewPhotoDeletionService(
    private val deletions: ReviewPhotoDeletionRepository,
    private val events: ApplicationEventPublisher,
) {
    fun enqueue(storedNames: Collection<String>) {
        if (storedNames.isEmpty()) return
        val queued = deletions.saveAll(storedNames.distinct().map { ReviewPhotoDeletion(storedName = it) })
        events.publishEvent(ReviewPhotoDeletionRequested(queued.map { it.id }))
    }
}

@Service
class ReviewPhotoDeletionListener(
    private val worker: ReviewPhotoDeletionWorker,
) {
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun afterCommit(event: ReviewPhotoDeletionRequested) = worker.process(event.ids)
}

@Service
class ReviewPhotoDeletionWorker(
    private val deletions: ReviewPhotoDeletionRepository,
    private val storage: ReviewPhotoStorage,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelayString = "\${tripplan.review.deletion-retry-ms:60000}")
    fun retryPending() = process(deletions.findTop100ByOrderByCreatedAtAsc().map { it.id })

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun process(ids: Collection<UUID>) {
        deletions.findAllById(ids).forEach { deletion ->
            runCatching { storage.delete(deletion.storedName) }
                .onSuccess { deletions.delete(deletion) }
                .onFailure { error ->
                    deletion.attempts += 1
                    deletion.lastError = (error.message ?: error.javaClass.simpleName).take(500)
                    deletion.updatedAt = Instant.now()
                    deletions.save(deletion)
                    log.warn("후기 사진 삭제 재시도 대기: storedName={}, attempts={}", deletion.storedName, deletion.attempts, error)
                }
        }
    }
}
