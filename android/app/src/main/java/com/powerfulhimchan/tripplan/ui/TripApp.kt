@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.powerfulhimchan.tripplan.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.powerfulhimchan.tripplan.R
import com.powerfulhimchan.tripplan.model.*
import java.time.LocalDate

private val Teal = Color(0xFF26667F)
private val Sand = Color(0xFFF6F1E9)
private val Sky = Color(0xFFDFF6FC)
private val FieldBackground = Color(0xFFF8FBFC)

@Composable
fun TripApp(viewModel: TripViewModel) {
    val state by viewModel.state.collectAsState()
    MaterialTheme(colorScheme = lightColorScheme(primary = Teal, surface = Color(0xFFF8FAF9))) {
        Surface(Modifier.fillMaxSize()) {
            when {
                !state.authenticated -> AuthScreen(state.loading, viewModel::login, viewModel::register)
                state.selected == null -> TripListScreen(
                    state, viewModel::select, viewModel::createTrip, viewModel::logout,
                    viewModel::acceptInvitation, viewModel::declineInvitation,
                )
                else -> TripDetailScreen(
                    state, { viewModel.select(null) }, viewModel::addItem,
                    viewModel::toggleNotification, viewModel::saveReview,
                    viewModel::deleteReviewPhoto, viewModel::invite,
                )
            }
            if (state.loading) LoadingOverlay()
            state.error?.let { ErrorSnackbar(it) }
        }
    }
}

@Composable
private fun AuthScreen(
    loading: Boolean,
    onLogin: (String, String) -> Unit,
    onRegister: (String, String) -> Unit,
) {
    var registerMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Teal,
        unfocusedBorderColor = Color(0xFFD9E4E8),
        focusedContainerColor = FieldBackground,
        unfocusedContainerColor = FieldBackground,
    )
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Sky, Color(0xFFF4FBFD), Color.White))),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(R.drawable.login_hero),
                contentDescription = "여행 가방과 지구본 일러스트",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.8f),
                contentScale = ContentScale.Fit,
            )
            Text(
                stringResource(R.string.app_name),
                fontSize = 42.sp,
                lineHeight = 48.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Teal,
            )
            Text(
                stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF5E7680),
            )
            Spacer(Modifier.height(22.dp))
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(
                        if (registerMode) "이메일로 회원가입" else "이메일로 로그인",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    OutlinedTextField(
                        email,
                        { email = it },
                        label = { Text("이메일 주소") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = fieldColors,
                    )
                    OutlinedTextField(
                        password, { password = it }, label = { Text("비밀번호 (8자 이상)") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(16.dp),
                        colors = fieldColors,
                    )
                    Button(
                        onClick = { if (registerMode) onRegister(email, password) else onLogin(email, password) },
                        enabled = !loading && email.isNotBlank() && password.length >= 8,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                    ) { Text(if (registerMode) "가입하기" else "로그인") }
                    TextButton(onClick = { registerMode = !registerMode }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                        Text(if (registerMode) "이미 회원인가요? 로그인" else "처음인가요? 회원가입")
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun TripListScreen(
    state: TripUiState,
    onSelect: (Trip) -> Unit,
    onCreate: (CreateTripRequest) -> Unit,
    onLogout: () -> Unit,
    onAccept: (String) -> Unit,
    onDecline: (String) -> Unit,
) {
    var showCreate by remember { mutableStateOf(false) }
    var showInvitations by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Column { Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold); Text(state.email.orEmpty(), style = MaterialTheme.typography.labelSmall) } },
                actions = {
                    TextButton(onClick = { showInvitations = true }) { Text("받은 초대 ${state.invitations.size}") }
                    TextButton(onClick = onLogout) { Text("로그아웃") }
                },
            )
        },
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
    if (showInvitations) InvitationDialog(state.invitations, { showInvitations = false }, onAccept, onDecline)
}

