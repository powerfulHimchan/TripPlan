@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.powerfulhimchan.tripplan.ui

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import com.powerfulhimchan.tripplan.BuildConfig
import com.powerfulhimchan.tripplan.R
import com.powerfulhimchan.tripplan.model.*
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val Teal = Color(0xFF26667F)
private val Sky = Color(0xFFDFF6FC)
private val PaleSky = Color(0xFFF2FAFC)
private val FieldBackground = Color(0xFFF8FBFC)
private val Coral = Color(0xFFFF7D6B)
private val Leaf = Color(0xFF55A97B)
private val Ink = Color(0xFF20343D)
private val Muted = Color(0xFF6A7F88)
private val Border = Color(0xFFDCE8EC)

@Composable
fun TripApp(viewModel: TripViewModel) {
    val state by viewModel.state.collectAsState()
    val requiredUpdate = state.requiredUpdate
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Teal,
            onPrimary = Color.White,
            secondary = Coral,
            tertiary = Leaf,
            background = PaleSky,
            surface = Color.White,
            surfaceVariant = Sky,
            onSurface = Ink,
            outline = Border,
        ),
        shapes = Shapes(
            small = RoundedCornerShape(14.dp),
            medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(28.dp),
        ),
    ) {
        Surface(Modifier.fillMaxSize()) {
            when {
                state.versionChecking -> VersionCheckScreen()
                requiredUpdate != null -> ForceUpdateScreen(requiredUpdate)
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
private fun VersionCheckScreen() {
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Sky, PaleSky, Color.White))),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.login_hero),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth(0.72f).aspectRatio(1.8f),
                contentScale = ContentScale.Fit,
            )
            Text(
                stringResource(R.string.app_name),
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Teal,
            )
            Spacer(Modifier.height(20.dp))
            CircularProgressIndicator(color = Coral, strokeWidth = 3.dp)
            Spacer(Modifier.height(12.dp))
            Text("사용 가능한 버전을 확인하고 있어요", color = Muted)
        }
    }
}

