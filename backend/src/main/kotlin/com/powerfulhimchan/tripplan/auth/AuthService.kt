package com.powerfulhimchan.tripplan.auth

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val users: AppUserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
) {
    @Transactional
    fun register(request: EmailPasswordRequest): AuthResponse {
        val email = request.email.trim().lowercase()
        require(!users.existsByEmail(email)) { "이미 가입된 이메일입니다." }
        val user = users.save(AppUser(email = email, passwordHash = passwordEncoder.encode(request.password)))
        return user.toAuthResponse()
    }

    @Transactional(readOnly = true)
    fun login(request: EmailPasswordRequest): AuthResponse {
        val user = users.findByEmail(request.email.trim().lowercase())
            ?: throw IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.")
        require(passwordEncoder.matches(request.password, user.passwordHash)) {
            "이메일 또는 비밀번호가 올바르지 않습니다."
        }
        return user.toAuthResponse()
    }

    private fun AppUser.toAuthResponse() = AuthResponse(jwtService.issue(this), id, email)
}