@Composable
private fun InvitationDialog(
    invitations: List<Invitation>,
    onDismiss: () -> Unit,
    onAccept: (String) -> Unit,
    onDecline: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("받은 여행 초대") },
        text = {
            if (invitations.isEmpty()) Text("대기 중인 초대가 없습니다.")
            else Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                invitations.forEach { invitation ->
                    Card {
                        Column(Modifier.padding(12.dp)) {
                            Text(invitation.tripTitle, fontWeight = FontWeight.Bold)
                            Text("${invitation.inviterEmail} 님의 초대", style = MaterialTheme.typography.bodySmall)
                            Row {
                                TextButton(onClick = { onDecline(invitation.id) }) { Text("거절") }
                                Button(onClick = { onAccept(invitation.id) }) { Text("수락") }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("닫기") } },
    )
}

@Composable
private fun TripDetailScreen(
    state: TripUiState,
    onBack: () -> Unit,
    onAddItem: (CreateItemRequest) -> Unit,
    onToggle: (ItineraryItem, Boolean) -> Unit,
    onSaveReview: (String, Int, String, List<Uri>) -> Unit,
    onDeleteReviewPhoto: (String, String) -> Unit,
    onInvite: (String) -> Unit,
) {
    val trip = state.selected ?: return
    val tripFinished = !LocalDate.now().isBefore(LocalDate.parse(trip.endDate))
    val canInvite = state.members.any { it.owner && it.email == state.email }
    var showItem by remember { mutableStateOf(false) }
    var showSharing by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(trip.title) },
                navigationIcon = { TextButton(onClick = onBack) { Text("‹ 목록") } },
                actions = { TextButton(onClick = { showSharing = true }) { Text("공유 ${state.members.size}") } },
            )
        },
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
            items(trip.items, key = { it.id }) { item ->
                ItineraryCard(
                    item, onToggle, tripFinished, state.reviews[item.id],
                    state.reviewPhotoBytes,
                    { rating, content, photos -> onSaveReview(item.id, rating, content, photos) },
                    { photoId -> onDeleteReviewPhoto(item.id, photoId) },
                )
            }
            item { Spacer(Modifier.height(72.dp)) }
        }
    }
    if (showItem) AddItemDialog(trip, { showItem = false }) { onAddItem(it); showItem = false }
    if (showSharing) SharingDialog(state.members, canInvite, { showSharing = false }) {
        onInvite(it)
        showSharing = false
    }
}

