@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.powerfulhimchan.tripplan.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.powerfulhimchan.tripplan.model.*
import java.time.LocalDate

private val Teal = Color(0xFF26667F)
private val Sand = Color(0xFFF6F1E9)

@Composable
fun TripApp(viewModel: TripViewModel) {
    val state by viewModel.state.collectAsState()
    MaterialTheme(colorScheme = lightColorScheme(primary = Teal, surface = Color(0xFFF8FAF9))) {
        Surface(Modifier.fillMaxSize()) {
            if (state.selected == null) {
                TripListScreen(state, viewModel::select, viewModel::createTrip)
            } else {
                TripDetailScreen(state, { viewModel.select(null) }, viewModel::addItem,
                    viewModel::toggleNotification, viewModel::saveReview)
            }
            state.error?.let { ErrorSnackbar(it) }
        }
    }
}

@Composable
private fun TripListScreen(state: TripUiState, onSelect: (Trip) -> Unit, onCreate: (CreateTripRequest) -> Unit) {
    var showCreate by remember { mutableStateOf(false) }
    Scaffold(
        topBar = { TopAppBar(title = { Text("TripPlan", fontWeight = FontWeight.Bold) }) },
        floatingActionButton = { FloatingActionButton(onClick = { showCreate = true }) { Text("＋") } }
    ) { padding ->
        if (state.trips.isEmpty() && !state.loading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("첫 여행을 계획해보세요", style = MaterialTheme.typography.headlineSmall)
                    Text("여행별 일정과 알림을 한곳에서 관리합니다.", color = Color.Gray)
                }
            }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.trips, key = { it.id }) { trip ->
                    Card(onClick = { onSelect(trip) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                        Column(Modifier.padding(20.dp)) {
                            Text(trip.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(6.dp))
                            Text("${trip.destination}  ·  ${trip.startDate} ~ ${trip.endDate}")
                            Text("일정 ${trip.items.size}개", color = Teal)
                        }
                    }
                }
            }
        }
    }
    if (showCreate) CreateTripDialog({ showCreate = false }) { onCreate(it); showCreate = false }
}

@Composable
private fun TripDetailScreen(
    state: TripUiState,
    onBack: () -> Unit,
    onAddItem: (CreateItemRequest) -> Unit,
    onToggle: (ItineraryItem, Boolean) -> Unit,
    onSaveReview: (Int, String) -> Unit,
) {
    val trip = state.selected ?: return
    var showItem by remember { mutableStateOf(false) }
    Scaffold(
        topBar = { TopAppBar(title = { Text(trip.title) }, navigationIcon = { TextButton(onClick = onBack) { Text("‹ 목록") } }) },
        floatingActionButton = { FloatingActionButton(onClick = { showItem = true }) { Text("＋ 일정") } }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Sand), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Text(trip.destination, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("${trip.startDate} ~ ${trip.endDate}")
                    }
                }
            }
            if (trip.items.isEmpty()) item { Text("등록된 일정이 없습니다.", modifier = Modifier.padding(12.dp), color = Color.Gray) }
            items(trip.items, key = { it.id }) { item -> ItineraryCard(item, onToggle) }
            if (!LocalDate.now().isBefore(LocalDate.parse(trip.endDate))) {
                item { ReviewEditor(state.review, onSaveReview) }
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }
    if (showItem) AddItemDialog(trip, { showItem = false }) { onAddItem(it); showItem = false }
}

@Composable
private fun ItineraryCard(item: ItineraryItem, onToggle: (ItineraryItem, Boolean) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.title, fontWeight = FontWeight.SemiBold)
                Text(item.scheduledAt.replace("T", " ").take(16), style = MaterialTheme.typography.bodySmall)
                item.place?.let { Text(it, color = Color.Gray) }
                if (item.notificationEnabled) Text("${item.notificationMinutesBefore}분 전 알림", color = Teal, style = MaterialTheme.typography.labelMedium)
            }
            Switch(checked = item.notificationEnabled, onCheckedChange = { onToggle(item, it) })
        }
    }
}

@Composable
private fun CreateTripDialog(onDismiss: () -> Unit, onSave: (CreateTripRequest) -> Unit) {
    var title by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var start by remember { mutableStateOf(LocalDate.now().toString()) }
    var end by remember { mutableStateOf(LocalDate.now().plusDays(2).toString()) }
    InputDialog("새 여행", onDismiss, title.isNotBlank() && destination.isNotBlank(), {
        onSave(CreateTripRequest(title, destination, start, end))
    }) {
        Field(title, { title = it }, "여행 이름")
        Field(destination, { destination = it }, "목적지")
        Field(start, { start = it }, "시작일 (YYYY-MM-DD)")
        Field(end, { end = it }, "종료일 (YYYY-MM-DD)")
    }
}

@Composable
private fun AddItemDialog(trip: Trip, onDismiss: () -> Unit, onSave: (CreateItemRequest) -> Unit) {
    var title by remember { mutableStateOf("") }
    var place by remember { mutableStateOf("") }
    var memo by remember { mutableStateOf("") }
    var at by remember { mutableStateOf("${trip.startDate}T09:00:00+09:00") }
    var before by remember { mutableStateOf("30") }
    InputDialog("일정 추가", onDismiss, title.isNotBlank(), {
        onSave(CreateItemRequest(title, place.ifBlank { null }, memo.ifBlank { null }, at, notificationMinutesBefore = before.toIntOrNull() ?: 0))
    }) {
        Field(title, { title = it }, "일정 이름")
        Field(place, { place = it }, "장소 (선택)")
        Field(memo, { memo = it }, "메모 (선택)")
        Field(at, { at = it }, "시각 (ISO-8601)")
        Field(before, { before = it }, "몇 분 전 알림")
    }
}

@Composable
private fun ReviewEditor(review: Review?, onSave: (Int, String) -> Unit) {
    var rating by remember(review) { mutableIntStateOf(review?.rating ?: 5) }
    var content by remember(review) { mutableStateOf(review?.content ?: "") }
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("여행 후기", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Row { (1..5).forEach { score -> TextButton(onClick = { rating = score }) { Text(if (score <= rating) "★" else "☆") } } }
            Field(content, { content = it }, "여행에서 기억하고 싶은 점")
            Button(onClick = { onSave(rating, content) }, enabled = content.isNotBlank(), modifier = Modifier.align(Alignment.End)) { Text("후기 저장") }
        }
    }
}

@Composable
private fun InputDialog(title: String, onDismiss: () -> Unit, enabled: Boolean, onSave: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, title = { Text(title) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }, confirmButton = { Button(onClick = onSave, enabled = enabled) { Text("저장") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } })
}

@Composable
private fun Field(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(value, onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = false)
}

@Composable
private fun ErrorSnackbar(message: String) {
    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.BottomCenter) {
        Snackbar { Text(message) }
    }
}
