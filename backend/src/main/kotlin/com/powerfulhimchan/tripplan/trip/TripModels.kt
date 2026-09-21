package com.powerfulhimchan.tripplan.trip

import jakarta.persistence.*
import java.time.*
import java.util.UUID

enum class ItineraryCategory {
    ACCOMMODATION,
    TRANSPORTATION,
    SIGHTSEEING,
    FOOD,
    CAFE,
    ACTIVITY,
    SHOPPING,
    CULTURE,
    REST,
    OTHER,
}

@Entity
@Table(name = "trips")
class Trip(
    @Id val id: UUID = UUID.randomUUID(),
    @Column(nullable = false) val userId: String,
    @Column(nullable = false, length = 120) var title: String,
    @Column(nullable = false, length = 120) var destination: String,
    @Column(nullable = false) var startDate: LocalDate,
    @Column(nullable = false) var endDate: LocalDate,
    @Column(nullable = false, length = 60) var timezone: String,
    @Column(nullable = false) val createdAt: Instant = Instant.now(),
    @Column(nullable = false) var updatedAt: Instant = Instant.now(),
) {
    protected constructor() : this(userId = "", title = "", destination = "", startDate = LocalDate.now(), endDate = LocalDate.now(), timezone = "UTC")
}

@Entity
@Table(name = "itinerary_items")
class ItineraryItem(
    @Id val id: UUID = UUID.randomUUID(),
    @Column(nullable = false) val tripId: UUID,
    @Column(nullable = false, length = 120) var title: String,
    @Column(length = 200) var place: String? = null,
    @Column(columnDefinition = "TEXT") var memo: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var category: ItineraryCategory = ItineraryCategory.OTHER,
    @Column(nullable = false) var scheduledAt: Instant,
    @Column(nullable = false) var endsAt: Instant = scheduledAt.plusSeconds(3600),
    @Column(nullable = false) var notificationEnabled: Boolean = true,
    @Column(nullable = false) var notificationMinutesBefore: Int = 0,
    var notificationSentAt: Instant? = null,
    @Column(nullable = false) val createdAt: Instant = Instant.now(),
) {
    protected constructor() : this(tripId = UUID.randomUUID(), title = "", scheduledAt = Instant.now())

    fun notificationDueAt(): Instant = scheduledAt.minusSeconds(notificationMinutesBefore * 60L)
}
