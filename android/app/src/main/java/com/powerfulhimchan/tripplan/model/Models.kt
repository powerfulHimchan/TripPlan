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
    val endsAt: String,
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
    val endsAt: String,
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

data class RepresentativePhoto(
    val id: String,
    val itemId: String,
    val originalName: String,
    val contentType: String,
    val sizeBytes: Long,
)

data class TripOverallReview(
    val id: String,
    val tripId: String,
    val rating: Int,
    val content: String,
    val representativePhoto: RepresentativePhoto?,
    val updatedAt: String,
)

data class SaveTripOverallReviewRequest(
    val rating: Int,
    val content: String,
    val representativePhotoId: String?,
)

data class GoogleCalendar(
    val id: Long,
    val name: String,
    val accountName: String,
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

data class TripDetail(
    val trip: Trip,
    val reviews: List<Review> = emptyList(),
    val overallReview: TripOverallReview?,
    val members: List<TripMember> = emptyList(),
)

data class AppVersionResponse(
    val platform: String,
    val currentVersionCode: Int,
    val minimumVersionCode: Int,
    val latestVersionCode: Int,
    val updateAvailable: Boolean,
    val updateRequired: Boolean,
    val forceUpdateAt: String?,
    val storeUrl: String,
    val message: String,
)
