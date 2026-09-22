package com.powerfulhimchan.tripplan.review

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/trips/{tripId}/review")
class TripOverallReviewController(private val service: TripOverallReviewService) {
    @PutMapping
    fun save(
        @AuthenticationPrincipal jwt: Jwt,
        @PathVariable tripId: UUID,
        @Valid @RequestBody request: SaveTripOverallReviewRequest,
    ) = service.save(jwt.subject, tripId, request)

    @GetMapping
    fun get(@AuthenticationPrincipal jwt: Jwt, @PathVariable tripId: UUID): ResponseEntity<TripOverallReviewResponse> =
        service.get(jwt.subject, tripId)?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.noContent().build()
}

@RestController
@RequestMapping("/api/v1/trips/overall-reviews")
class TripOverallReviewListController(private val service: TripOverallReviewService) {
    @GetMapping
    fun getAll(@AuthenticationPrincipal jwt: Jwt) = service.getAll(jwt.subject)
}
