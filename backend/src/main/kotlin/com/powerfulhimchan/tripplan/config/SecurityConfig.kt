package com.powerfulhimchan.tripplan.config

import com.nimbusds.jose.jwk.source.ImmutableSecret
import com.nimbusds.jose.proc.SecurityContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.web.SecurityFilterChain
import javax.crypto.spec.SecretKeySpec

@Configuration
class SecurityConfig {
    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain = http
        .csrf { it.disable() }
        .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
        .authorizeHttpRequests {
            it.requestMatchers("/api/v1/auth/**", "/error").permitAll()
                .anyRequest().authenticated()
        }
        .oauth2ResourceServer { it.jwt {} }
        .build()

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun jwtSecretKey(@Value("\${tripplan.auth.jwt-secret}") secret: String): SecretKeySpec {
        require(secret.toByteArray().size >= 32) { "JWT_SECRET은 32바이트 이상이어야 합니다." }
        return SecretKeySpec(secret.toByteArray(), "HmacSHA256")
    }

    @Bean
    fun jwtEncoder(key: SecretKeySpec): JwtEncoder = NimbusJwtEncoder(ImmutableSecret<SecurityContext>(key))

    @Bean
    fun jwtDecoder(key: SecretKeySpec): JwtDecoder = NimbusJwtDecoder.withSecretKey(key)
        .macAlgorithm(MacAlgorithm.HS256)
        .build()
}

