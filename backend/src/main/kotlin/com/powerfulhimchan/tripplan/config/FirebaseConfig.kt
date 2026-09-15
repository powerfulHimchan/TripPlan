package com.powerfulhimchan.tripplan.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.powerfulhimchan.tripplan.notification.FirebasePushSender
import com.powerfulhimchan.tripplan.notification.LoggingPushSender
import com.powerfulhimchan.tripplan.notification.PushSender
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class FirebaseConfig {
    @Bean
    fun pushSender(@Value("\${tripplan.notification.fcm-enabled:false}") enabled: Boolean): PushSender {
        if (!enabled) return LoggingPushSender()
        val app = FirebaseApp.getApps().firstOrNull() ?: FirebaseApp.initializeApp(
            FirebaseOptions.builder().setCredentials(GoogleCredentials.getApplicationDefault()).build()
        )
        return FirebasePushSender(FirebaseMessaging.getInstance(app))
    }
}