@Composable
private fun ForceUpdateScreen(policy: AppVersionResponse) {
    val context = LocalContext.current
    BackHandler(enabled = true) {}
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Sky, PaleSky, Color.White))),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.login_hero),
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().aspectRatio(1.8f),
                contentScale = ContentScale.Fit,
            )
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("새로운 여담을 만나볼까요?", color = Coral, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "업데이트가 필요해요",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Ink,
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        policy.message,
                        color = Muted,
                        lineHeight = 22.sp,
                    )
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        color = FieldBackground,
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(
                            "현재 ${BuildConfig.VERSION_NAME} (${policy.currentVersionCode})  ·  " +
                                "최소 ${policy.minimumVersionCode}",
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = Muted,
                        )
                    }
                    Spacer(Modifier.height(22.dp))
                    Button(
                        onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse(policy.storeUrl)).apply {
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                },
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                    ) {
                        Text("지금 업데이트", fontWeight = FontWeight.Bold)
                    }
                }
            }
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
    val fieldColors = yeodamFieldColors()
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
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Sky, PaleSky, Color.White))),
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = yeodamTopBarColors(),
                    title = {
                        Column {
                            Text(stringResource(R.string.app_name), fontWeight = FontWeight.ExtraBold, color = Teal)
                            Text(state.email.orEmpty(), style = MaterialTheme.typography.labelSmall, color = Muted)
                        }
                    },
                    actions = {
                        TextButton(onClick = { showInvitations = true }) {
                            Text("초대 ${state.invitations.size}", fontWeight = FontWeight.SemiBold)
                        }
                        TextButton(onClick = onLogout) { Text("로그아웃", color = Muted) }
                    },
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { showCreate = true },
                    containerColor = Coral,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(18.dp),
                ) { Text("＋ 새 여행", fontWeight = FontWeight.Bold) }
            },
        ) { padding ->
            if (state.trips.isEmpty() && !state.loading) {
                LazyColumn(
                    Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    item {
                        Image(
                            painter = painterResource(R.drawable.login_hero),
                            contentDescription = null,
                            modifier = Modifier.fillMaxWidth().aspectRatio(1.8f),
                            contentScale = ContentScale.Fit,
                        )
                        Text("첫 여행을 계획해보세요", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Ink)
                        Spacer(Modifier.height(8.dp))
                        Text("일정과 알림, 여행 후 추억까지\n여담에서 함께 담아보세요.", color = Muted, lineHeight = 22.sp)
                        Spacer(Modifier.height(22.dp))
                        Button(
                            onClick = { showCreate = true },
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.fillMaxWidth().height(54.dp),
                        ) { Text("첫 여행 만들기", fontWeight = FontWeight.Bold) }
                    }
                }
            } else {
                LazyColumn(
                    Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item {
                        Text("어디로 떠나볼까요?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Ink)
                        Text("계획하고, 함께 담은 여행 ${state.trips.size}개", color = Muted)
                    }
                    items(state.trips, key = { it.id }) { trip ->
                        Card(
                            onClick = { onSelect(trip) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        ) {
                            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(shape = RoundedCornerShape(50), color = Sky) {
                                    Text(trip.destination, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = Teal, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                }
                                Text(trip.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Ink)
                                Text("${trip.startDate}  —  ${trip.endDate}", color = Muted)
                                HorizontalDivider(color = Border)
                                Text("일정 ${trip.items.size}개  ·  추억을 담으러 가기 →", color = Coral, fontWeight = FontWeight.SemiBold)
                            }
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
        shape = RoundedCornerShape(28.dp),
        containerColor = Color.White,
        tonalElevation = 10.dp,
        title = { Text("받은 여행 초대", color = Teal, fontWeight = FontWeight.ExtraBold) },
        text = {
            if (invitations.isEmpty()) Text("대기 중인 초대가 없습니다.", color = Muted)
            else Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                invitations.forEach { invitation ->
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = PaleSky),
                        border = BorderStroke(1.dp, Border),
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(invitation.tripTitle, fontWeight = FontWeight.Bold, color = Ink)
                            Text("${invitation.inviterEmail} 님의 초대", style = MaterialTheme.typography.bodySmall, color = Muted)
                            Row(modifier = Modifier.align(Alignment.End)) {
                                TextButton(onClick = { onDecline(invitation.id) }) { Text("거절") }
                                Button(onClick = { onAccept(invitation.id) }, shape = RoundedCornerShape(14.dp)) { Text("수락") }
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
    BackHandler(onBack = onBack)
    val zoneId = remember(trip.timezone) { ZoneId.of(trip.timezone) }
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(trip.id) {
        while (true) {
            now = Instant.now()
            delay(60_000)
        }
    }
    val canInvite = state.members.any { it.owner && it.email == state.email }
    val activeItem = trip.items.firstOrNull { scheduleProgress(it, now) == ScheduleProgress.IN_PROGRESS }
    val hasCompletedItem = trip.items.any { scheduleProgress(it, now) == ScheduleProgress.COMPLETED }
    val timelineGroups = remember(trip.items, trip.timezone) {
        trip.items.groupBy { Instant.parse(it.scheduledAt).atZone(zoneId).toLocalDate() }
    }
    var showItem by remember { mutableStateOf(false) }
    var showSharing by remember { mutableStateOf(false) }
    var showTimeline by remember(trip.id) { mutableStateOf(false) }
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Sky, PaleSky, Color.White))),
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = yeodamTopBarColors(),
                    title = { Text(trip.title, fontWeight = FontWeight.ExtraBold, color = Ink) },
                    navigationIcon = { TextButton(onClick = onBack) { Text("‹ 목록", fontWeight = FontWeight.SemiBold) } },
                    actions = { TextButton(onClick = { showSharing = true }) { Text("함께 ${state.members.size}", fontWeight = FontWeight.SemiBold) } },
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = { showItem = true },
                    containerColor = Coral,
                    contentColor = Color.White,
                    shape = RoundedCornerShape(18.dp),
                ) { Text("＋ 일정 추가", fontWeight = FontWeight.Bold) }
            },
        ) { padding ->
            LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(26.dp))
                            .background(Brush.linearGradient(listOf(Teal, Color(0xFF3E8EA0))))
                            .padding(22.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.18f)) {
                                Text("여행지", modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp), color = Color.White, style = MaterialTheme.typography.labelMedium)
                            }
                            Text(trip.destination, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            Text("${trip.startDate}  —  ${trip.endDate}", color = Color.White.copy(alpha = 0.82f))
                            Text("일정 ${trip.items.size}개 · 함께하는 사람 ${state.members.size}명", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                item {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (!showTimeline) {
                            Button(onClick = { showTimeline = false }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("일정 카드") }
                            OutlinedButton(onClick = { showTimeline = true }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("전체 타임라인") }
                        } else {
                            OutlinedButton(onClick = { showTimeline = false }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("일정 카드") }
                            Button(onClick = { showTimeline = true }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("전체 타임라인") }
                        }
                    }
                }
                activeItem?.let { current ->
                    item(key = "active-${current.id}") {
                        Surface(shape = RoundedCornerShape(20.dp), color = Coral, shadowElevation = 4.dp) {
                            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text("지금 진행 중", color = Color.White.copy(alpha = 0.82f), style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                    Text(current.title, color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                                    Text(itineraryTimeRange(current, zoneId), color = Color.White.copy(alpha = 0.9f))
                                }
                                Text("NOW", color = Color.White, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
                item {
                    Text(
                        if (showTimeline) "여행 전체 타임라인" else if (hasCompletedItem) "여행의 순간들" else "다가오는 일정",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Ink,
                    )
                    Text(
                        if (showTimeline) "날짜별 흐름과 현재 진행 상태를 한눈에 확인하세요."
                        else if (hasCompletedItem) "종료된 일정마다 기억에 남은 이야기를 기록해보세요."
                        else "알림을 켜두면 계획한 시간에 알려드려요.",
                        color = Muted,
                    )
                }
                if (trip.items.isEmpty()) item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, Border),
                    ) {
                        Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("아직 등록된 일정이 없어요", fontWeight = FontWeight.Bold, color = Ink)
                            Text("첫 일정을 추가해 여행을 채워보세요.", color = Muted)
                        }
                    }
                }
                if (showTimeline) {
                    timelineGroups.forEach { (date, dayItems) ->
                        item(key = "date-$date") {
                            Text(
                                date.format(DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN)),
                                color = Teal,
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        items(dayItems, key = { "timeline-${it.id}" }) { timelineItem ->
                            TimelineItemCard(
                                item = timelineItem,
                                zoneId = zoneId,
                                now = now,
                                isLast = timelineItem.id == dayItems.last().id,
                            )
                        }
                    }
                } else {
                    items(trip.items, key = { it.id }) { item ->
                        ItineraryCard(
                            item, zoneId, now, onToggle,
                            scheduleProgress(item, now) == ScheduleProgress.COMPLETED,
                            state.reviews[item.id],
                            state.reviewPhotoBytes,
                            { rating, content, photos -> onSaveReview(item.id, rating, content, photos) },
                            { photoId -> onDeleteReviewPhoto(item.id, photoId) },
                        )
                    }
                }
            }
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
        shape = RoundedCornerShape(28.dp),
        containerColor = Color.White,
        tonalElevation = 10.dp,
        title = { Text("함께 계획하는 사람", color = Teal, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                members.forEach { member ->
                    Surface(shape = RoundedCornerShape(16.dp), color = PaleSky, border = BorderStroke(1.dp, Border)) {
                        Column(Modifier.fillMaxWidth().padding(12.dp)) {
                            Text(if (member.owner) "여행 만든 사람" else "함께하는 사람", color = Coral, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text(member.email, color = Ink)
                        }
                    }
                }
                if (canInvite) {
                    HorizontalDivider(color = Border)
                    OutlinedTextField(
                        email,
                        { email = it },
                        label = { Text("가입된 회원 이메일") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = yeodamFieldColors(),
                    )
                }
            }
        },
        confirmButton = {
            if (canInvite) Button(onClick = { onInvite(email) }, enabled = email.isNotBlank(), shape = RoundedCornerShape(14.dp)) { Text("초대 보내기") }
            else TextButton(onClick = onDismiss) { Text("확인") }
        },
        dismissButton = { if (canInvite) TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

private enum class ScheduleProgress(val label: String) {
    UPCOMING("예정"),
    IN_PROGRESS("진행 중"),
    COMPLETED("완료"),
}

private fun scheduleProgress(item: ItineraryItem, now: Instant): ScheduleProgress {
    val startsAt = Instant.parse(item.scheduledAt)
    val endsAt = Instant.parse(item.endsAt)
    return when {
        now.isBefore(startsAt) -> ScheduleProgress.UPCOMING
        now.isBefore(endsAt) -> ScheduleProgress.IN_PROGRESS
        else -> ScheduleProgress.COMPLETED
    }
}

private fun itineraryTimeRange(item: ItineraryItem, zoneId: ZoneId): String {
    val formatter = DateTimeFormatter.ofPattern("HH:mm")
    val startsAt = Instant.parse(item.scheduledAt).atZone(zoneId)
    val endsAt = Instant.parse(item.endsAt).atZone(zoneId)
    return "${startsAt.format(formatter)} — ${endsAt.format(formatter)}"
}

@Composable
private fun TimelineItemCard(
    item: ItineraryItem,
    zoneId: ZoneId,
    now: Instant,
    isLast: Boolean,
) {
    val progress = scheduleProgress(item, now)
    val accentColor = when (progress) {
        ScheduleProgress.UPCOMING -> Teal
        ScheduleProgress.IN_PROGRESS -> Coral
        ScheduleProgress.COMPLETED -> Leaf
    }
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(Modifier.width(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(16.dp).clip(RoundedCornerShape(50)).background(accentColor))
            if (!isLast) Box(Modifier.width(2.dp).weight(1f).background(Border))
        }
        Card(
            modifier = Modifier.weight(1f).padding(bottom = if (isLast) 0.dp else 6.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = if (progress == ScheduleProgress.IN_PROGRESS) Color(0xFFFFF1EE) else Color.White),
            border = BorderStroke(if (progress == ScheduleProgress.IN_PROGRESS) 2.dp else 1.dp, if (progress == ScheduleProgress.IN_PROGRESS) Coral else Border),
            elevation = CardDefaults.cardElevation(defaultElevation = if (progress == ScheduleProgress.IN_PROGRESS) 4.dp else 1.dp),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(itineraryTimeRange(item, zoneId), color = accentColor, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                    Surface(shape = RoundedCornerShape(50), color = accentColor.copy(alpha = 0.14f)) {
                        Text(progress.label, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), color = accentColor, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Ink)
                item.place?.let { Text("장소 · $it", color = Muted, style = MaterialTheme.typography.bodySmall) }
                item.memo?.let { Text(it, color = Muted, style = MaterialTheme.typography.bodySmall) }
                if (progress == ScheduleProgress.IN_PROGRESS) {
                    Text("현재 진행 중인 일정입니다", color = Coral, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun ItineraryCard(
    item: ItineraryItem,
    zoneId: ZoneId,
    now: Instant,
    onToggle: (ItineraryItem, Boolean) -> Unit,
    reviewEnabled: Boolean,
    review: Review?,
    photoBytes: Map<String, ByteArray>,
    onSaveReview: (Int, String, List<Uri>) -> Unit,
    onDeletePhoto: (String) -> Unit,
) {
    val progress = scheduleProgress(item, now)
    val dateLabel = Instant.parse(item.scheduledAt).atZone(zoneId).format(DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN))
    val progressColor = when (progress) {
        ScheduleProgress.UPCOMING -> Teal
        ScheduleProgress.IN_PROGRESS -> Coral
        ScheduleProgress.COMPLETED -> Leaf
    }
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(1.dp, Border.copy(alpha = 0.7f)),
    ) {
        Column {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(width = 5.dp, height = 54.dp).clip(RoundedCornerShape(50)).background(if (item.notificationEnabled) Coral else Border))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Surface(shape = RoundedCornerShape(50), color = progressColor.copy(alpha = 0.14f)) {
                        Text(progress.label, modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp), color = progressColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Ink)
                    Text("$dateLabel · ${itineraryTimeRange(item, zoneId)}", style = MaterialTheme.typography.bodyMedium, color = Muted)
                    item.place?.let { Text("장소 · $it", color = Muted, style = MaterialTheme.typography.bodySmall) }
                    if (item.notificationEnabled) {
                        Text("${item.notificationMinutesBefore}분 전 알려드려요", color = Coral, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    } else {
                        Text("알림 꺼짐", color = Muted, style = MaterialTheme.typography.labelMedium)
                    }
                }
                Switch(
                    checked = item.notificationEnabled,
                    onCheckedChange = { onToggle(item, it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = Teal, uncheckedTrackColor = Border),
                )
            }
            if (reviewEnabled) {
                HorizontalDivider(color = Border)
                ReviewEditor(review, photoBytes, onSaveReview, onDeletePhoto)
            }
        }
    }
}

@Composable
private fun CreateTripDialog(onDismiss: () -> Unit, onSave: (CreateTripRequest) -> Unit) {
    var title by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    val tomorrow = remember { LocalDate.now().plusDays(1) }
    var start by remember { mutableStateOf(tomorrow.toString()) }
    var end by remember { mutableStateOf(tomorrow.plusDays(1).toString()) }
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
    val initialWindow = remember(trip.id, trip.items) { nextItineraryWindow(trip) }
    var title by remember { mutableStateOf("") }
    var place by remember { mutableStateOf("") }
    var memo by remember { mutableStateOf("") }
    var startsAt by remember { mutableStateOf(initialWindow.first) }
    var endsAt by remember { mutableStateOf(initialWindow.second) }
    var before by remember { mutableStateOf("30") }
    InputDialog("일정 추가", onDismiss, title.isNotBlank() && startsAt.isNotBlank() && endsAt.isNotBlank(), {
        onSave(
            CreateItemRequest(
                title = title,
                place = place.ifBlank { null },
                memo = memo.ifBlank { null },
                scheduledAt = startsAt,
                endsAt = endsAt,
                notificationMinutesBefore = before.toIntOrNull() ?: 0,
            ),
        )
    }) {
        Field(title, { title = it }, "일정 이름")
        Field(place, { place = it }, "장소 (선택)")
        Field(memo, { memo = it }, "메모 (선택)")
        Field(startsAt, { startsAt = it }, "시작 시각 (ISO-8601)")
        Field(endsAt, { endsAt = it }, "종료 시각 (ISO-8601)")
        Field(before, { before = it }, "몇 분 전 알림")
    }
}

private fun nextItineraryWindow(trip: Trip): Pair<String, String> {
    val zoneId = ZoneId.of(trip.timezone)
    val startsAt = trip.items
        .maxByOrNull { Instant.parse(it.scheduledAt) }
        ?.let { Instant.parse(it.endsAt).atZone(zoneId) }
        ?: LocalDate.parse(trip.startDate).atTime(9, 0).atZone(zoneId)
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX")
    return startsAt.format(formatter) to startsAt.plusHours(1).format(formatter)
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
    Column(Modifier.background(PaleSky).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("이 계획의 여담", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Teal)
        Text("그날의 기분과 기억을 계획별로 남겨보세요.", color = Muted, style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            (1..5).forEach { score ->
                TextButton(
                    onClick = { rating = score },
                    modifier = Modifier.size(40.dp),
                    contentPadding = PaddingValues(0.dp),
                ) { Text(if (score <= rating) "★" else "☆", color = Coral, fontSize = 26.sp) }
            }
        }
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
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, Teal),
        ) { Text("사진 추가 (${existingCount + selectedPhotos.size}/5)") }
        Text("사진은 장당 최대 5MB, 후기당 최대 5장까지 등록할 수 있습니다.", style = MaterialTheme.typography.labelSmall, color = Muted)
        Button(
            onClick = { onSave(rating, content, selectedPhotos) },
            enabled = content.isNotBlank(),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.align(Alignment.End),
        ) { Text("후기 저장") }
    }
}

@Composable
private fun ReviewPhotoRow(name: String, bytes: ByteArray?, onRemove: () -> Unit) {
    Surface(shape = RoundedCornerShape(14.dp), color = Color.White, border = BorderStroke(1.dp, Border)) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            bytes?.let {
                val bitmap = remember(it) {
                    BitmapFactory.decodeByteArray(it, 0, it.size, BitmapFactory.Options().apply { inSampleSize = 4 })
                }
                bitmap?.let { decoded ->
                    Image(
                        bitmap = decoded.asImageBitmap(),
                        contentDescription = name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)),
                    )
                }
            }
            Text(name, modifier = Modifier.weight(1f), maxLines = 1, color = Ink)
            TextButton(onClick = onRemove) { Text("삭제", color = Coral) }
        }
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
    Surface(shape = RoundedCornerShape(14.dp), color = Color.White, border = BorderStroke(1.dp, Border)) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            bitmap?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "선택한 후기 사진",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)),
                )
            }
            Text("선택한 사진", modifier = Modifier.weight(1f), color = Ink)
            TextButton(onClick = onRemove) { Text("취소", color = Coral) }
        }
    }
}

