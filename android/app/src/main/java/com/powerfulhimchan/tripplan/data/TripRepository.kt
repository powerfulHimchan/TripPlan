package com.powerfulhimchan.tripplan.data

import com.powerfulhimchan.tripplan.BuildConfig
import com.powerfulhimchan.tripplan.model.*
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class TripRepository(private val tokenStore: TokenStore) {
    private val api: TripApi = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(OkHttpClient.Builder()
            .addInterceptor { chain ->
                val request = chain.request().newBuilder().apply {
                    tokenStore.accessToken?.let { header("Authorization", "Bearer $it") }
                }.build()
                chain.proceed(request)
            }
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC else HttpLoggingInterceptor.Level.NONE
            }).build())
        .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().add(KotlinJsonAdapterFactory()).build()))
        .build().create(TripApi::class.java)

    val isLoggedIn get() = tokenStore.isLoggedIn
    val email get() = tokenStore.email

    suspend fun appVersion(currentVersionCode: Int) = api.getAndroidVersionPolicy(currentVersionCode)
    suspend fun register(email: String, password: String) = api.register(EmailPasswordRequest(email, password)).also(tokenStore::save)
    suspend fun login(email: String, password: String) = api.login(EmailPasswordRequest(email, password)).also(tokenStore::save)
    fun logout() = tokenStore.clear()

    suspend fun trips() = api.getTrips()
    suspend fun createTrip(request: CreateTripRequest) = api.createTrip(request)
    suspend fun addItem(tripId: String, request: CreateItemRequest) = api.addItem(tripId, request)
    suspend fun updateItem(itemId: String, request: CreateItemRequest) = api.updateItem(itemId, request)
    suspend fun setNotification(item: ItineraryItem, enabled: Boolean) =
        api.updateNotification(item.id, UpdateNotificationRequest(enabled, item.notificationMinutesBefore))
    suspend fun registerDevice(token: String) = api.registerDevice(RegisterDeviceRequest(token))
    suspend fun getReview(itemId: String) = api.getReview(itemId).body()
    suspend fun saveReview(itemId: String, rating: Int, content: String) =
        api.saveReview(itemId, SaveReviewRequest(rating, content))
    suspend fun uploadReviewPhotos(itemId: String, uploads: List<ReviewPhotoUpload>): Review {
        val parts = uploads.map { upload ->
            MultipartBody.Part.createFormData(
                "files",
                upload.fileName,
                upload.bytes.toRequestBody(upload.contentType.toMediaType()),
            )
        }
        return api.uploadReviewPhotos(itemId, parts)
    }
    suspend fun reviewPhoto(itemId: String, photoId: String) = api.getReviewPhoto(itemId, photoId).bytes()
    suspend fun deleteReviewPhoto(itemId: String, photoId: String) = api.deleteReviewPhoto(itemId, photoId)
    suspend fun invite(tripId: String, email: String) = api.invite(tripId, InviteRequest(email))
    suspend fun invitations() = api.getInvitations()
    suspend fun acceptInvitation(id: String) = api.acceptInvitation(id)
    suspend fun declineInvitation(id: String) = api.declineInvitation(id)
    suspend fun members(tripId: String) = api.getMembers(tripId)
}

data class ReviewPhotoUpload(
    val fileName: String,
    val contentType: String,
    val bytes: ByteArray,
)
