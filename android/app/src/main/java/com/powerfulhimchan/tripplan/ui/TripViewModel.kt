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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

data class TripUiState(
    val versionChecking: Boolean = false,
    val requiredUpdate: AppVersionResponse? = null,
    val authenticated: Boolean = false,
    val email: String? = null,
    val trips: List<Trip> = emptyList(),
    val selected: Trip? = null,
    val reviews: Map<String, Review> = emptyMap(),
    val overallReviews: Map<String, TripOverallReview> = emptyMap(),
    val googleCalendars: List<GoogleCalendar> = emptyList(),
    val calendarExportMessage: String? = null,
    val invitations: List<Invitation> = emptyList(),
    val members: List<TripMember> = emptyList(),
    val detailLoading: Boolean = false,
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val busyOperations: Set<String> = emptySet(),
    val error: String? = null,
    val errorCanRetry: Boolean = false,
)

private data class HomeData(
    val trips: List<Trip>,
    val overallReviews: Map<String, TripOverallReview>,
    val invitations: List<Invitation>,
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
    private var retryAction: (() -> Unit)? = null

    init {
        checkAppVersion()
    }

    fun login(email: String, password: String) = authenticate { repository.login(email, password) }

    fun register(email: String, password: String) = authenticate { repository.register(email, password) }

    fun logout() {
        repository.logout()
        detailCache.clear()
        _state.value = TripUiState()
    }

    private fun checkAppVersion() {
        viewModelScope.launch {
            val policy = runCatching { repository.appVersion(BuildConfig.VERSION_CODE) }.getOrNull()
            _state.value = _state.value.copy(
                versionChecking = false,
                requiredUpdate = policy?.takeIf { it.updateRequired },
            )
            if (policy?.updateRequired != true && repository.isLoggedIn) refresh(manual = false)
        }
    }

    fun refresh(manual: Boolean = true) {
        launchOperation(
            key = "home-refresh",
            retry = { refresh(manual = true) },
            refreshing = manual,
        ) {
            val home = loadHomeData()
            _state.value.copy(
                trips = home.trips,
                overallReviews = home.overallReviews,
                invitations = home.invitations,
                authenticated = true,
                email = repository.email,
            )
        }
    }

    fun refreshSelectedTrip() {
        val trip = _state.value.selected ?: return
        detailCache.remove(trip.id)
        loadTripDetail(trip, refreshing = true)
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
        if (cached != null) return

        loadTripDetail(trip)
    }

    fun createTrip(request: CreateTripRequest) = launchOperation("create-trip") {
        val created = repository.createTrip(request)
        _state.value.copy(trips = (_state.value.trips + created).sortedBy(Trip::startDate))
    }

    fun updateSelectedTrip(request: CreateTripRequest) {
        val trip = _state.value.selected ?: return
        launchOperation("update-trip-${trip.id}", retry = { updateSelectedTrip(request) }) {
            val updated = repository.updateTrip(trip.id, request)
            replaceTrip(updated)
        }
    }

    fun addItem(request: CreateItemRequest) {
        val trip = _state.value.selected ?: return
        launchOperation("add-item-${trip.id}", retry = { addItem(request) }) {
            val created = repository.addItem(trip.id, request)
            updateSelectedItems { items -> (items + created).sortedBy(ItineraryItem::scheduledAt) }
        }
    }

    fun updateItem(itemId: String, request: CreateItemRequest) {
        val trip = _state.value.selected ?: return
        launchOperation("update-item-$itemId", retry = { updateItem(itemId, request) }) {
            val updated = repository.updateItem(itemId, request)
            updateSelectedItems { items ->
                items.map { if (it.id == itemId) updated else it }.sortedBy(ItineraryItem::scheduledAt)
            }
        }
    }

    fun toggleNotification(item: ItineraryItem, enabled: Boolean) {
        val trip = _state.value.selected ?: return
        launchOperation("notification-${item.id}", retry = { toggleNotification(item, enabled) }) {
            val updated = repository.setNotification(item, enabled)
            updateSelectedItems { items -> items.map { if (it.id == item.id) updated else it } }
        }
    }

    fun saveReview(
        itemId: String,
        rating: Int,
        content: String,
        photoUris: List<Uri>,
        removedPhotoIds: Set<String>,
    ) = launchOperation("save-review-$itemId") {
        var review = repository.saveReview(itemId, rating, content)
        removedPhotoIds.forEach { photoId ->
            review = repository.deleteReviewPhoto(itemId, photoId)
        }
        if (photoUris.isNotEmpty()) {
            val remaining = MAX_PHOTO_COUNT - review.photos.size
            require(photoUris.size <= remaining) { "후기 사진은 최대 5장까지 등록할 수 있습니다." }
            val uploads = withContext(Dispatchers.IO) { photoUris.map(::readPhoto) }
            review = repository.uploadReviewPhotos(itemId, uploads)
        }
        val trip = _state.value.selected
        val refreshedOverallReview = trip?.let { repository.getTripOverallReview(it.id) }
        val overallReviews = if (trip != null) {
            if (refreshedOverallReview != null) _state.value.overallReviews + (trip.id to refreshedOverallReview)
            else _state.value.overallReviews - trip.id
        } else _state.value.overallReviews
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
            overallReviews = overallReviews,
        )
    }

    fun deleteReviewPhoto(itemId: String, photoId: String) = launchOperation("delete-photo-$photoId") {
        val review = repository.deleteReviewPhoto(itemId, photoId)
        _state.value.selected?.let { trip ->
            detailCache[trip.id]?.let { cached ->
                detailCache[trip.id] = cached.copy(
                    reviews = cached.reviews.filterNot { it.itemId == itemId } + review,
                )
            }
        }
        _state.value.copy(reviews = _state.value.reviews + (itemId to review))
    }

    fun saveTripOverallReview(rating: Int, content: String, representativePhotoId: String?) {
        val trip = _state.value.selected ?: return
        launchOperation("save-overall-review-${trip.id}") {
            val review = repository.saveTripOverallReview(trip.id, rating, content, representativePhotoId)
            detailCache[trip.id]?.let { cached -> detailCache[trip.id] = cached.copy(overallReview = review) }
            val reviews = _state.value.overallReviews + (trip.id to review)
            _state.value.copy(overallReviews = reviews)
        }
    }

    fun loadGoogleCalendars() = launchOperation("calendar-load") {
        val calendars = withContext(Dispatchers.IO) { calendarExporter.calendars() }
        _state.value.copy(
            googleCalendars = calendars,
            calendarExportMessage = if (calendars.isEmpty()) "기기에 연결된 Google 캘린더를 찾을 수 없습니다." else null,
        )
    }

    fun exportSelectedTripToCalendar(calendarId: Long) {
        val trip = _state.value.selected ?: return
        launchOperation("calendar-export") {
            val result = withContext(Dispatchers.IO) { calendarExporter.export(trip, calendarId) }
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
        launchOperation("invite-${trip.id}") {
            repository.invite(trip.id, email)
            val members = repository.members(trip.id)
            detailCache[trip.id]?.let { cached -> detailCache[trip.id] = cached.copy(members = members) }
            _state.value.copy(members = members)
        }
    }

    fun acceptInvitation(id: String) = launchOperation("accept-invitation-$id") {
        repository.acceptInvitation(id)
        val home = loadHomeData()
        _state.value.copy(
            trips = home.trips,
            overallReviews = home.overallReviews,
            invitations = home.invitations,
        )
    }

    fun declineInvitation(id: String) = launchOperation("decline-invitation-$id") {
        repository.declineInvitation(id)
        _state.value.copy(invitations = repository.invitations())
    }

    fun registerDevice(token: String) {
        deviceToken = token
        if (_state.value.authenticated) launchOperation("register-device") { repository.registerDevice(token); _state.value }
    }

    private fun authenticate(request: suspend () -> AuthResponse) = launchOperation("authenticate", showGlobalLoading = true) {
        request()
        deviceToken?.let { repository.registerDevice(it) }
        val home = loadHomeData()
        _state.value.copy(
            authenticated = true,
            email = repository.email,
            trips = home.trips,
            overallReviews = home.overallReviews,
            invitations = home.invitations,
        )
    }

    private suspend fun loadHomeData(): HomeData = coroutineScope {
        val trips = async { repository.trips() }
        val overallReviews = async { repository.getTripOverallReviews().associateBy(TripOverallReview::tripId) }
        val invitations = async { repository.invitations() }
        HomeData(trips.await(), overallReviews.await(), invitations.await())
    }

    fun dismissError() {
        retryAction = null
        _state.update { it.copy(error = null, errorCanRetry = false) }
    }

    fun retryLastRequest() {
        val retry = retryAction ?: return
        retryAction = null
        _state.update { it.copy(error = null, errorCanRetry = false) }
        retry()
    }

    private fun loadTripDetail(trip: Trip, refreshing: Boolean = false) {
        viewModelScope.launch {
            if (refreshing) {
                _state.update {
                    it.copy(
                        refreshing = true,
                        busyOperations = it.busyOperations + "trip-refresh-${trip.id}",
                        error = null,
                        errorCanRetry = false,
                    )
                }
            }
            runCatching { repository.tripDetail(trip.id) }
                .onSuccess { detail ->
                    retryAction = null
                    detailCache[trip.id] = detail
                    _state.update { current ->
                        if (current.selected?.id != trip.id) {
                            current.copy(
                                refreshing = false,
                                busyOperations = current.busyOperations - "trip-refresh-${trip.id}",
                            )
                        } else {
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
                                refreshing = false,
                                busyOperations = current.busyOperations - "trip-refresh-${trip.id}",
                                error = null,
                                errorCanRetry = false,
                            )
                        }
                    }
                }
                .onFailure { failure ->
                    retryAction = { loadTripDetail(trip, refreshing = true) }
                    _state.update { current ->
                        current.copy(
                            detailLoading = false,
                            refreshing = false,
                            busyOperations = current.busyOperations - "trip-refresh-${trip.id}",
                            error = failure.message ?: "여행 상세 정보를 불러오지 못했습니다.",
                            errorCanRetry = true,
                        )
                    }
                }
        }
    }

    private fun replaceTrip(updated: Trip): TripUiState {
        val current = _state.value
        detailCache[updated.id]?.let { detailCache[updated.id] = it.copy(trip = updated) }
        return current.copy(
            trips = current.trips.map { if (it.id == updated.id) updated else it },
            selected = current.selected?.let { if (it.id == updated.id) updated else it },
        )
    }

    private fun updateSelectedItems(transform: (List<ItineraryItem>) -> List<ItineraryItem>): TripUiState {
        val current = _state.value
        val selected = current.selected ?: return current
        val updated = selected.copy(items = transform(selected.items))
        detailCache[selected.id]?.let { detailCache[selected.id] = it.copy(trip = updated) }
        return current.copy(
            selected = updated,
            trips = current.trips.map { if (it.id == updated.id) updated else it },
        )
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

    private fun launchOperation(
        key: String,
        showGlobalLoading: Boolean = false,
        refreshing: Boolean = false,
        retry: (() -> Unit)? = null,
        block: suspend () -> TripUiState,
    ) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    loading = if (showGlobalLoading) true else it.loading,
                    refreshing = if (refreshing) true else it.refreshing,
                    busyOperations = it.busyOperations + key,
                    error = null,
                    errorCanRetry = false,
                )
            }
            runCatching { block() }
                .onSuccess { next ->
                    retryAction = null
                    _state.value = next.copy(
                        loading = if (showGlobalLoading) false else next.loading,
                        refreshing = if (refreshing) false else next.refreshing,
                        busyOperations = next.busyOperations - key,
                        error = null,
                        errorCanRetry = false,
                    )
                }
                .onFailure { failure ->
                    retryAction = retry
                    _state.update {
                        it.copy(
                            loading = if (showGlobalLoading) false else it.loading,
                            refreshing = if (refreshing) false else it.refreshing,
                            busyOperations = it.busyOperations - key,
                            error = failure.message ?: "요청에 실패했습니다.",
                            errorCanRetry = retry != null,
                        )
                    }
                }
        }
    }

    companion object {
        private const val MAX_PHOTO_COUNT = 5
        private const val MAX_PHOTO_SIZE = 5 * 1024 * 1024
    }
}
