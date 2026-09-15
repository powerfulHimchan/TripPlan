package com.powerfulhimchan.tripplan.notification

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "device_tokens")
class DeviceToken(
    @Id @Column(length = 512) val token: String,
    @Column(nullable = false, length = 100) var userId: String,
    @Column(nullable = false, length = 20) val platform: String = "ANDROID",
    @Column(nullable = false) var updatedAt: Instant = Instant.now(),
) {
    protected constructor() : this(token = "", userId = "")
}

data class RegisterDeviceRequest(val token: String)

interface DeviceTokenRepository : org.springframework.data.jpa.repository.JpaRepository<DeviceToken, String> {
    fun findAllByUserId(userId: String): List<DeviceToken>
}

