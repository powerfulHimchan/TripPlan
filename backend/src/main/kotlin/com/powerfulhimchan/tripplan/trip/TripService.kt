package com.powerfulhimchan.tripplan.trip

import com.powerfulhimchan.tripplan.review.ReviewCleanupService
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@Service
class TripService(
    private val trips: TripRepository,
    private val items: ItineraryItemRepository,
    private val reviewCleanup: ReviewCleanupService,
) {
    @Transactional
    fun create(userId: String, request: CreateTripRequest): TripResponse {
        require(!request.endDate.isBefore(request.startDate)) { "종료일은 시작일보다 빠를 수 없습니다." }
        runCatching { ZoneId.of(request.timezone) }.getOrElse { throw IllegalArgumentException("올바르지 않은 시간대입니다.") }
        return trips.save(
            Trip(userId = userId, title = request.title.trim(), destination = request.destination.trim(),
                startDate = request.startDate, endDate = request.endDate, timezone = request.timezone)
        ).toResponse(emptyList(), userId)
    }

    @Transactional(readOnly = true)
    fun list(userId: String): List<TripResponse> {
        val accessibleTrips = trips.findAllAccessible(userId)
        if (accessibleTrips.isEmpty()) return emptyList()
        val itemsByTripId = items.findAllByTripIdInOrderByScheduledAt(accessibleTrips.map(Trip::id))
            .groupBy(ItineraryItem::tripId)
        return accessibleTrips.map { trip -> trip.toResponse(itemsByTripId[trip.id].orEmpty(), userId) }
    }

    @Transactional(readOnly = true)
    fun get(userId: String, tripId: UUID): TripResponse {
        val trip = accessibleTrip(userId, tripId)
        return trip.toResponse(items.findAllByTripIdOrderByScheduledAt(tripId), userId)
    }

    @Transactional
    fun update(userId: String, tripId: UUID, request: UpdateTripRequest): TripResponse {
        val trip = ownedTrip(userId, tripId)
        validateTripRequest(request.startDate, request.endDate, request.timezone)
        val tripItems = items.findAllByTripIdOrderByScheduledAt(tripId)
        val newZoneId = ZoneId.of(request.timezone)
        require(tripItems.none { item ->
            val itemStart = item.scheduledAt.atZone(newZoneId).toLocalDate()
            val itemEnd = item.endsAt.atZone(newZoneId).toLocalDate()
            itemStart.isBefore(request.startDate) || itemStart.isAfter(request.endDate) ||
                itemEnd.isBefore(request.startDate) || itemEnd.isAfter(request.endDate)
        }) { "변경한 여행 기간 밖에 포함되는 일정이 있습니다. 일정을 먼저 수정해주세요." }
        trip.title = request.title.trim()
        trip.destination = request.destination.trim()
        trip.startDate = request.startDate
        trip.endDate = request.endDate
        trip.timezone = request.timezone
        trip.updatedAt = java.time.Instant.now()
        return trip.toResponse(tripItems, userId)
    }

    @Transactional
    fun addItem(userId: String, tripId: UUID, request: CreateItineraryItemRequest): ItineraryItemResponse {
        val trip = accessibleTrip(userId, tripId)
        val zoneId = ZoneId.of(trip.timezone)
        require(!LocalDate.now(zoneId).isAfter(trip.endDate)) { "종료된 여행에는 일정을 추가할 수 없습니다." }
        require(request.costWon >= 0) { "일정 비용은 0원 이상이어야 합니다." }
        val endsAt = request.endsAt ?: request.scheduledAt.plusSeconds(3600)
        validateItemWindow(trip, request.scheduledAt, endsAt)
        return items.save(ItineraryItem(
            tripId = tripId,
            title = request.title.trim(),
            place = request.place?.trim()?.ifBlank { null },
            memo = request.memo?.trim()?.ifBlank { null },
            category = request.category,
            costWon = request.costWon,
            scheduledAt = request.scheduledAt,
            endsAt = endsAt,
            notificationEnabled = request.notificationEnabled,
            notificationMinutesBefore = request.notificationMinutesBefore,
        )).toResponse()
    }

    @Transactional
    fun updateItem(userId: String, itemId: UUID, request: UpdateItineraryItemRequest): ItineraryItemResponse {
        val item = items.findById(itemId).orElseThrow { EntityNotFoundException("일정을 찾을 수 없습니다.") }
        val trip = accessibleTrip(userId, item.tripId)
        val now = java.time.Instant.now()
        require(now.isBefore(item.scheduledAt)) { "시작된 일정은 수정할 수 없습니다." }
        require(now.isBefore(request.scheduledAt)) { "일정 시작 시각은 현재보다 이후여야 합니다." }
        require(request.costWon >= 0) { "일정 비용은 0원 이상이어야 합니다." }
        validateItemWindow(trip, request.scheduledAt, request.endsAt)

        val notificationScheduleChanged = item.scheduledAt != request.scheduledAt ||
            item.notificationEnabled != request.notificationEnabled ||
            item.notificationMinutesBefore != request.notificationMinutesBefore
        item.title = request.title.trim()
        item.place = request.place?.trim()?.ifBlank { null }
        item.memo = request.memo?.trim()?.ifBlank { null }
        item.category = request.category
        item.costWon = request.costWon
        item.scheduledAt = request.scheduledAt
        item.endsAt = request.endsAt
        item.notificationEnabled = request.notificationEnabled
        item.notificationMinutesBefore = request.notificationMinutesBefore
        if (notificationScheduleChanged) item.notificationSentAt = null
        return item.toResponse()
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
        reviewCleanup.prepareItemDeletion(itemId)
        items.delete(item)
    }

    private fun accessibleTrip(userId: String, tripId: UUID): Trip =
        trips.findAccessible(tripId, userId) ?: throw EntityNotFoundException("여행을 찾을 수 없습니다.")

    private fun ownedTrip(userId: String, tripId: UUID): Trip =
        trips.findByIdAndUserId(tripId, userId) ?: throw EntityNotFoundException("여행을 찾을 수 없습니다.")

    private fun validateTripRequest(startDate: LocalDate, endDate: LocalDate, timezone: String) {
        require(!endDate.isBefore(startDate)) { "종료일은 시작일보다 빠를 수 없습니다." }
        runCatching { ZoneId.of(timezone) }.getOrElse { throw IllegalArgumentException("올바르지 않은 시간대입니다.") }
    }

    private fun validateItemWindow(trip: Trip, scheduledAt: java.time.Instant, endsAt: java.time.Instant) {
        require(endsAt.isAfter(scheduledAt)) { "일정 종료 시각은 시작 시각보다 늦어야 합니다." }
        val zoneId = ZoneId.of(trip.timezone)
        val startDate = scheduledAt.atZone(zoneId).toLocalDate()
        val endDate = endsAt.atZone(zoneId).toLocalDate()
        require(
            !startDate.isBefore(trip.startDate) && !startDate.isAfter(trip.endDate) &&
                !endDate.isBefore(trip.startDate) && !endDate.isAfter(trip.endDate)
        ) { "일정 시작과 종료 시각은 여행 기간 안이어야 합니다." }
    }

    private fun Trip.toResponse(items: List<ItineraryItem>, requesterId: String) = TripResponse(
        id, title, destination, startDate, endDate, timezone, items.map { it.toResponse() },
        owner = userId == requesterId,
    )

    private fun ItineraryItem.toResponse() = ItineraryItemResponse(
        id = id,
        title = title,
        place = place,
        memo = memo,
        scheduledAt = scheduledAt,
        endsAt = endsAt,
        notificationEnabled = notificationEnabled,
        notificationMinutesBefore = notificationMinutesBefore,
        category = category,
        costWon = costWon,
    )
}