@Composable
private fun SharingDialog(members: List<TripMember>, canInvite: Boolean, onDismiss: () -> Unit, onInvite: (String) -> Unit) {
    var email by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("함께 계획하는 사람") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                members.forEach { member -> Text("${if (member.owner) "소유자" else "참여자"} · ${member.email}") }
                if (canInvite) {
                    HorizontalDivider()
                    OutlinedTextField(email, { email = it }, label = { Text("가입된 회원 이메일") }, singleLine = true)
                }
            }
        },
        confirmButton = {
            if (canInvite) Button(onClick = { onInvite(email) }, enabled = email.isNotBlank()) { Text("초대 보내기") }
            else TextButton(onClick = onDismiss) { Text("확인") }
        },
        dismissButton = { if (canInvite) TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

@Composable
private fun ItineraryCard(
    item: ItineraryItem,
    onToggle: (ItineraryItem, Boolean) -> Unit,
    reviewEnabled: Boolean,
    review: Review?,
    photoBytes: Map<String, ByteArray>,
    onSaveReview: (Int, String, List<Uri>) -> Unit,
    onDeletePhoto: (String) -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column {
            Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(item.title, fontWeight = FontWeight.SemiBold)
                    Text(item.scheduledAt.replace("T", " ").take(16), style = MaterialTheme.typography.bodySmall)
                    item.place?.let { Text(it, color = Color.Gray) }
                    if (item.notificationEnabled) Text("${item.notificationMinutesBefore}분 전 알림", color = Teal, style = MaterialTheme.typography.labelMedium)
                }
                Switch(checked = item.notificationEnabled, onCheckedChange = { onToggle(item, it) })
            }
            if (reviewEnabled) {
                HorizontalDivider()
                ReviewEditor(review, photoBytes, onSaveReview, onDeletePhoto)
            }
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
private fun ReviewEditor(
    review: Review?,
    photoBytes: Map<String, ByteArray>,
    onSave: (Int, String, List<Uri>) -> Unit,
    onDeletePhoto: (String) -> Unit,
) {
    var rating by remember(review) { mutableIntStateOf(review?.rating ?: 5) }
    var content by remember(review) { mutableStateOf(review?.content ?: "") }
    var selectedPhotos by remember(review?.updatedAt) { mutableStateOf<List<Uri>>(emptyList()) }
    val existingCount = review?.photos?.size ?: 0
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        selectedPhotos = (selectedPhotos + uris).distinct().take(5 - existingCount)
    }
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("이 계획 후기", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row { (1..5).forEach { score -> TextButton(onClick = { rating = score }) { Text(if (score <= rating) "★" else "☆") } } }
        Field(content, { content = it }, "이 계획에서 기억하고 싶은 점")
        review?.photos?.forEach { photo ->
            ReviewPhotoRow(
                name = photo.originalName,
                bytes = photoBytes[photo.id],
                onRemove = { onDeletePhoto(photo.id) },
            )
        }
        selectedPhotos.forEach { uri ->
            SelectedPhotoRow(uri) { selectedPhotos = selectedPhotos - uri }
        }
        OutlinedButton(
            onClick = { picker.launch("image/*") },
            enabled = existingCount + selectedPhotos.size < 5,
        ) { Text("사진 추가 (${existingCount + selectedPhotos.size}/5)") }
        Text("사진은 장당 최대 5MB, 후기당 최대 5장까지 등록할 수 있습니다.", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Button(
            onClick = { onSave(rating, content, selectedPhotos) },
            enabled = content.isNotBlank(),
            modifier = Modifier.align(Alignment.End),
        ) { Text("후기 저장") }
    }
}

@Composable
private fun ReviewPhotoRow(name: String, bytes: ByteArray?, onRemove: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        bytes?.let {
            val bitmap = remember(it) {
                BitmapFactory.decodeByteArray(it, 0, it.size, BitmapFactory.Options().apply { inSampleSize = 4 })
            }
            bitmap?.let { decoded ->
                Image(
                    bitmap = decoded.asImageBitmap(),
                    contentDescription = name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
                )
            }
        }
        Text(name, modifier = Modifier.weight(1f), maxLines = 1)
        TextButton(onClick = onRemove) { Text("삭제") }
    }
}

@Composable
private fun SelectedPhotoRow(uri: Uri, onRemove: () -> Unit) {
    val context = LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(null, uri) {
        value = context.contentResolver.openInputStream(uri)?.use { input ->
            BitmapFactory.decodeStream(input, null, BitmapFactory.Options().apply { inSampleSize = 4 })
        }
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        bitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "선택한 후기 사진",
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(8.dp)),
            )
        }
        Text("선택한 사진", modifier = Modifier.weight(1f))
        TextButton(onClick = onRemove) { Text("취소") }
    }
}

@Composable
private fun InputDialog(title: String, onDismiss: () -> Unit, enabled: Boolean, onSave: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = content) },
        confirmButton = { Button(onClick = onSave, enabled = enabled) { Text("저장") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

@Composable
private fun Field(value: String, onChange: (String) -> Unit, label: String) {
    OutlinedTextField(value, onChange, label = { Text(label) }, modifier = Modifier.fillMaxWidth(), singleLine = false)
}

@Composable
private fun LoadingOverlay() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorSnackbar(message: String) {
    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.BottomCenter) {
        Snackbar { Text(message) }
    }
}
