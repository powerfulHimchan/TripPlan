package com.powerfulhimchan.tripplan.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.powerfulhimchan.tripplan.data.TripRepository
import com.powerfulhimchan.tripplan.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TripUiState(
    val trips: List<Trip> = emptyList(),
    val selected: Trip? = null,
    val reviews: Map<String, Review> = emptyMap(),
    val loading: Boolean = false,
    val error: String? = null,
)

class TripViewModel(private val repository: TripRepository = TripRepository()) : ViewModel() {
    private val _state = MutableStateFlow(TripUiState())
    val state = _state.asStateFlow()

    init { refresh() }

    fun refresh() = launch {
        val trips = repository.trips()
        val selectedId = _state.value.selected?.id
        _state.value.copy(trips = trips, selected = trips.firstOrNull { it.id == selectedId }, error = null)
    }

    fun select(trip: Trip?) {
        _state.value = _state.value.copy(selected = trip, reviews = emptyMap())
        if (trip != null) launch {
            val reviews = trip.items.mapNotNull { item ->
                repository.getReview(item.id)?.let { item.id to it }
            }.toMap()
            _state.value.copy(reviews = reviews)
        }
    }

    fun createTrip(request: CreateTripRequest) = launch {
        repository.createTrip(request)
        val trips = repository.trips()
        _state.value.copy(trips = trips)
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

    fun saveReview(itemId: String, rating: Int, content: String) {
        launch {
            val review = repository.saveReview(itemId, rating, content)
            _state.value.copy(reviews = _state.value.reviews + (itemId to review))
        }
    }

    fun registerDevice(token: String) = launch { repository.registerDevice(token); _state.value }

    private fun launch(block: suspend () -> TripUiState) {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            _state.value = runCatching { block() }
                .getOrElse { _state.value.copy(error = it.message ?: "요청에 실패했습니다.") }
                .copy(loading = false)
        }
    }
}
