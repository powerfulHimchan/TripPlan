package com.powerfulhimchan.tripplan.version

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class AppVersionServiceTest {
    @Test
    fun `강제 업데이트 시작 전에는 최소 버전보다 낮아도 사용할 수 있다`() {
        val policy = service(now = "2026-09-30T23:59:59Z").androidPolicy(4)

        assertThat(policy.updateAvailable).isTrue()
        assertThat(policy.updateRequired).isFalse()
    }

    @Test
    fun `강제 업데이트 시작 시각부터 최소 버전 미만을 차단한다`() {
        val policy = service(now = "2026-10-01T00:00:00Z").androidPolicy(4)

        assertThat(policy.updateRequired).isTrue()
        assertThat(policy.minimumVersionCode).isEqualTo(5)
    }

    @Test
    fun `최소 버전은 강제 업데이트 시작 후에도 사용할 수 있다`() {
        assertThat(service(now = "2026-10-02T00:00:00Z").androidPolicy(5).updateRequired).isFalse()
    }

    private fun service(now: String) = AppVersionService(
        minimumVersionCode = 5,
        latestVersionCode = 7,
        forceUpdateAtValue = "2026-10-01T00:00:00Z",
        storeUrl = "https://play.google.com/store/apps/details?id=com.powerfulhimchan.tripplan",
        message = "업데이트가 필요합니다.",
        clock = Clock.fixed(Instant.parse(now), ZoneOffset.UTC),
    )
}
