package com.powerfulhimchan.tripplan.review

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/items/{itemId}/review")
class ReviewController(private val service: ReviewService) {
    @PutMapping
    fun save(@RequestHeader("X-User-Id") userId: String, @PathVariable itemId: UUID,
             @Valid @RequestBody request: SaveReviewRequest) = service.save(userId, itemId, request)

    @GetMapping
    fun get(@RequestHeader("X-User-Id") userId: String, @PathVariable itemId: UUID) = service.get(userId, itemId)
}
