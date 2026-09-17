package com.powerfulhimchan.tripplan.trip

import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.ZoneId
import java.util.UUID

@Service
class TripService(
    private val trips: TripRepository,
    private val items: ItineraryItemRepository,
) {
    @Transactional
    fun create(userId: String, request: CreateTripRequest): TripResponse {
        require(!request.endDate.isBefore(request.startDate)) { "종료일은 시작일보다 빠를 수 없습니다." }
        runCatching { ZoneId.of(request.timezone) }.getOrElse { throw IllegalArgumentException("올바르지 않은 시간대입니다.") }
        return trips.save(
            Trip(userId = userId, title = request.title.trim(), destination = request.destination.trim(),
                startDate = request.startDate, endDate = request.endDate, timezone = request.timezone)
        ).toResponse(emptyList())
    }

    @Transactional(readOnly = true)
    fun list(userId: String): List<TripResponse> =
        trips.findAllAccessible(userId).map { trip ->
            trip.toResponse(items.findAllByTripIdOrderByScheduledAt(trip.id))
        }

    @Transactional(readOnly = true)
    fun get(userId: String, tripId: UUID): TripResponse {
        val trip = accessibleTrip(userId, tripId)
        return trip.toResponse(items.findAllByTripIdOrderByScheduledAt(tripId))
    }

    @Transactional
    fun addItem(userId: String, tripId: UUID, request: CreateItineraryItemRequest): ItineraryItemResponse {
        val trip = accessibleTrip(userId, tripId)
        val endsAt = request.endsAt ?: request.scheduledAt.plusSeconds(3600)
        require(endsAt.isAfter(request.scheduledAt)) { "일정 종료 시각은 시작 시각보다 늦어야 합니다." }
        val zoneId = ZoneId.of(trip.timezone)
        val startDate = request.scheduledAt.atZone(zoneId).toLocalDate()
        val endDate = endsAt.atZone(zoneId).toLocalDate()
        require(
            !startDate.isBefore(trip.startDate) && !startDate.isAfter(trip.endDate) &&
                !endDate.isBefore(trip.startDate) && !endDate.isAfter(trip.endDate)
        ) { "일정 시작과 종료 시각은 여행 기간 안이어야 합니다." }
        return items.save(ItineraryItem(
            tripId = tripId,
            title = request.title.trim(),
            place = request.place?.trim()?.ifBlank { null },
            memo = request.memo?.trim()?.ifBlank { null },
            scheduledAt = request.scheduledAt,
            endsAt = endsAt,
            notificationEnabled = request.notificationEnabled,
            notificationMinutesBefore = request.notificationMinutesBefore,
        )).toResponse()
    }

    @Transactional
    fun updateNotification(userId: String, itemId: UUID, request: UpdateNotificationRequest): ItineraryItemResponse {
        val item = items.findById(itemId).orElseThrow { EntityNotFoundException("일정을 찾을 수 없습니다.") }
        accessibleTrip(userId, item.tripId)
        item.notificationEnabled = request.enabled
        item.notificationMinutesBefore = request.minutesBefore
        item.notificationSentAt = null
        return item.toResponse()
    }

    @Transactional
    fun deleteItem(userId: String, itemId: UUID) {
        val item = items.findById(itemId).orElseThrow { EntityNotFoundException("일정을 찾을 수 없습니다.") }
        accessibleTrip(userId, item.tripId)
        items.delete(item)
    }

    private fun accessibleTrip(userId: String, tripId: UUID): Trip =
        trips.findAccessible(tripId, userId) ?: throw EntityNotFoundException("여행을 찾을 수 없습니다.")

    private fun Trip.toResponse(items: List<ItineraryItem>) = TripResponse(
        id, title, destination, startDate, endDate, timezone, items.map { it.toResponse() }
    )

    private fun ItineraryItem.toResponse() = ItineraryItemResponse(
        id, title, place, memo, scheduledAt, endsAt, notificationEnabled, notificationMinutesBefore
    )
}
