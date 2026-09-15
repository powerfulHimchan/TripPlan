package com.powerfulhimchan.tripplan.trip

import jakarta.validation.constraints.*
import java.time.*
import java.util.UUID

data class CreateTripRequest(
    @field:NotBlank @field:Size(max = 120) val title: String,
    @field:NotBlank @field:Size(max = 120) val destination: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    @field:NotBlank val timezone: String = "Asia/Seoul",
)

data class CreateItineraryItemRequest(
    @field:NotBlank @field:Size(max = 120) val title: String,
    @field:Size(max = 200) val place: String? = null,
    val memo: String? = null,
    val scheduledAt: Instant,
    val notificationEnabled: Boolean = true,
    @field:Min(0) @field:Max(10080) val notificationMinutesBefore: Int = 0,
)

data class UpdateNotificationRequest(
    val enabled: Boolean,
    @field:Min(0) @field:Max(10080) val minutesBefore: Int = 0,
)

data class ItineraryItemResponse(
    val id: UUID,
    val title: String,
    val place: String?,
    val memo: String?,
    val scheduledAt: Instant,
    val notificationEnabled: Boolean,
    val notificationMinutesBefore: Int,
)

data class TripResponse(
    val id: UUID,
    val title: String,
    val destination: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val timezone: String,
    val items: List<ItineraryItemResponse>,
)

