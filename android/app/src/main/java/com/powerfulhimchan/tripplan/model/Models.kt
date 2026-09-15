package com.powerfulhimchan.tripplan.model

data class Trip(
    val id: String,
    val title: String,
    val destination: String,
    val startDate: String,
    val endDate: String,
    val timezone: String,
    val items: List<ItineraryItem> = emptyList(),
)

data class ItineraryItem(
    val id: String,
    val title: String,
    val place: String?,
    val memo: String?,
    val scheduledAt: String,
    val notificationEnabled: Boolean,
    val notificationMinutesBefore: Int,
)

data class CreateTripRequest(
    val title: String,
    val destination: String,
    val startDate: String,
    val endDate: String,
    val timezone: String = "Asia/Seoul",
)

data class CreateItemRequest(
    val title: String,
    val place: String?,
    val memo: String?,
    val scheduledAt: String,
    val notificationEnabled: Boolean = true,
    val notificationMinutesBefore: Int = 30,
)

data class UpdateNotificationRequest(val enabled: Boolean, val minutesBefore: Int)
data class RegisterDeviceRequest(val token: String)
data class SaveReviewRequest(val rating: Int, val content: String)
data class Review(val id: String, val itemId: String, val rating: Int, val content: String, val updatedAt: String)
