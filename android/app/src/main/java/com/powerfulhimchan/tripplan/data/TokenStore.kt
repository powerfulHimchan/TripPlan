package com.powerfulhimchan.tripplan.data

import android.content.Context
import com.powerfulhimchan.tripplan.model.AuthResponse

class TokenStore(context: Context) {
    private val preferences = context.getSharedPreferences("tripplan_auth", Context.MODE_PRIVATE)

    val accessToken: String? get() = preferences.getString("access_token", null)
    val email: String? get() = preferences.getString("email", null)
    val isLoggedIn: Boolean get() = !accessToken.isNullOrBlank()

    fun save(auth: AuthResponse) {
        preferences.edit()
            .putString("access_token", auth.accessToken)
            .putString("email", auth.email)
            .apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }
}
