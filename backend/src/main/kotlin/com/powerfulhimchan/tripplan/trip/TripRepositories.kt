package com.powerfulhimchan.tripplan.trip

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.UUID

interface TripRepository : JpaRepository<Trip, UUID> {
    fun findAllByUserIdOrderByStartDateDesc(userId: String): List<Trip>
    fun findByIdAndUserId(id: UUID, userId: String): Trip?
}

interface ItineraryItemRepository : JpaRepository<ItineraryItem, UUID> {
    fun findAllByTripIdOrderByScheduledAt(tripId: UUID): List<ItineraryItem>

    @Query("""
        select i from ItineraryItem i
        where i.notificationEnabled = true
          and i.notificationSentAt is null
          and i.scheduledAt <= :latestScheduledAt
    """)
    fun findPotentiallyDue(latestScheduledAt: Instant): List<ItineraryItem>
}

