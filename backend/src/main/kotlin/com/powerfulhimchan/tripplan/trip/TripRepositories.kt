package com.powerfulhimchan.tripplan.trip

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.util.UUID

interface TripRepository : JpaRepository<Trip, UUID> {
    fun findByIdAndUserId(id: UUID, userId: String): Trip?

    @Query("""
        select distinct t from Trip t
        where t.userId = :userId
           or exists (
               select m.id from TripMember m
               where m.tripId = t.id and m.userId = :userId
           )
        order by t.startDate desc
    """)
    fun findAllAccessible(@Param("userId") userId: String): List<Trip>

    @Query("""
        select t from Trip t
        where t.id = :tripId
          and (t.userId = :userId or exists (
              select m.id from TripMember m
              where m.tripId = t.id and m.userId = :userId
          ))
    """)
    fun findAccessible(@Param("tripId") tripId: UUID, @Param("userId") userId: String): Trip?
}

interface ItineraryItemRepository : JpaRepository<ItineraryItem, UUID> {
    fun findAllByTripIdOrderByScheduledAt(tripId: UUID): List<ItineraryItem>
    fun findAllByTripIdInOrderByScheduledAt(tripIds: Collection<UUID>): List<ItineraryItem>

    @Query("""
        select i from ItineraryItem i
        where i.notificationEnabled = true
          and i.notificationSentAt is null
          and i.scheduledAt <= :latestScheduledAt
    """)
    fun findPotentiallyDue(latestScheduledAt: Instant): List<ItineraryItem>
}
