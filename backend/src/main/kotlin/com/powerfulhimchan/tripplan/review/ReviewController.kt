package com.powerfulhimchan.tripplan.review

import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1/items/{itemId}/review")
class ReviewController(private val service: ReviewService) {
    @PutMapping
    fun save(@AuthenticationPrincipal jwt: Jwt, @PathVariable itemId: UUID,
             @Valid @RequestBody request: SaveReviewRequest) = service.save(jwt.subject, itemId, request)

    @GetMapping
    fun get(@AuthenticationPrincipal jwt: Jwt, @PathVariable itemId: UUID) = service.get(jwt.subject, itemId)
}
