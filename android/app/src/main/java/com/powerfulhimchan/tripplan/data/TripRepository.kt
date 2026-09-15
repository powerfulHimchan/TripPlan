package com.powerfulhimchan.tripplan.data

import com.powerfulhimchan.tripplan.BuildConfig
import com.powerfulhimchan.tripplan.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class TripRepository {
    private val api: TripApi = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(OkHttpClient.Builder().addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
        }).build())
        .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().add(KotlinJsonAdapterFactory()).build()))
        .build().create(TripApi::class.java)

    private val userId = "demo-user"

    suspend fun trips() = api.getTrips(userId)
    suspend fun createTrip(request: CreateTripRequest) = api.createTrip(userId, request)
    suspend fun addItem(tripId: String, request: CreateItemRequest) = api.addItem(userId, tripId, request)
    suspend fun setNotification(item: ItineraryItem, enabled: Boolean) =
        api.updateNotification(userId, item.id, UpdateNotificationRequest(enabled, item.notificationMinutesBefore))
    suspend fun registerDevice(token: String) = api.registerDevice(userId, RegisterDeviceRequest(token))
    suspend fun getReview(tripId: String) = api.getReview(userId, tripId)
    suspend fun saveReview(tripId: String, rating: Int, content: String) =
        api.saveReview(userId, tripId, SaveReviewRequest(rating, content))
}

