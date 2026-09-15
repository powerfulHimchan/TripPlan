package com.powerfulhimchan.tripplan.sharing

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import java.util.UUID

@RestController
@RequestMapping("/api/v1")
class SharingController(private val service: SharingService) {
    @PostMapping("/trips/{tripId}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    fun invite(@AuthenticationPrincipal jwt: Jwt, @PathVariable tripId: UUID, @Valid @RequestBody request: InviteRequest) =
        service.invite(jwt.subject, tripId, request)

    @GetMapping("/invitations")
    fun received(@AuthenticationPrincipal jwt: Jwt) = service.received(jwt.subject)

    @PostMapping("/invitations/{invitationId}/accept")
    fun accept(@AuthenticationPrincipal jwt: Jwt, @PathVariable invitationId: UUID) = service.accept(jwt.subject, invitationId)

    @PostMapping("/invitations/{invitationId}/decline")
    fun decline(@AuthenticationPrincipal jwt: Jwt, @PathVariable invitationId: UUID) = service.decline(jwt.subject, invitationId)

    @GetMapping("/trips/{tripId}/members")
    fun members(@AuthenticationPrincipal jwt: Jwt, @PathVariable tripId: UUID) = service.members(jwt.subject, tripId)
}

