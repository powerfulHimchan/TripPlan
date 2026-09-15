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
    val review: Review? = null,
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
        _state.value = _state.value.copy(selected = trip, review = null)
        if (trip != null) launch { _state.value.copy(review = repository.getReview(trip.id)) }
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

    fun saveReview(rating: Int, content: String) {
        val trip = _state.value.selected ?: return
        launch { _state.value.copy(review = repository.saveReview(trip.id, rating, content)) }
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

