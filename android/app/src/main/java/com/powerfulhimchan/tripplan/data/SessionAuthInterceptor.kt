package com.powerfulhimchan.tripplan.data

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

class SessionExpiredException(private val rejectedToken: String) : IOException(
    "로그인이 만료되었거나 해제되었습니다. 다시 로그인해주세요.",
) {
    fun belongsTo(token: String?) = rejectedToken == token
}

class SessionAuthInterceptor(private val accessToken: () -> String?) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val path = original.url.encodedPath
        val publicRequest = path.startsWith("/api/v1/auth/") ||
            path.startsWith("/api/v1/app-version/")
        val token = if (publicRequest) null else accessToken()?.takeIf { it.isNotBlank() }
        val request = original.newBuilder()
            .removeHeader("Authorization")
            .apply { token?.let { header("Authorization", "Bearer $it") } }
            .build()
        val response = chain.proceed(request)
        if (response.code == 401 && token != null) {
            response.close()
            throw SessionExpiredException(token)
        }
        return response
    }
}
