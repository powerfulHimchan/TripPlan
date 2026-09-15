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
data class ReviewPhoto(
    val id: String,
    val originalName: String,
    val contentType: String,
    val sizeBytes: Long,
)
data class Review(
    val id: String,
    val itemId: String,
    val rating: Int,
    val content: String,
    val updatedAt: String,
    val photos: List<ReviewPhoto> = emptyList(),
)

data class EmailPasswordRequest(val email: String, val password: String)
data class AuthResponse(val accessToken: String, val userId: String, val email: String)
data class InviteRequest(val email: String)
data class Invitation(
    val id: String,
    val tripId: String,
    val tripTitle: String,
    val inviterEmail: String,
    val inviteeEmail: String,
    val status: String,
    val createdAt: String,
)
data class TripMember(val userId: String, val email: String, val owner: Boolean)
