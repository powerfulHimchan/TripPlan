package com.powerfulhimchan.tripplan.version

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.Clock
import java.time.Instant

data class AppVersionResponse(
    val platform: String,
    val currentVersionCode: Int,
    val minimumVersionCode: Int,
    val latestVersionCode: Int,
    val updateAvailable: Boolean,
    val updateRequired: Boolean,
    val forceUpdateAt: Instant?,
    val storeUrl: String,
    val message: String,
)

@RestController
@RequestMapping("/api/v1/app-version")
class AppVersionController(private val service: AppVersionService) {
    @GetMapping("/android")
    fun android(@RequestParam currentVersionCode: Int): AppVersionResponse =
        service.androidPolicy(currentVersionCode)
}

class AppVersionService(
    @Value("\${ANDROID_MIN_VERSION_CODE:1}")
    private val minimumVersionCode: Int,
    @Value("\${ANDROID_LATEST_VERSION_CODE:2}")
    private val latestVersionCode: Int,
    @Value("\${ANDROID_FORCE_UPDATE_AT:}")
    forceUpdateAtValue: String,
    @Value("\${ANDROID_STORE_URL:https://play.google.com/store/apps/details?id=com.powerfulhimchan.tripplan}")
    private val storeUrl: String,
    @Value("\${ANDROID_UPDATE_MESSAGE:새 버전이 출시되었습니다. 계속 사용하려면 앱을 업데이트해주세요.}")
    private val message: String,
    private val clock: Clock,
) {
    private val forceUpdateAt = forceUpdateAtValue.trim()
        .takeIf(String::isNotEmpty)
        ?.let(Instant::parse)

    init {
        require(minimumVersionCode >= 1) { "ANDROID_MIN_VERSION_CODE는 1 이상이어야 합니다." }
        require(latestVersionCode >= minimumVersionCode) {
            "ANDROID_LATEST_VERSION_CODE는 ANDROID_MIN_VERSION_CODE 이상이어야 합니다."
        }
    }

    fun androidPolicy(currentVersionCode: Int): AppVersionResponse {
        require(currentVersionCode >= 1) { "currentVersionCode는 1 이상이어야 합니다." }
        val enforcementStarted = forceUpdateAt == null || !clock.instant().isBefore(forceUpdateAt)

        return AppVersionResponse(
            platform = "ANDROID",
            currentVersionCode = currentVersionCode,
            minimumVersionCode = minimumVersionCode,
            latestVersionCode = latestVersionCode,
            updateAvailable = currentVersionCode < latestVersionCode,
            updateRequired = currentVersionCode < minimumVersionCode && enforcementStarted,
            forceUpdateAt = forceUpdateAt,
            storeUrl = storeUrl,
            message = message,
        )
    }
}

@Configuration
class AppVersionConfig {
    @Bean
    fun appVersionService(
        @Value("\${ANDROID_MIN_VERSION_CODE:1}") minimumVersionCode: Int,
        @Value("\${ANDROID_LATEST_VERSION_CODE:2}") latestVersionCode: Int,
        @Value("\${ANDROID_FORCE_UPDATE_AT:}") forceUpdateAtValue: String,
        @Value("\${ANDROID_STORE_URL:https://play.google.com/store/apps/details?id=com.powerfulhimchan.tripplan}") storeUrl: String,
        @Value("\${ANDROID_UPDATE_MESSAGE:새 버전이 출시되었습니다. 계속 사용하려면 앱을 업데이트해주세요.}") message: String,
    ) = AppVersionService(
        minimumVersionCode,
        latestVersionCode,
        forceUpdateAtValue,
        storeUrl,
        message,
        Clock.systemUTC(),
    )
}
