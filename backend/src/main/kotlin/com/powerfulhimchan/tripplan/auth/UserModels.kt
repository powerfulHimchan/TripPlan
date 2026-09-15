package com.powerfulhimchan.tripplan.auth

import jakarta.persistence.*
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "app_users")
class AppUser(
    @Id @Column(length = 36) val id: String = UUID.randomUUID().toString(),
    @Column(nullable = false, unique = true, length = 320) val email: String,
    @Column(nullable = false, length = 100) val passwordHash: String,
    @Column(nullable = false) val createdAt: Instant = Instant.now(),
) {
    protected constructor() : this(email = "", passwordHash = "")
}

interface AppUserRepository : org.springframework.data.jpa.repository.JpaRepository<AppUser, String> {
    fun findByEmail(email: String): AppUser?
    fun existsByEmail(email: String): Boolean
}

data class EmailPasswordRequest(
    @field:Email(message = "올바른 이메일 주소를 입력해주세요.")
    @field:NotBlank(message = "이메일 주소를 입력해주세요.")
    val email: String,
    @field:Size(min = 8, max = 72, message = "비밀번호는 8자 이상 72자 이하여야 합니다.")
    val password: String,
)

data class AuthResponse(val accessToken: String, val userId: String, val email: String)

