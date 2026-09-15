package com.powerfulhimchan.tripplan.notification

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import org.slf4j.LoggerFactory

interface PushSender {
    fun send(token: String, title: String, body: String, data: Map<String, String>)
}

class FirebasePushSender(private val messaging: FirebaseMessaging) : PushSender {
    override fun send(token: String, title: String, body: String, data: Map<String, String>) {
        val message = Message.builder()
            .setToken(token)
            .setNotification(Notification.builder().setTitle(title).setBody(body).build())
            .putAllData(data)
            .build()
        messaging.send(message)
    }
}

class LoggingPushSender : PushSender {
    private val log = LoggerFactory.getLogger(javaClass)
    override fun send(token: String, title: String, body: String, data: Map<String, String>) {
        log.info("FCM disabled. token={}, title={}, body={}, data={}", token.take(12), title, body, data)
    }
}

