package com.powerfulhimchan.tripplan.data

import com.powerfulhimchan.tripplan.model.*
import retrofit2.http.*

interface TripApi {
    @GET("api/v1/trips")
    suspend fun getTrips(@Header("X-User-Id") userId: String): List<Trip>

    @POST("api/v1/trips")
    suspend fun createTrip(@Header("X-User-Id") userId: String, @Body request: CreateTripRequest): Trip

    @POST("api/v1/trips/{tripId}/items")
    suspend fun addItem(@Header("X-User-Id") userId: String, @Path("tripId") tripId: String, @Body request: CreateItemRequest): ItineraryItem

    @PATCH("api/v1/items/{itemId}/notification")
    suspend fun updateNotification(@Header("X-User-Id") userId: String, @Path("itemId") itemId: String, @Body request: UpdateNotificationRequest): ItineraryItem

    @POST("api/v1/devices")
    suspend fun registerDevice(@Header("X-User-Id") userId: String, @Body request: RegisterDeviceRequest)

    @GET("api/v1/items/{itemId}/review")
    suspend fun getReview(@Header("X-User-Id") userId: String, @Path("itemId") itemId: String): Review?

    @PUT("api/v1/items/{itemId}/review")
    suspend fun saveReview(@Header("X-User-Id") userId: String, @Path("itemId") itemId: String, @Body request: SaveReviewRequest): Review
}
