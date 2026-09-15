package com.powerfulhimchan.tripplan.ui

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.powerfulhimchan.tripplan.data.TokenStore
import com.powerfulhimchan.tripplan.data.ReviewPhotoUpload
import com.powerfulhimchan.tripplan.data.TripRepository
import com.powerfulhimchan.tripplan.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

data class TripUiState(
    val authenticated: Boolean = false,
    val email: String? = null,
    val trips: List<Trip> = emptyList(),
    val selected: Trip? = null,
    val reviews: Map<String, Review> = emptyMap(),
    val reviewPhotoBytes: Map<String, ByteArray> = emptyMap(),
    val invitations: List<Invitation> = emptyList(),
    val members: List<TripMember> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
)

class TripViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TripRepository(TokenStore(application))
    private val _state = MutableStateFlow(
        TripUiState(authenticated = repository.isLoggedIn, email = repository.email)
    )
    val state = _state.asStateFlow()
    private var deviceToken: String? = null

    init {
        if (repository.isLoggedIn) refresh()
    }

    fun login(email: String, password: String) = authenticate { repository.login(email, password) }

    fun register(email: String, password: String) = authenticate { repository.register(email, password) }

    fun logout() {
        repository.logout()
        _state.value = TripUiState()
    }

    fun refresh() = launch {
        _state.value.copy(
            trips = repository.trips(),
            invitations = repository.invitations(),
            authenticated = true,
            email = repository.email,
            error = null,
        )
    }

    fun select(trip: Trip?) {
        _state.value = _state.value.copy(
            selected = trip,
            reviews = emptyMap(),
            reviewPhotoBytes = emptyMap(),
            members = emptyList(),
        )
        if (trip != null) launch {
            val reviews = trip.items.mapNotNull { item ->
                repository.getReview(item.id)?.let { item.id to it }
            }.toMap()
            _state.value.copy(
                reviews = reviews,
                reviewPhotoBytes = loadPhotoBytes(reviews),
                members = repository.members(trip.id),
            )
        }
    }

    fun createTrip(request: CreateTripRequest) = launch {
        repository.createTrip(request)
        _state.value.copy(trips = repository.trips())
    }

    fun addItem(request: CreateItemRequest) {
        val trip = _state.value.selected ?: return
        launch {
            repository.addItem(trip.id, request)
            val trips = repository.trips()
            _state.value.copy(trips = trips, selected = trips.first { it.id == trip.id })
        }
    }

    fun toggleNotification(item: ItineraryItem, enabled: Boolean) {
        val trip = _state.value.selected ?: return
        launch {
            repository.setNotification(item, enabled)
            val trips = repository.trips()
            _state.value.copy(trips = trips, selected = trips.first { it.id == trip.id })
        }
    }

    fun saveReview(itemId: String, rating: Int, content: String, photoUris: List<Uri>) = launch {
        var review = repository.saveReview(itemId, rating, content)
        if (photoUris.isNotEmpty()) {
            val remaining = MAX_PHOTO_COUNT - review.photos.size
            require(photoUris.size <= remaining) { "후기 사진은 최대 5장까지 등록할 수 있습니다." }
            review = repository.uploadReviewPhotos(itemId, photoUris.map(::readPhoto))
        }
        val bytes = review.photos.associate { it.id to repository.reviewPhoto(itemId, it.id) }
        _state.value.copy(
            reviews = _state.value.reviews + (itemId to review),
            reviewPhotoBytes = _state.value.reviewPhotoBytes + bytes,
        )
    }

    fun deleteReviewPhoto(itemId: String, photoId: String) = launch {
        val review = repository.deleteReviewPhoto(itemId, photoId)
        _state.value.copy(
            reviews = _state.value.reviews + (itemId to review),
            reviewPhotoBytes = _state.value.reviewPhotoBytes - photoId,
        )
    }

    fun invite(email: String) {
        val trip = _state.value.selected ?: return
        launch {
            repository.invite(trip.id, email)
            _state.value.copy(members = repository.members(trip.id))
        }
    }

    fun acceptInvitation(id: String) = launch {
        repository.acceptInvitation(id)
        _state.value.copy(trips = repository.trips(), invitations = repository.invitations())
    }

    fun declineInvitation(id: String) = launch {
        repository.declineInvitation(id)
        _state.value.copy(invitations = repository.invitations())
    }

    fun registerDevice(token: String) {
        deviceToken = token
        if (_state.value.authenticated) launch { repository.registerDevice(token); _state.value }
    }

    private fun authenticate(request: suspend () -> AuthResponse) = launch {
        request()
        deviceToken?.let { repository.registerDevice(it) }
        _state.value.copy(
            authenticated = true,
            email = repository.email,
            trips = repository.trips(),
            invitations = repository.invitations(),
        )
    }

    private suspend fun loadPhotoBytes(reviews: Map<String, Review>): Map<String, ByteArray> = buildMap {
        reviews.forEach { (itemId, review) ->
            review.photos.forEach { photo -> put(photo.id, repository.reviewPhoto(itemId, photo.id)) }
        }
    }

    private fun readPhoto(uri: Uri): ReviewPhotoUpload {
        val resolver = getApplication<Application>().contentResolver
        val contentType = resolver.getType(uri) ?: "application/octet-stream"
        require(contentType.startsWith("image/")) { "이미지 파일만 등록할 수 있습니다." }
        val fileName = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        } ?: "photo"
        val bytes = resolver.openInputStream(uri)?.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                require(total <= MAX_PHOTO_SIZE) { "사진 한 장은 최대 5MB까지 등록할 수 있습니다." }
                output.write(buffer, 0, read)
            }
            output.toByteArray()
        } ?: throw IllegalArgumentException("사진 파일을 읽을 수 없습니다.")
        require(bytes.isNotEmpty()) { "빈 사진 파일은 등록할 수 없습니다." }
        return ReviewPhotoUpload(fileName, contentType, bytes)
    }

    private fun launch(block: suspend () -> TripUiState) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            _state.value = runCatching { block() }
                .getOrElse { _state.value.copy(error = it.message ?: "요청에 실패했습니다.") }
                .copy(loading = false)
        }
    }

    companion object {
        private const val MAX_PHOTO_COUNT = 5
        private const val MAX_PHOTO_SIZE = 5 * 1024 * 1024
    }
}
