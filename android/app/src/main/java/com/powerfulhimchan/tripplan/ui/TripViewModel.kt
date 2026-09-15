package com.powerfulhimchan.tripplan.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.powerfulhimchan.tripplan.data.TokenStore
import com.powerfulhimchan.tripplan.data.TripRepository
import com.powerfulhimchan.tripplan.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TripUiState(
    val authenticated: Boolean = false,
    val email: String? = null,
    val trips: List<Trip> = emptyList(),
    val selected: Trip? = null,
    val reviews: Map<String, Review> = emptyMap(),
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
        _state.value = _state.value.copy(selected = trip, reviews = emptyMap(), members = emptyList())
        if (trip != null) launch {
            val reviews = trip.items.mapNotNull { item ->
                repository.getReview(item.id)?.let { item.id to it }
            }.toMap()
            _state.value.copy(reviews = reviews, members = repository.members(trip.id))
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

    fun saveReview(itemId: String, rating: Int, content: String) = launch {
        val review = repository.saveReview(itemId, rating, content)
        _state.value.copy(reviews = _state.value.reviews + (itemId to review))
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

    private fun launch(block: suspend () -> TripUiState) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            _state.value = runCatching { block() }
                .getOrElse { _state.value.copy(error = it.message ?: "요청에 실패했습니다.") }
                .copy(loading = false)
        }
    }
}
