package com.powerfulhimchan.tripplan.data

import com.powerfulhimchan.tripplan.model.*
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

interface TripApi {
    @GET("api/v1/app-version/android")
    suspend fun getAndroidVersionPolicy(
        @Query("currentVersionCode") currentVersionCode: Int,
    ): AppVersionResponse

    @POST("api/v1/auth/register")
    suspend fun register(@Body request: EmailPasswordRequest): AuthResponse

    @POST("api/v1/auth/login")
    suspend fun login(@Body request: EmailPasswordRequest): AuthResponse

    @GET("api/v1/trips")
    suspend fun getTrips(): List<Trip>

    @GET("api/v1/trips/{tripId}/details")
    suspend fun getTripDetail(@Path("tripId") tripId: String): TripDetail

    @POST("api/v1/trips")
    suspend fun createTrip(@Body request: CreateTripRequest): Trip

    @PATCH("api/v1/trips/{tripId}")
    suspend fun updateTrip(@Path("tripId") tripId: String, @Body request: CreateTripRequest): Trip

    @POST("api/v1/trips/{tripId}/items")
    suspend fun addItem(@Path("tripId") tripId: String, @Body request: CreateItemRequest): ItineraryItem

    @PUT("api/v1/items/{itemId}")
    suspend fun updateItem(@Path("itemId") itemId: String, @Body request: CreateItemRequest): ItineraryItem

    @PATCH("api/v1/items/{itemId}/notification")
    suspend fun updateNotification(@Path("itemId") itemId: String, @Body request: UpdateNotificationRequest): ItineraryItem

    @POST("api/v1/devices")
    suspend fun registerDevice(@Body request: RegisterDeviceRequest)

    @GET("api/v1/items/{itemId}/review")
    suspend fun getReview(@Path("itemId") itemId: String): Response<Review>

    @PUT("api/v1/items/{itemId}/review")
    suspend fun saveReview(@Path("itemId") itemId: String, @Body request: SaveReviewRequest): Review

    @Multipart
    @POST("api/v1/items/{itemId}/review/photos")
    suspend fun uploadReviewPhotos(
        @Path("itemId") itemId: String,
        @Part files: List<MultipartBody.Part>,
    ): Review

    @Streaming
    @GET("api/v1/items/{itemId}/review/photos/{photoId}/content")
    suspend fun getReviewPhoto(
        @Path("itemId") itemId: String,
        @Path("photoId") photoId: String,
    ): ResponseBody

    @DELETE("api/v1/items/{itemId}/review/photos/{photoId}")
    suspend fun deleteReviewPhoto(
        @Path("itemId") itemId: String,
        @Path("photoId") photoId: String,
    ): Review

    @GET("api/v1/trips/{tripId}/review")
    suspend fun getTripOverallReview(@Path("tripId") tripId: String): Response<TripOverallReview>

    @GET("api/v1/trips/overall-reviews")
    suspend fun getTripOverallReviews(): List<TripOverallReview>

    @PUT("api/v1/trips/{tripId}/review")
    suspend fun saveTripOverallReview(
        @Path("tripId") tripId: String,
        @Body request: SaveTripOverallReviewRequest,
    ): TripOverallReview

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
