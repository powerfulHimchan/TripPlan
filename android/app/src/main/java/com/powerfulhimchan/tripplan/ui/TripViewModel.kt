package com.powerfulhimchan.tripplan.ui

import android.app.Application
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.powerfulhimchan.tripplan.BuildConfig
import com.powerfulhimchan.tripplan.data.TokenStore
import com.powerfulhimchan.tripplan.data.ReviewPhotoUpload
import com.powerfulhimchan.tripplan.data.TripRepository
import com.powerfulhimchan.tripplan.data.GoogleCalendarExporter
import com.powerfulhimchan.tripplan.model.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

data class TripUiState(
    val versionChecking: Boolean = false,
    val requiredUpdate: AppVersionResponse? = null,
    val authenticated: Boolean = false,
    val email: String? = null,
    val trips: List<Trip> = emptyList(),
    val selected: Trip? = null,
    val reviews: Map<String, Review> = emptyMap(),
    val reviewPhotoBytes: Map<String, ByteArray> = emptyMap(),
    val overallReviews: Map<String, TripOverallReview> = emptyMap(),
    val overallReviewPhotoBytes: Map<String, ByteArray> = emptyMap(),
    val googleCalendars: List<GoogleCalendar> = emptyList(),
    val calendarExportMessage: String? = null,
    val invitations: List<Invitation> = emptyList(),
    val members: List<TripMember> = emptyList(),
    val detailLoading: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null,
)

class TripViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TripRepository(TokenStore(application))
    private val calendarExporter = GoogleCalendarExporter(application)
    private val _state = MutableStateFlow(
        TripUiState(
            versionChecking = true,
            authenticated = repository.isLoggedIn,
            email = repository.email,
        )
    )
    val state = _state.asStateFlow()
    private var deviceToken: String? = null
    private val detailCache = mutableMapOf<String, TripDetail>()
    private val photoIdsInFlight = mutableSetOf<String>()

    init {
        checkAppVersion()
    }

    fun login(email: String, password: String) = authenticate { repository.login(email, password) }

    fun register(email: String, password: String) = authenticate { repository.register(email, password) }

    fun logout() {
        repository.logout()
        detailCache.clear()
        photoIdsInFlight.clear()
        _state.value = TripUiState()
    }

    private fun checkAppVersion() {
        viewModelScope.launch {
            val policy = runCatching { repository.appVersion(BuildConfig.VERSION_CODE) }.getOrNull()
            _state.value = _state.value.copy(
                versionChecking = false,
                requiredUpdate = policy?.takeIf { it.updateRequired },
            )
            if (policy?.updateRequired != true && repository.isLoggedIn) refresh()
        }
    }

    fun refresh() = launch {
        val trips = repository.trips()
        val overallReviews = loadOverallReviews(trips)
        val retainedPhotoBytes = _state.value.overallReviewPhotoBytes.filterKeys(overallReviews::containsKey)
        val missingPhotoReviews = overallReviews.filterKeys { it !in retainedPhotoBytes }
        _state.value.copy(
            trips = trips,
            overallReviews = overallReviews,
            overallReviewPhotoBytes = retainedPhotoBytes + loadOverallReviewPhotoBytes(missingPhotoReviews),
            invitations = repository.invitations(),
            authenticated = true,
            email = repository.email,
            error = null,
        )
    }

    fun select(trip: Trip?) {
        if (trip == null) {
            _state.update {
                it.copy(selected = null, reviews = emptyMap(), members = emptyList(), detailLoading = false)
            }
            return
        }

        val cached = detailCache[trip.id]
        _state.update { current ->
            val overallReviews = when {
                cached == null -> current.overallReviews
                else -> cached.overallReview
                    ?.let { current.overallReviews + (trip.id to it) }
                    ?: (current.overallReviews - trip.id)
            }
            current.copy(
                selected = trip,
                reviews = cached?.reviews.orEmpty().associateBy(Review::itemId),
                members = cached?.members.orEmpty(),
                overallReviews = overallReviews,
                detailLoading = cached == null,
                error = null,
            )
        }
        if (cached != null) {
            loadReviewPhotosInBackground(cached)
            return
        }

        viewModelScope.launch {
            runCatching { repository.tripDetail(trip.id) }
                .onSuccess { detail ->
                    detailCache[trip.id] = detail
                    _state.update { current ->
                        if (current.selected?.id != trip.id) current
                        else {
                            val overallReviews = detail.overallReview
                                ?.let { current.overallReviews + (trip.id to it) }
                                ?: (current.overallReviews - trip.id)
                            current.copy(
                                selected = detail.trip,
                                trips = current.trips.map { if (it.id == trip.id) detail.trip else it },
                                reviews = detail.reviews.associateBy(Review::itemId),
                                overallReviews = overallReviews,
                                members = detail.members,
                                detailLoading = false,
                                error = null,
                            )
                        }
                    }
                    loadReviewPhotosInBackground(detail)
                }
                .onFailure { failure ->
                    _state.update { current ->
                        if (current.selected?.id != trip.id) current
                        else current.copy(
                            detailLoading = false,
                            error = failure.message ?: "여행 상세 정보를 불러오지 못했습니다.",
                        )
                    }
                }
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
            detailCache.remove(trip.id)
            val trips = repository.trips()
            _state.value.copy(trips = trips, selected = trips.first { it.id == trip.id })
        }
    }

    fun updateItem(itemId: String, request: CreateItemRequest) {
        val trip = _state.value.selected ?: return
        launch {
            repository.updateItem(itemId, request)
            detailCache.remove(trip.id)
            val trips = repository.trips()
            _state.value.copy(trips = trips, selected = trips.first { it.id == trip.id })
        }
    }

    fun toggleNotification(item: ItineraryItem, enabled: Boolean) {
        val trip = _state.value.selected ?: return
        launch {
            repository.setNotification(item, enabled)
            detailCache.remove(trip.id)
            val trips = repository.trips()
            _state.value.copy(trips = trips, selected = trips.first { it.id == trip.id })
        }
    }

    fun saveReview(
        itemId: String,
        rating: Int,
        content: String,
        photoUris: List<Uri>,
        removedPhotoIds: Set<String>,
    ) = launch {
        var review = repository.saveReview(itemId, rating, content)
        removedPhotoIds.forEach { photoId ->
            review = repository.deleteReviewPhoto(itemId, photoId)
        }
        if (photoUris.isNotEmpty()) {
            val remaining = MAX_PHOTO_COUNT - review.photos.size
            require(photoUris.size <= remaining) { "후기 사진은 최대 5장까지 등록할 수 있습니다." }
            review = repository.uploadReviewPhotos(itemId, photoUris.map(::readPhoto))
        }
        val bytes = review.photos.associate { it.id to repository.reviewPhoto(itemId, it.id) }
        val trip = _state.value.selected
        val refreshedOverallReview = trip?.let { repository.getTripOverallReview(it.id) }
        val overallReviews = if (trip != null) {
            if (refreshedOverallReview != null) _state.value.overallReviews + (trip.id to refreshedOverallReview)
            else _state.value.overallReviews - trip.id
        } else _state.value.overallReviews
        val overallPhotoBytes = if (trip != null) {
            val withoutOldCover = _state.value.overallReviewPhotoBytes - trip.id
            if (refreshedOverallReview != null) withoutOldCover + loadOverallReviewPhotoBytes(mapOf(trip.id to refreshedOverallReview))
            else withoutOldCover
        } else _state.value.overallReviewPhotoBytes
        if (trip != null) {
            detailCache[trip.id]?.let { cached ->
                detailCache[trip.id] = cached.copy(
                    reviews = cached.reviews.filterNot { it.itemId == itemId } + review,
                    overallReview = refreshedOverallReview,
                )
            }
        }
        _state.value.copy(
            reviews = _state.value.reviews + (itemId to review),
            reviewPhotoBytes = (_state.value.reviewPhotoBytes - removedPhotoIds) + bytes,
            overallReviews = overallReviews,
            overallReviewPhotoBytes = overallPhotoBytes,
        )
    }

    fun deleteReviewPhoto(itemId: String, photoId: String) = launch {
        val review = repository.deleteReviewPhoto(itemId, photoId)
        _state.value.selected?.let { trip ->
            detailCache[trip.id]?.let { cached ->
                detailCache[trip.id] = cached.copy(
                    reviews = cached.reviews.filterNot { it.itemId == itemId } + review,
                )
            }
        }
        _state.value.copy(
            reviews = _state.value.reviews + (itemId to review),
            reviewPhotoBytes = _state.value.reviewPhotoBytes - photoId,
        )
    }

    fun saveTripOverallReview(rating: Int, content: String, representativePhotoId: String?) {
        val trip = _state.value.selected ?: return
        launch {
            val review = repository.saveTripOverallReview(trip.id, rating, content, representativePhotoId)
            detailCache[trip.id]?.let { cached -> detailCache[trip.id] = cached.copy(overallReview = review) }
            val reviews = _state.value.overallReviews + (trip.id to review)
            _state.value.copy(
                overallReviews = reviews,
                overallReviewPhotoBytes = (_state.value.overallReviewPhotoBytes - trip.id) +
                    loadOverallReviewPhotoBytes(mapOf(trip.id to review)),
            )
        }
    }

    fun loadGoogleCalendars() = launch {
        val calendars = calendarExporter.calendars()
        _state.value.copy(
            googleCalendars = calendars,
            calendarExportMessage = if (calendars.isEmpty()) "기기에 연결된 Google 캘린더를 찾을 수 없습니다." else null,
        )
    }

    fun exportSelectedTripToCalendar(calendarId: Long) {
        val trip = _state.value.selected ?: return
        launch {
            val result = calendarExporter.export(trip, calendarId)
            _state.value.copy(
                calendarExportMessage = "Google 캘린더에 ${result.created}개를 추가하고 ${result.updated}개를 업데이트했습니다.",
            )
        }
    }

    fun clearCalendarExportMessage() {
        _state.value = _state.value.copy(calendarExportMessage = null)
    }

    fun invite(email: String) {
        val trip = _state.value.selected ?: return
        launch {
            repository.invite(trip.id, email)
            val members = repository.members(trip.id)
            detailCache[trip.id]?.let { cached -> detailCache[trip.id] = cached.copy(members = members) }
            _state.value.copy(members = members)
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
        val trips = repository.trips()
        val overallReviews = loadOverallReviews(trips)
        _state.value.copy(
            authenticated = true,
            email = repository.email,
            trips = trips,
            overallReviews = overallReviews,
            overallReviewPhotoBytes = loadOverallReviewPhotoBytes(overallReviews),
            invitations = repository.invitations(),
        )
    }

    private suspend fun loadPhotoBytes(reviews: Map<String, Review>): Map<String, ByteArray> = buildMap {
        reviews.forEach { (itemId, review) ->
            review.photos.forEach { photo -> put(photo.id, repository.reviewPhoto(itemId, photo.id)) }
        }
    }

    private fun loadReviewPhotosInBackground(detail: TripDetail) {
        val missing = detail.reviews
            .flatMap { review -> review.photos.map { photo -> review.itemId to photo } }
            .distinctBy { it.second.id }
            .filter { (_, photo) ->
                photo.id !in _state.value.reviewPhotoBytes && photoIdsInFlight.add(photo.id)
            }
        if (missing.isEmpty()) return

        viewModelScope.launch {
            try {
                missing.chunked(PHOTO_DOWNLOAD_CONCURRENCY).forEach { chunk ->
                    val loaded = coroutineScope {
                        chunk.map { (itemId, photo) ->
                            async {
                                photo to runCatching { repository.reviewPhoto(itemId, photo.id) }.getOrNull()
                            }
                        }.awaitAll()
                    }
                    loaded.forEach { (photo, bytes) ->
                        if (bytes != null) {
                            _state.update { current ->
                                val coverBytes = if (detail.overallReview?.representativePhoto?.id == photo.id) {
                                    current.overallReviewPhotoBytes + (detail.trip.id to bytes)
                                } else current.overallReviewPhotoBytes
                                current.copy(
                                    reviewPhotoBytes = current.reviewPhotoBytes + (photo.id to bytes),
                                    overallReviewPhotoBytes = coverBytes,
                                )
                            }
                        }
                        photoIdsInFlight.remove(photo.id)
                    }
                }
            } finally {
                missing.forEach { photoIdsInFlight.remove(it.second.id) }
            }
        }
    }

    private suspend fun loadOverallReviews(trips: List<Trip>): Map<String, TripOverallReview> = buildMap {
        trips.forEach { trip -> repository.getTripOverallReview(trip.id)?.let { put(trip.id, it) } }
    }

    private suspend fun loadOverallReviewPhotoBytes(reviews: Map<String, TripOverallReview>): Map<String, ByteArray> = buildMap {
        reviews.values.forEach { review ->
            review.representativePhoto?.let { photo ->
                put(review.tripId, repository.reviewPhoto(photo.itemId, photo.id))
            }
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
        private const val PHOTO_DOWNLOAD_CONCURRENCY = 4
    }
}
