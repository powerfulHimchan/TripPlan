package com.powerfulhimchan.tripplan.auth

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(private val service: AuthService) {
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    fun register(@Valid @RequestBody request: EmailPasswordRequest) = service.register(request)

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: EmailPasswordRequest) = service.login(request)
}