@Composable
private fun InputDialog(title: String, onDismiss: () -> Unit, enabled: Boolean, onSave: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = Color.White,
        tonalElevation = 10.dp,
        title = { Text(title, color = Teal, fontWeight = FontWeight.ExtraBold) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp), content = content) },
        confirmButton = { Button(onClick = onSave, enabled = enabled, shape = RoundedCornerShape(14.dp)) { Text("저장") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("취소") } },
    )
}

@Composable
private fun Field(value: String, onChange: (String) -> Unit, label: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            modifier = Modifier.padding(start = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = Muted,
        )
        OutlinedTextField(
            value,
            onChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = false,
            shape = RoundedCornerShape(16.dp),
            colors = yeodamFieldColors(),
        )
    }
}

@Composable
private fun LoadingOverlay() {
    Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.72f)), contentAlignment = Alignment.Center) {
        Surface(shape = RoundedCornerShape(22.dp), color = Color.White, shadowElevation = 8.dp) {
            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator(color = Teal)
                Text("여행을 담는 중이에요", color = Muted, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun ErrorSnackbar(message: String) {
    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.BottomCenter) {
        Snackbar(containerColor = Ink, contentColor = Color.White, shape = RoundedCornerShape(16.dp)) { Text(message) }
    }
}

@Composable
private fun yeodamFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Teal,
    unfocusedBorderColor = Border,
    focusedContainerColor = FieldBackground,
    unfocusedContainerColor = FieldBackground,
    focusedLabelColor = Teal,
    cursorColor = Teal,
)

@Composable
private fun yeodamTopBarColors() = TopAppBarDefaults.topAppBarColors(
    containerColor = Color.Transparent,
    scrolledContainerColor = Color.White.copy(alpha = 0.94f),
    titleContentColor = Ink,
    actionIconContentColor = Teal,
    navigationIconContentColor = Teal,
)
