package com.powerfulhimchan.tripplan.trip

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class TripController(
    private val service: TripService,
    private val detailService: TripDetailService,
) {
    @PostMapping("/trips")
    @ResponseStatus(HttpStatus.CREATED)
    fun create(@AuthenticationPrincipal jwt: Jwt, @Valid @RequestBody request: CreateTripRequest) =
        service.create(jwt.subject, request)

    @GetMapping("/trips")
    fun list(@AuthenticationPrincipal jwt: Jwt, @RequestParam(defaultValue = "false") archived: Boolean) =
        service.list(jwt.subject, archived)

    @GetMapping("/trips/{tripId}")
    fun get(@AuthenticationPrincipal jwt: Jwt, @PathVariable tripId: UUID) = service.get(jwt.subject, tripId)

    @PatchMapping("/trips/{tripId}")
    fun update(@AuthenticationPrincipal jwt: Jwt, @PathVariable tripId: UUID,
               @Valid @RequestBody request: UpdateTripRequest) = service.update(jwt.subject, tripId, request)

    @PostMapping("/trips/{tripId}/archive")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun archive(@AuthenticationPrincipal jwt: Jwt, @PathVariable tripId: UUID) =
        service.archive(jwt.subject, tripId)

    @DeleteMapping("/trips/{tripId}/archive")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun unarchive(@AuthenticationPrincipal jwt: Jwt, @PathVariable tripId: UUID) =
        service.unarchive(jwt.subject, tripId)

    @DeleteMapping("/trips/{tripId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(@AuthenticationPrincipal jwt: Jwt, @PathVariable tripId: UUID) =
        service.delete(jwt.subject, tripId)

    @GetMapping("/trips/{tripId}/details")
    fun details(@AuthenticationPrincipal jwt: Jwt, @PathVariable tripId: UUID) =
        detailService.get(jwt.subject, tripId)

    @PostMapping("/trips/{tripId}/items")
    @ResponseStatus(HttpStatus.CREATED)
    fun addItem(@AuthenticationPrincipal jwt: Jwt, @PathVariable tripId: UUID,
                @Valid @RequestBody request: CreateItineraryItemRequest) = service.addItem(jwt.subject, tripId, request)

    @PutMapping("/items/{itemId}")
    fun updateItem(@AuthenticationPrincipal jwt: Jwt, @PathVariable itemId: UUID,
                   @Valid @RequestBody request: UpdateItineraryItemRequest) =
        service.updateItem(jwt.subject, itemId, request)

    @PatchMapping("/items/{itemId}/notification")
    fun updateNotification(@AuthenticationPrincipal jwt: Jwt, @PathVariable itemId: UUID,
                           @Valid @RequestBody request: UpdateNotificationRequest) =
        service.updateNotification(jwt.subject, itemId, request)

    @DeleteMapping("/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteItem(@AuthenticationPrincipal jwt: Jwt, @PathVariable itemId: UUID) =
        service.deleteItem(jwt.subject, itemId)
}
