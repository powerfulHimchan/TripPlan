package com.powerfulhimchan.tripplan.auth

import org.springframework.beans.factory.annotation.Value
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit

@Service
class JwtService(
    private val encoder: JwtEncoder,
    @Value("\${tripplan.auth.token-valid-days:30}") private val validDays: Long,
) {
    fun issue(user: AppUser): String {
        val now = Instant.now()
        val claims = JwtClaimsSet.builder()
            .issuer("tripplan")
            .issuedAt(now)
            .expiresAt(now.plus(validDays, ChronoUnit.DAYS))
            .subject(user.id)
            .claim("email", user.email)
            .build()
        val header = JwsHeader.with(MacAlgorithm.HS256).build()
        return encoder.encode(JwtEncoderParameters.from(header, claims)).tokenValue
    }
}
