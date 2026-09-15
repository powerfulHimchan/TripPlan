package com.powerfulhimchan.tripplan.data

import com.powerfulhimchan.tripplan.model.*
import retrofit2.http.*

interface TripApi {
    @POST("api/v1/auth/register")
    suspend fun register(@Body request: EmailPasswordRequest): AuthResponse

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: EmailPasswordRequest): AuthResponse

    @GET("api/v1/trips")
    suspend fun getTrips(): List<Trip>

    @POST("api/v1/trips")
    suspend fun createTrip(@Body request: CreateTripRequest): Trip

    @POST("api/v1/trips/{tripId}/items")
    suspend fun addItem(@Path("tripId") tripId: String, @Body request: CreateItemRequest): ItineraryItem

    @PATCH("api/v1/items/{itemId}/notification")
    suspend fun updateNotification(@Path("itemId") itemId: String, @Body request: UpdateNotificationRequest): ItineraryItem

    @POST("api/v1/devices")
    suspend fun registerDevice(@Body request: RegisterDeviceRequest)

    @GET("api/v1/items/{itemId}/review")
    suspend fun getReview(@Path("itemId") itemId: String): Review?

    @PUT("api/v1/items/{itemId}/review")
    suspend fun saveReview(@Path("itemId") itemId: String, @Body request: SaveReviewRequest): Review

    @POST("api/v1/trips/{tripId}/invitations")
    suspend fun invite(@Path("tripId") tripId: String, @Body request: InviteRequest): Invitation

    @GET("api/v1/invitations")
    suspend fun getInvitations(): List<Invitation>

    @POST("api/v1/invitations/{invitationId}/accept")
    suspend fun acceptInvitation(@Path("invitationId") invitationId: String): Invitation

    @POST("api/v1/invitations/{invitationId}/decline")
    suspend fun declineInvitation(@Path("invitationId") invitationId: String): Invitation

    @GET("api/v1/trips/{tripId}/members")
    suspend fun getMembers(@Path("tripId") tripId: String): List<TripMember>
}
