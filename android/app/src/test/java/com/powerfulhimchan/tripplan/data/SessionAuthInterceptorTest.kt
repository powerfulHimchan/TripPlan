package com.powerfulhimchan.tripplan.data

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SessionAuthInterceptorTest {
    private val server = MockWebServer()
    private var token: String? = "expired-token"
    private val client = OkHttpClient.Builder()
        .addInterceptor(SessionAuthInterceptor { token })
        .build()

    @Before fun start() = server.start()
    @After fun stop() = server.shutdown()

    @Test fun publicVersionRequestDoesNotSendStoredToken() {
        server.enqueue(MockResponse().setResponseCode(200))
        request("/api/v1/app-version/android?currentVersionCode=15").use {
            assertEquals(200, it.code)
        }
        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test fun loginRejectionIsNotTreatedAsExpiredSession() {
        server.enqueue(MockResponse().setResponseCode(401))
        request("/api/v1/auth/login").use { assertEquals(401, it.code) }
        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test fun authenticatedRejectionRequestsRelogin() {
        server.enqueue(MockResponse().setResponseCode(401))
        val failure = assertThrows(SessionExpiredException::class.java) {
            request("/api/v1/trips").close()
        }
        assertTrue(failure.belongsTo("expired-token"))
        assertFalse(failure.belongsTo("new-token"))
        assertFalse(failure.belongsTo(null))
        assertEquals("Bearer expired-token", server.takeRequest().getHeader("Authorization"))
        assertFalse(failure.message.orEmpty().contains("expired-token"))
    }

    @Test fun missingTokenDoesNotSignalExpiredSession() {
        token = null
        server.enqueue(MockResponse().setResponseCode(401))
        request("/api/v1/trips").use { assertEquals(401, it.code) }
        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test fun permissionAndServerErrorsDoNotExpireSession() {
        listOf(403, 500, 503).forEach { code ->
            server.enqueue(MockResponse().setResponseCode(code))
            request("/api/v1/trips").use { assertEquals(code, it.code) }
            assertEquals("Bearer expired-token", server.takeRequest().getHeader("Authorization"))
        }
    }

    @Test fun successfulAuthenticatedRequestKeepsToken() {
        token = "new-token"
        server.enqueue(MockResponse().setResponseCode(200))
        request("/api/v1/trips").use { assertEquals(200, it.code) }
        assertEquals("Bearer new-token", server.takeRequest().getHeader("Authorization"))
    }

    private fun request(path: String) = client.newCall(
        Request.Builder().url(server.url(path)).build(),
    ).execute()
}
