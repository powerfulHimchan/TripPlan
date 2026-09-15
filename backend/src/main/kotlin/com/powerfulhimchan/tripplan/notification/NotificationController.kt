package com.powerfulhimchan.tripplan.notification

import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/devices")
class NotificationController(private val service: NotificationService) {
    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun register(@AuthenticationPrincipal jwt: Jwt, @RequestBody request: RegisterDeviceRequest) =
        service.register(jwt.subject, request.token)
}
