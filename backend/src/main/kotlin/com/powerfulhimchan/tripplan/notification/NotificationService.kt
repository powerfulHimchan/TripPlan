package com.powerfulhimchan.tripplan.notification

import com.powerfulhimchan.tripplan.trip.ItineraryItemRepository
import com.powerfulhimchan.tripplan.trip.TripRepository
import com.powerfulhimchan.tripplan.sharing.TripMemberRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class NotificationService(
    private val items: ItineraryItemRepository,
    private val trips: TripRepository,
    private val tokens: DeviceTokenRepository,
    private val members: TripMemberRepository,
    private val sender: PushSender,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun register(userId: String, token: String) {
        require(token.isNotBlank()) { "FCM token이 비어 있습니다." }
        tokens.save(DeviceToken(token = token, userId = userId))
    }

    @Transactional
    @Scheduled(fixedDelay = 60_000)
    fun sendDueNotifications() {
        val now = Instant.now()
        items.findPotentiallyDue(now.plusSeconds(7 * 24 * 3600)).forEach { item ->
            if (item.notificationDueAt().isAfter(now)) return@forEach
            val trip = trips.findById(item.tripId).orElse(null) ?: return@forEach
            val participantIds = listOf(trip.userId) + members.findAllByTripId(trip.id).map { it.userId }
            val userTokens = participantIds.flatMap(tokens::findAllByUserId)
            if (userTokens.isEmpty()) return@forEach
            val body = item.place?.let { "${item.title} · $it" } ?: item.title
            val success = userTokens.any { device ->
                runCatching {
                    sender.send(device.token, "${trip.title} 일정 알림", body,
                        mapOf("tripId" to trip.id.toString(), "itemId" to item.id.toString()))
                }.onFailure { log.warn("Push 발송 실패: itemId={}", item.id, it) }.isSuccess
            }
            if (success) item.notificationSentAt = now
        }
    }
}
