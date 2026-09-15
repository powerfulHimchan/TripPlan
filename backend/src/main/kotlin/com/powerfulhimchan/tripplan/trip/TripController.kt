package com.powerfulhimchan.tripplan.trip

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class TripController(private val service: TripService) {
    @PostMapping("/trips")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@RequestHeader("X-User-Id") userId: String, @Valid @RequestBody request: CreateTripRequest) =
        service.create(userId, request)

    @GetMapping("/trips")
    fun list(@RequestHeader("X-User-Id") userId: String) = service.list(userId)

    @GetMapping("/trips/{tripId}")
    fun get(@RequestHeader("X-User-Id") userId: String, @PathVariable tripId: UUID) = service.get(userId, tripId)

    @PostMapping("/trips/{tripId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    fun addItem(@RequestHeader("X-User-Id") userId: String, @PathVariable tripId: UUID,
                @Valid @RequestBody request: CreateItineraryItemRequest) = service.addItem(userId, tripId, request)

    @PatchMapping("/items/{itemId}/notification")
    fun updateNotification(@RequestHeader("X-User-Id") userId: String, @PathVariable itemId: UUID,
                           @Valid @RequestBody request: UpdateNotificationRequest) =
        service.updateNotification(userId, itemId, request)

    @DeleteMapping("/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteItem(@RequestHeader("X-User-Id") userId: String, @PathVariable itemId: UUID) =
        service.deleteItem(userId, itemId)
}

