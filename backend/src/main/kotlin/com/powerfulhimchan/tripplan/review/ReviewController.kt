package com.powerfulhimchan.tripplan.review

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/trips/{tripId}/review")
class ReviewController(private val service: ReviewService) {
    @PutMapping
    fun save(@RequestHeader("X-User-Id") userId: String, @PathVariable tripId: UUID,
             @Valid @RequestBody request: SaveReviewRequest) = service.save(userId, tripId, request)

    @GetMapping
    fun get(@RequestHeader("X-User-Id") userId: String, @PathVariable tripId: UUID) = service.get(userId, tripId)
}

