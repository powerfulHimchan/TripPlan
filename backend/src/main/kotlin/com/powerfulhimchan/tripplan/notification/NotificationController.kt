package com.powerfulhimchan.tripplan.notification

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/devices")
class NotificationController(private val service: NotificationService) {
    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun register(@RequestHeader("X-User-Id") userId: String, @RequestBody request: RegisterDeviceRequest) =
        service.register(userId, request.token)
}

