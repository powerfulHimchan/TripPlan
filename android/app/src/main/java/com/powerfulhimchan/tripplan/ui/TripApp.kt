@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.powerfulhimchan.tripplan.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.LinearLayout
import android.widget.NumberPicker
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape as ComposeRoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.powerfulhimchan.tripplan.BuildConfig
import com.powerfulhimchan.tripplan.R
import com.powerfulhimchan.tripplan.data.TokenStore
import com.powerfulhimchan.tripplan.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Headers
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.text.NumberFormat
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
private val WonNumberFormat = NumberFormat.getNumberInstance(Locale.KOREA)
private val RectangularShape = ComposeRoundedCornerShape(5.dp)
private val RectangularSmallShape = ComposeRoundedCornerShape(3.dp)

// 기존 크기 호출을 낮은 반경의 사각형 토큰으로 제한해 모든 컨트롤의 모서리를 일관되게 유지한다.
private fun RoundedCornerShape(size: Dp) = if (size <= 10.dp) RectangularSmallShape else RectangularShape
private fun RoundedCornerShape(percent: Int) = if (percent <= 0) RectangularSmallShape else RectangularShape

private data class ItineraryCategoryStyle(
    val value: String,
    val label: String,
    val containerColor: Color,
    val accentColor: Color,
)

private val ItineraryCategoryStyles = listOf(
    ItineraryCategoryStyle("ACCOMMODATION", "숙박", Color(0xFFF1EAFB), Color(0xFF7953A9)),
    ItineraryCategoryStyle("TRANSPORTATION", "교통", Color(0xFFE6F2FF), Color(0xFF397DB8)),
    ItineraryCategoryStyle("SIGHTSEEING", "관광", Color(0xFFE8F6EC), Color(0xFF3D8B5E)),
    ItineraryCategoryStyle("FOOD", "식사", Color(0xFFFFF0E3), Color(0xFFC66A2B)),
    ItineraryCategoryStyle("CAFE", "카페·디저트", Color(0xFFFBECEF), Color(0xFFA8556A)),
    ItineraryCategoryStyle("ACTIVITY", "체험·액티비티", Color(0xFFFFECE8), Color(0xFFD65F4E)),
    ItineraryCategoryStyle("SHOPPING", "쇼핑", Color(0xFFF3ECFA), Color(0xFF8B5AA8)),
    ItineraryCategoryStyle("CULTURE", "공연·문화", Color(0xFFECEFFD), Color(0xFF5267B2)),
    ItineraryCategoryStyle("REST", "휴식", Color(0xFFE5F5F3), Color(0xFF34877E)),
    ItineraryCategoryStyle("OTHER", "기타", Color(0xFFF1F4F5), Color(0xFF66777E)),
)

private fun itineraryCategoryStyle(value: String) =
    ItineraryCategoryStyles.firstOrNull { it.value == value } ?: ItineraryCategoryStyles.last()

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
            small = RectangularSmallShape,
            medium = RectangularShape,
            large = RectangularShape,
        ),
    ) {
        Surface(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize()) {
            when {
                state.versionChecking -> VersionCheckScreen()
                requiredUpdate != null -> ForceUpdateScreen(requiredUpdate)
                !state.authenticated -> AuthScreen(state.loading, viewModel::login, viewModel::register)
                state.selected == null -> TripListScreen(
                    state, viewModel::select, viewModel::createTrip, viewModel::logout,
                    viewModel::acceptInvitation, viewModel::declineInvitation, { viewModel.refresh() },
                )
                else -> TripDetailScreen(
                    state, { viewModel.select(null) }, viewModel::addItem,
                    viewModel::updateItem, viewModel::toggleNotification,
                    viewModel::saveReview, viewModel::saveTripOverallReview,
                    viewModel::loadGoogleCalendars, viewModel::exportSelectedTripToCalendar,
                    viewModel::clearCalendarExportMessage, viewModel::invite,
                    viewModel::updateSelectedTrip,
                    viewModel::refreshSelectedTrip,
                )
            }
            if (state.busyOperations.isNotEmpty() && !state.refreshing && !state.loading) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                    color = Coral,
                    trackColor = Sky,
                )
            }
            state.error?.let {
                ErrorSnackbar(
                    message = it,
                    canRetry = state.errorCanRetry,
                    onRetry = viewModel::retryLastRequest,
                    onDismiss = viewModel::dismissError,
                )
            }
            }
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
    val keyboardController = LocalSoftwareKeyboardController.current
    val submit = {
        if (!loading && email.isNotBlank() && password.length >= 8) {
            keyboardController?.hide()
            if (registerMode) onRegister(email, password) else onLogin(email, password)
        }
    }
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
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next,
                            autoCorrectEnabled = false,
                        ),
                        shape = RoundedCornerShape(16.dp),
                        colors = fieldColors,
                    )
                    OutlinedTextField(
                        password, { password = it }, label = { Text("비밀번호 (8자 이상)") },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done,
                            autoCorrectEnabled = false,
                        ),
                        keyboardActions = KeyboardActions(onDone = { submit() }),
                        shape = RoundedCornerShape(16.dp),
                        colors = fieldColors,
                    )
                    Button(
                        onClick = submit,
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
    onRefresh: () -> Unit,
) {
    var showCreate by remember { mutableStateOf(false) }
    var showInvitations by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    val activeTrips = state.trips.filter { tripProgress(it) != TripProgress.COMPLETED }
    val completedTrips = state.trips.filter { tripProgress(it) == TripProgress.COMPLETED }
    val visibleTrips = if (selectedTab == 0) activeTrips else completedTrips
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
                            YeodamAnimatedWordmark()
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
            PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
            if (state.trips.isEmpty() && !state.refreshing) {
                LazyColumn(
                    Modifier.fillMaxSize(),
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
                    Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 96.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    item {
                        Text("어디로 떠나볼까요?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = Ink)
                        Text("계획하고, 함께 담은 여행 ${state.trips.size}개", color = Muted)
                    }
                    item {
                        TabRow(
                            selectedTabIndex = selectedTab,
                            containerColor = Color.White,
                            contentColor = Teal,
                            modifier = Modifier.clip(RoundedCornerShape(18.dp)),
                            divider = {},
                        ) {
                            Tab(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                text = { Text("여행 계획 ${activeTrips.size}", fontWeight = FontWeight.Bold) },
                            )
                            Tab(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                text = { Text("지난 여행 ${completedTrips.size}", fontWeight = FontWeight.Bold) },
                            )
                        }
                    }
                    if (visibleTrips.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(24.dp),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                border = BorderStroke(1.dp, Border),
                            ) {
                                Column(
                                    Modifier.fillMaxWidth().padding(28.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text(
                                        if (selectedTab == 0) "예정되거나 진행 중인 여행이 없어요" else "아직 종료된 여행이 없어요",
                                        fontWeight = FontWeight.Bold,
                                        color = Ink,
                                    )
                                    Text(
                                        if (selectedTab == 0) "새로운 여행을 계획해보세요." else "여행이 끝나면 이곳에서 다시 볼 수 있어요.",
                                        color = Muted,
                                    )
                                }
                            }
                        }
                    }
                    items(visibleTrips, key = { it.id }) { trip ->
                        val progress = tripProgress(trip)
                        val coverPhoto = state.overallReviews[trip.id]?.representativePhoto
                        val hasCover = coverPhoto != null
                        Card(
                            onClick = { onSelect(trip) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        ) {
                            Box {
                                coverPhoto?.let { photo ->
                                    RemoteReviewPhoto(
                                        itemId = photo.itemId,
                                        photoId = photo.id,
                                        contentDescription = "${trip.title} 대표 사진",
                                        modifier = Modifier.matchParentSize(),
                                    )
                                }
                                if (hasCover) Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.46f)))
                                Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                        Surface(shape = RoundedCornerShape(50), color = if (hasCover) Color.White.copy(alpha = 0.9f) else Sky) {
                                            Text(trip.destination, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = Teal, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(Modifier.weight(1f))
                                        TripStatusBadge(progress)
                                    }
                                    Text(trip.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = if (hasCover) Color.White else Ink)
                                    Text(formatTripDateRange(trip), color = if (hasCover) Color.White.copy(alpha = 0.88f) else Muted)
                                    HorizontalDivider(color = if (hasCover) Color.White.copy(alpha = 0.4f) else Border)
                                    Text(
                                        when (progress) {
                                            TripProgress.UPCOMING -> "일정 ${trip.items.size}개  ·  계획 확인하기 →"
                                            TripProgress.IN_PROGRESS -> "일정 ${trip.items.size}개  ·  진행 중인 여행 보기 →"
                                            TripProgress.COMPLETED -> "일정 ${trip.items.size}개  ·  후기 돌아보기 →"
                                        },
                                        color = if (hasCover) Color.White else Coral,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
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
private fun YeodamAnimatedWordmark() {
    val transition = rememberInfiniteTransition(label = "여담 워드마크")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 5_800
                0f at 0
                0f at 900
                1f at 2_000
                1f at 4_700
                0f at 5_800
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "여행을 담다에서 여담으로",
    )
    val characters = listOf(
        Triple("여", 0f, 0f),
        Triple("행", 24f, 24f),
        Triple("을", 48f, 48f),
        Triple("담", 78f, 27f),
        Triple("다", 102f, 102f),
    )

    Box(Modifier.width(126.dp).height(28.dp)) {
        characters.forEachIndexed { index, (character, startX, endX) ->
            val remains = index == 0 || index == 3
            Text(
                text = character,
                modifier = Modifier
                    .offset(x = (startX + (endX - startX) * progress).dp)
                    .graphicsLayer {
                        scaleY = if (remains) 1f else 1f - progress
                        alpha = if (remains) 1f else 1f - progress
                        transformOrigin = TransformOrigin.Center
                    },
                color = if (remains) lerp(Ink, Teal, progress) else Ink,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
            )
        }
    }
}

private fun formatTripDateRange(trip: Trip): String {
    val formatter = DateTimeFormatter.ofPattern("yyyy년 MM월 dd일")
    return "${LocalDate.parse(trip.startDate).format(formatter)} - ${LocalDate.parse(trip.endDate).format(formatter)}"
}

private enum class TripProgress(val label: String) {
    UPCOMING("예정"),
    IN_PROGRESS("진행 중"),
    COMPLETED("종료"),
}

private fun tripProgress(trip: Trip): TripProgress {
    val today = LocalDate.now(ZoneId.of(trip.timezone))
    return when {
        today.isBefore(LocalDate.parse(trip.startDate)) -> TripProgress.UPCOMING
        today.isAfter(LocalDate.parse(trip.endDate)) -> TripProgress.COMPLETED
        else -> TripProgress.IN_PROGRESS
    }
}

@Composable
private fun TripStatusBadge(progress: TripProgress) {
    val color = when (progress) {
        TripProgress.UPCOMING -> Teal
        TripProgress.IN_PROGRESS -> Coral
        TripProgress.COMPLETED -> Muted
    }
    Surface(shape = RoundedCornerShape(50), color = color.copy(alpha = 0.14f)) {
        Text(
            progress.label,
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp),
            color = color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
        )
    }
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
    onUpdateItem: (String, CreateItemRequest) -> Unit,
    onToggle: (ItineraryItem, Boolean) -> Unit,
    onSaveReview: (String, Int, String, List<Uri>, Set<String>) -> Unit,
    onSaveOverallReview: (Int, String, String?) -> Unit,
    onLoadGoogleCalendars: () -> Unit,
    onExportToCalendar: (Long) -> Unit,
    onClearCalendarMessage: () -> Unit,
    onInvite: (String) -> Unit,
    onUpdateTrip: (CreateTripRequest) -> Unit,
    onRefresh: () -> Unit,
) {
    val trip = state.selected ?: return
    if (tripProgress(trip) == TripProgress.COMPLETED) {
        CompletedTripAlbumScreen(
            state = state,
            onBack = onBack,
            onSaveReview = onSaveReview,
            onSaveOverallReview = onSaveOverallReview,
            onLoadGoogleCalendars = onLoadGoogleCalendars,
            onExportToCalendar = onExportToCalendar,
            onClearCalendarMessage = onClearCalendarMessage,
            onInvite = onInvite,
            onUpdateTrip = onUpdateTrip,
            onRefresh = onRefresh,
        )
        return
    }
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
    var editingItem by remember { mutableStateOf<ItineraryItem?>(null) }
    var showSharing by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
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
                    actions = {
                        CalendarExportAction(state, onLoadGoogleCalendars, onExportToCalendar, onClearCalendarMessage)
                        TextButton(onClick = { showSharing = true }) { Text("동행 ${state.members.size}", fontWeight = FontWeight.SemiBold) }
                        if (trip.owner) {
                            TextButton(onClick = { showSettings = true }) { Text("설정", fontWeight = FontWeight.SemiBold) }
                        }
                    },
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
            PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(if (showTimeline) 6.dp else 14.dp),
            ) {
                if (state.detailLoading) {
                    item(key = "detail-loading") {
                        DetailLoadingIndicator()
                    }
                }
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
                            Text(formatTripDateRange(trip), color = Color.White.copy(alpha = 0.82f))
                            Text("일정 ${trip.items.size}개 · 함께하는 사람 ${state.members.size}명", color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                item(key = "trip-cost-summary") {
                    TripCostSummary(trip.items, zoneId)
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
                                    Text("비용 · ${formatWon(current.costWon)}", color = Color.White.copy(alpha = 0.9f), fontWeight = FontWeight.Bold)
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
                    when {
                        hasCompletedItem -> Text("종료된 일정의 기억에 남은 이야기를 기록해보세요.", color = Muted)
                    }
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
                            DateCostHeader(date, dayItems)
                        }
                        items(dayItems, key = { "timeline-${it.id}" }) { timelineItem ->
                            TimelineItemCard(
                                item = timelineItem,
                                zoneId = zoneId,
                                now = now,
                                isLast = timelineItem.id == dayItems.last().id,
                                onEdit = { editingItem = timelineItem },
                            )
                        }
                    }
                } else {
                    items(trip.items, key = { it.id }) { item ->
                        ItineraryCard(
                            item, zoneId, now, onToggle,
                            scheduleProgress(item, now) == ScheduleProgress.COMPLETED,
                            state.reviews[item.id],
                            { editingItem = item },
                            { rating, content, photos, removedPhotoIds ->
                                onSaveReview(item.id, rating, content, photos, removedPhotoIds)
                            },
                        )
                    }
                }
            }
            }
        }
    }
    if (showItem) AddItemDialog(trip, { showItem = false }) { onAddItem(it); showItem = false }
    editingItem?.let { item ->
        EditItemDialog(item, zoneId, { editingItem = null }) { request ->
            onUpdateItem(item.id, request)
            editingItem = null
        }
    }
    if (showSharing) SharingDialog(state.members, canInvite, { showSharing = false }) {
        onInvite(it)
        showSharing = false
    }
    if (showSettings) TripSettingsDialog(
        trip = trip,
        busy = state.busyOperations.any { it.contains("trip-${trip.id}") },
        onDismiss = { showSettings = false },
        onUpdate = { onUpdateTrip(it); showSettings = false },
    )
}

@Composable
private fun CompletedTripAlbumScreen(
    state: TripUiState,
    onBack: () -> Unit,
    onSaveReview: (String, Int, String, List<Uri>, Set<String>) -> Unit,
    onSaveOverallReview: (Int, String, String?) -> Unit,
    onLoadGoogleCalendars: () -> Unit,
    onExportToCalendar: (Long) -> Unit,
    onClearCalendarMessage: () -> Unit,
    onInvite: (String) -> Unit,
    onUpdateTrip: (CreateTripRequest) -> Unit,
    onRefresh: () -> Unit,
) {
    val trip = state.selected ?: return
    BackHandler(onBack = onBack)
    val zoneId = remember(trip.timezone) { ZoneId.of(trip.timezone) }
    val reviewedCount = remember(trip.items, state.reviews) {
        trip.items.count { state.reviews[it.id] != null }
    }
    val albumPhotos = remember(trip.items, state.reviews) {
        trip.items.flatMap { item ->
            state.reviews[item.id]?.photos.orEmpty().map { photo -> item to photo }
        }
    }
    val groupedItems = remember(trip.items, trip.timezone) {
        trip.items.groupBy { Instant.parse(it.scheduledAt).atZone(zoneId).toLocalDate() }
    }
    val canInvite = state.members.any { it.owner && it.email == state.email }
    var editingItemId by remember(trip.id) { mutableStateOf<String?>(null) }
    var editingOverallReview by remember(trip.id) { mutableStateOf(false) }
    var showSharing by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFFFF1EE), PaleSky, Color.White))),
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = yeodamTopBarColors(),
                    title = { Text(trip.title, fontWeight = FontWeight.ExtraBold, color = Ink) },
                    navigationIcon = { TextButton(onClick = onBack) { Text("‹ 지난 여행", fontWeight = FontWeight.SemiBold) } },
                    actions = {
                        CalendarExportAction(state, onLoadGoogleCalendars, onExportToCalendar, onClearCalendarMessage)
                        TextButton(onClick = { showSharing = true }) { Text("동행 ${state.members.size}", fontWeight = FontWeight.SemiBold) }
                        if (trip.owner) {
                            TextButton(onClick = { showSettings = true }) { Text("설정", fontWeight = FontWeight.SemiBold) }
                        }
                    },
                )
            },
        ) { padding ->
            PullToRefreshBox(
                isRefreshing = state.refreshing,
                onRefresh = onRefresh,
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 40.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (state.detailLoading) {
                    item(key = "album-detail-loading") {
                        DetailLoadingIndicator()
                    }
                }
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(28.dp))
                            .background(Brush.linearGradient(listOf(Coral, Color(0xFFFFA968))))
                            .padding(22.dp),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(shape = RoundedCornerShape(50), color = Color.White.copy(alpha = 0.2f)) {
                                Text("우리의 지난 여행", modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp), color = Color.White, fontWeight = FontWeight.Bold)
                            }
                            Text(trip.destination, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            Text(formatTripDateRange(trip), color = Color.White.copy(alpha = 0.9f))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AlbumStat("후기", "$reviewedCount/${trip.items.size}")
                                AlbumStat("사진", "${albumPhotos.size}장")
                                AlbumStat("일정", "${trip.items.size}개")
                            }
                        }
                    }
                }

                item {
                    TripOverallReviewCard(
                        review = state.overallReviews[trip.id],
                        albumPhotos = albumPhotos,
                        editing = editingOverallReview,
                        onEdit = { editingOverallReview = !editingOverallReview },
                        onSave = { rating, content, representativePhotoId ->
                            onSaveOverallReview(rating, content, representativePhotoId)
                            editingOverallReview = false
                        },
                    )
                }

                item(key = "album-cost-summary") {
                    TripCostSummary(trip.items, zoneId)
                }

                item {
                    Text("여행 사진", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Ink)
                    Text("남긴 사진을 한곳에 모았어요.", color = Muted)
                }

                if (albumPhotos.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Border),
                        ) {
                            Column(Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("아직 앨범에 사진이 없어요", fontWeight = FontWeight.Bold, color = Ink)
                                Text("아래 일정의 후기에 사진을 추가해보세요.", color = Muted)
                            }
                        }
                    }
                } else {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(albumPhotos, key = { it.second.id }) { (item, photo) ->
                                Column(Modifier.width(164.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                    AlbumPhoto(
                                        itemId = item.id,
                                        photoId = photo.id,
                                        contentDescription = photo.originalName,
                                        modifier = Modifier.fillMaxWidth().height(112.dp),
                                    )
                                    Text(item.title, maxLines = 1, color = Ink, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(Modifier.height(2.dp))
                    Text("날짜별 여담", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Ink)
                    Text("기억에 남은 이야기를 완성해보세요.", color = Muted)
                }

                if (trip.items.isEmpty()) {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(22.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, Border),
                        ) {
                            Text("등록된 일정이 없는 여행입니다.", modifier = Modifier.padding(24.dp), color = Muted)
                        }
                    }
                }

                groupedItems.forEach { (date, dayItems) ->
                    item(key = "album-date-$date") {
                        DateCostHeader(date, dayItems)
                    }
                    items(dayItems, key = { "album-item-${it.id}" }) { item ->
                        AlbumReviewCard(
                            item = item,
                            zoneId = zoneId,
                            review = state.reviews[item.id],
                            editing = editingItemId == item.id,
                            onEdit = { editingItemId = if (editingItemId == item.id) null else item.id },
                            onSaveReview = { rating, content, photos, removedPhotoIds ->
                                onSaveReview(item.id, rating, content, photos, removedPhotoIds)
                                editingItemId = null
                            },
                        )
                    }
                }
            }
            }
        }
    }
    if (showSharing) SharingDialog(state.members, canInvite, { showSharing = false }) {
        onInvite(it)
        showSharing = false
    }
    if (showSettings) TripSettingsDialog(
        trip = trip,
        busy = state.busyOperations.any { it.contains("trip-${trip.id}") },
        onDismiss = { showSettings = false },
        onUpdate = { onUpdateTrip(it); showSettings = false },
    )
}

@Composable
private fun TripOverallReviewCard(
    review: TripOverallReview?,
    albumPhotos: List<Pair<ItineraryItem, ReviewPhoto>>,
    editing: Boolean,
    onEdit: () -> Unit,
    onSave: (Int, String, String?) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Coral.copy(alpha = 0.35f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("여행 전체 여담", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Teal)
                    Text("여행 전체의 기억을 한 번 더 남겨보세요.", color = Muted, style = MaterialTheme.typography.bodySmall)
                }
                if (review != null && !editing) {
                    TextButton(onClick = onEdit) { Text("수정", color = Teal, fontWeight = FontWeight.Bold) }
                }
            }
            when {
                editing -> TripOverallReviewEditor(review, albumPhotos, onSave, onEdit)
                review == null -> {
                    Text("별점과 이야기, 대표 사진을 선택할 수 있어요.", color = Muted)
                    Button(onClick = onEdit, modifier = Modifier.align(Alignment.End), shape = RoundedCornerShape(14.dp)) {
                        Text("전체 후기 작성")
                    }
                }
                else -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        repeat(5) { index -> Text(if (index < review.rating) "★" else "☆", color = Coral, fontSize = 24.sp) }
                    }
                    if (review.content.isNotBlank()) Text(review.content, color = Ink, lineHeight = 21.sp)
                    review.representativePhoto?.let { photo ->
                        AlbumPhoto(
                            itemId = photo.itemId,
                            photoId = photo.id,
                            contentDescription = photo.originalName,
                            modifier = Modifier.fillMaxWidth().height(180.dp),
                        )
                        Text("여행 목록 대표 사진", color = Teal, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun TripOverallReviewEditor(
    review: TripOverallReview?,
    albumPhotos: List<Pair<ItineraryItem, ReviewPhoto>>,
    onSave: (Int, String, String?) -> Unit,
    onCancel: () -> Unit,
) {
    var rating by remember(review) { mutableIntStateOf(review?.rating ?: 5) }
    var ratingTouched by remember(review) { mutableStateOf(false) }
    var content by remember(review) { mutableStateOf(review?.content.orEmpty()) }
    var selectedPhotoId by remember(review) { mutableStateOf(review?.representativePhoto?.id) }
    val hasChanges = if (review == null) {
        ratingTouched || content.isNotBlank() || selectedPhotoId != null
    } else {
        rating != review.rating || content != review.content || selectedPhotoId != review.representativePhoto?.id
    }
    HorizontalDivider(color = Border)
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        (1..5).forEach { score ->
            TextButton(
                onClick = { rating = score; ratingTouched = true },
                modifier = Modifier.size(40.dp),
                contentPadding = PaddingValues(0.dp),
            ) { Text(if (score <= rating) "★" else "☆", color = Coral, fontSize = 26.sp) }
        }
    }
    Field(content, { content = it }, "여행 전체에서 기억하고 싶은 점")
    Text("대표 사진", color = Ink, fontWeight = FontWeight.Bold)
    if (albumPhotos.isEmpty()) {
        Text("일정별 후기에 사진을 추가하면 대표 사진으로 선택할 수 있어요.", color = Muted, style = MaterialTheme.typography.bodySmall)
    } else {
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(albumPhotos, key = { "overall-photo-${it.second.id}" }) { (item, photo) ->
                Card(
                    onClick = { selectedPhotoId = if (selectedPhotoId == photo.id) null else photo.id },
                    modifier = Modifier.width(112.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(if (selectedPhotoId == photo.id) 3.dp else 1.dp, if (selectedPhotoId == photo.id) Coral else Border),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                ) {
                    Column {
                        AlbumPhoto(
                            itemId = item.id,
                            photoId = photo.id,
                            contentDescription = photo.originalName,
                            modifier = Modifier.fillMaxWidth().height(82.dp),
                        )
                        Text(item.title, modifier = Modifier.padding(7.dp), maxLines = 1, color = Ink, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
        TextButton(onClick = onCancel) { Text("취소", color = Muted) }
        Button(
            onClick = { onSave(rating, content, selectedPhotoId) },
            enabled = hasChanges,
            shape = RoundedCornerShape(14.dp),
        ) { Text("전체 후기 저장") }
    }
}

@Composable
private fun RowScope.AlbumStat(label: String, value: String) {
    Surface(
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.18f),
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = Color.White, fontWeight = FontWeight.ExtraBold)
            Text(label, color = Color.White.copy(alpha = 0.82f), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun AlbumReviewCard(
    item: ItineraryItem,
    zoneId: ZoneId,
    review: Review?,
    editing: Boolean,
    onEdit: () -> Unit,
    onSaveReview: (Int, String, List<Uri>, Set<String>) -> Unit,
) {
    val category = itineraryCategoryStyle(item.category)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = category.containerColor),
        border = BorderStroke(1.dp, if (review == null) Coral.copy(alpha = 0.4f) else Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(itineraryTimeRange(item, zoneId), color = category.accentColor, fontWeight = FontWeight.ExtraBold)
                        CategoryBadge(category)
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = (if (review == null) Coral else Leaf).copy(alpha = 0.14f),
                    ) {
                        Text(
                            if (review == null) "후기 필요" else "후기 완료",
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                            color = if (review == null) Coral else Leaf,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                Spacer(Modifier.height(5.dp))
                Text(item.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = Ink)
                item.place?.let { Text("장소 · $it", color = Muted) }
                Text("비용 · ${formatWon(item.costWon)}", color = category.accentColor, fontWeight = FontWeight.Bold)
                item.memo?.let { Text(it, color = Muted, style = MaterialTheme.typography.bodySmall) }
            }

            if (editing) {
                HorizontalDivider(color = Border)
                ReviewEditor(item.id, review, onSaveReview)
                TextButton(onClick = onEdit, modifier = Modifier.align(Alignment.End).padding(end = 10.dp, bottom = 6.dp)) {
                    Text("작성 닫기", color = Muted)
                }
            } else if (review == null) {
                Surface(
                    modifier = Modifier.padding(horizontal = 18.dp),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFFFF1EE),
                ) {
                    Text("이 일정에서 기억하고 싶은 순간을 남겨보세요.", modifier = Modifier.padding(14.dp), color = Muted)
                }
                Button(
                    onClick = onEdit,
                    modifier = Modifier.align(Alignment.End).padding(end = 18.dp, bottom = 18.dp),
                    shape = RoundedCornerShape(14.dp),
                ) { Text("후기 작성") }
            } else {
                Row(Modifier.padding(horizontal = 18.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(5) { index -> Text(if (index < review.rating) "★" else "☆", color = Coral, fontSize = 22.sp) }
                }
                if (review.content.isNotBlank()) {
                    Text(review.content, modifier = Modifier.padding(horizontal = 18.dp), color = Ink, lineHeight = 21.sp)
                }
                if (review.photos.isNotEmpty()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 18.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(review.photos, key = { it.id }) { photo ->
                            AlbumPhoto(
                                itemId = item.id,
                                photoId = photo.id,
                                contentDescription = photo.originalName,
                                modifier = Modifier.size(92.dp),
                            )
                        }
                    }
                }
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.align(Alignment.End).padding(end = 18.dp, bottom = 18.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Teal),
                ) { Text("후기 수정") }
            }
        }
    }
}

@Composable
private fun AlbumPhoto(
    itemId: String,
    photoId: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier, shape = RoundedCornerShape(16.dp), color = Sky) {
        RemoteReviewPhoto(itemId, photoId, contentDescription, Modifier.fillMaxSize())
    }
}

@Composable
private fun RemoteReviewPhoto(
    itemId: String,
    photoId: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val token = remember { TokenStore(context).accessToken }
    val request = remember(itemId, photoId, token) {
        val cacheKey = "review-photo:$photoId:${token?.hashCode() ?: 0}"
        ImageRequest.Builder(context)
            .data("${BuildConfig.API_BASE_URL}api/v1/items/$itemId/review/photos/$photoId/content")
            .memoryCacheKey(cacheKey)
            .diskCacheKey(cacheKey)
            .headers(
                Headers.Builder().apply {
                    token?.let { add("Authorization", "Bearer $it") }
                }.build(),
            )
            .crossfade(true)
            .build()
    }
    AsyncImage(
        model = request,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier,
    )
}

@Composable
private fun CalendarExportAction(
    state: TripUiState,
    onLoadCalendars: () -> Unit,
    onExport: (Long) -> Unit,
    onClearMessage: () -> Unit,
) {
    val context = LocalContext.current
    var showCalendars by remember { mutableStateOf(false) }
    var permissionDenied by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.all { it }) {
            onLoadCalendars()
            showCalendars = true
        } else {
            permissionDenied = true
        }
    }
    val openPicker = {
        val permissions = arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR)
        if (permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
            onLoadCalendars()
            showCalendars = true
        } else {
            permissionLauncher.launch(permissions)
        }
    }
    val calendarBusy = state.busyOperations.any { it.startsWith("calendar-") }
    TextButton(onClick = openPicker, enabled = !calendarBusy) {
        if (calendarBusy) CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
        else Text("내보내기", fontWeight = FontWeight.SemiBold)
    }
    if (showCalendars) {
        AlertDialog(
            onDismissRequest = { showCalendars = false },
            shape = RoundedCornerShape(24.dp),
            title = { Text("Google 캘린더 선택", color = Teal, fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.googleCalendars.isEmpty()) {
                        Text("동기화 가능한 Google 캘린더를 불러오는 중이거나 등록된 캘린더가 없습니다.", color = Muted)
                    }
                    state.googleCalendars.forEach { calendar ->
                        OutlinedButton(
                            onClick = { onExport(calendar.id); showCalendars = false },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Column(Modifier.fillMaxWidth()) {
                                Text(calendar.name, color = Ink, fontWeight = FontWeight.Bold)
                                Text(calendar.accountName, color = Muted, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showCalendars = false }) { Text("닫기") } },
        )
    }
    if (permissionDenied) {
        AlertDialog(
            onDismissRequest = { permissionDenied = false },
            title = { Text("캘린더 권한 필요") },
            text = { Text("여담 일정을 Google 캘린더에 내보내려면 캘린더 읽기·쓰기 권한이 필요합니다.") },
            confirmButton = { TextButton(onClick = { permissionDenied = false }) { Text("확인") } },
        )
    }
    state.calendarExportMessage?.let { message ->
        AlertDialog(
            onDismissRequest = onClearMessage,
            title = { Text("Google 캘린더") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = onClearMessage) { Text("확인") } },
        )
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

private fun formatWon(amount: Long): String =
    "${WonNumberFormat.format(amount)}원"

@Composable
private fun TripCostSummary(itineraryItems: List<ItineraryItem>, zoneId: ZoneId) {
    val dailyCosts = remember(itineraryItems, zoneId) {
        itineraryItems
            .groupBy { Instant.parse(it.scheduledAt).atZone(zoneId).toLocalDate() }
            .mapValues { (_, items) -> items.sumOf(ItineraryItem::costWon) }
            .toSortedMap()
            .toList()
    }
    val totalCost = remember(itineraryItems) { itineraryItems.sumOf(ItineraryItem::costWon) }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, Border),
        shadowElevation = 1.dp,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("여행 비용", color = Ink, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                Text(formatWon(totalCost), color = Coral, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            }
            if (dailyCosts.isEmpty()) {
                Text("일정을 추가하면 날짜별 비용을 확인할 수 있어요.", color = Muted, style = MaterialTheme.typography.bodySmall)
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(dailyCosts, key = { it.first.toString() }) { (date, cost) ->
                        Surface(shape = RoundedCornerShape(13.dp), color = PaleSky) {
                            Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(date.format(DateTimeFormatter.ofPattern("M월 d일")), color = Muted, style = MaterialTheme.typography.labelSmall)
                                Text(formatWon(cost), color = Teal, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DateCostHeader(date: LocalDate, dayItems: List<ItineraryItem>) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            date.format(DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN)),
            color = Teal,
            fontWeight = FontWeight.ExtraBold,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        Text(formatWon(dayItems.sumOf(ItineraryItem::costWon)), color = Coral, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun TimelineItemCard(
    item: ItineraryItem,
    zoneId: ZoneId,
    now: Instant,
    isLast: Boolean,
    onEdit: () -> Unit,
) {
    val progress = scheduleProgress(item, now)
    val category = itineraryCategoryStyle(item.category)
    val accentColor = when (progress) {
        ScheduleProgress.UPCOMING -> Teal
        ScheduleProgress.IN_PROGRESS -> Coral
        ScheduleProgress.COMPLETED -> Leaf
    }
    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        Column(Modifier.width(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(16.dp).clip(CircleShape).background(accentColor))
            if (!isLast) Box(Modifier.width(2.dp).weight(1f).background(Border))
        }
        Card(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 3.dp)
                .clickable(enabled = progress == ScheduleProgress.UPCOMING, onClick = onEdit),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = category.containerColor),
            border = BorderStroke(if (progress == ScheduleProgress.IN_PROGRESS) 1.5.dp else 1.dp, if (progress == ScheduleProgress.IN_PROGRESS) Coral else Border),
            elevation = CardDefaults.cardElevation(defaultElevation = if (progress == ScheduleProgress.IN_PROGRESS) 3.dp else 0.dp),
        ) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    itineraryTimeRange(item, zoneId),
                    color = accentColor,
                    fontWeight = FontWeight.ExtraBold,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.width(96.dp),
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CategoryBadge(category, compact = true)
                        Text(
                            item.title,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = Ink,
                        )
                    }
                    item.place?.let {
                        Text("장소 · $it", maxLines = 1, color = Muted, style = MaterialTheme.typography.labelSmall)
                    }
                    Text("비용 · ${formatWon(item.costWon)}", color = category.accentColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                }
                Surface(shape = RoundedCornerShape(50), color = accentColor.copy(alpha = 0.14f)) {
                    Text(
                        if (progress == ScheduleProgress.IN_PROGRESS) "진행 중" else progress.label,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        color = accentColor,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
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
    onEdit: () -> Unit,
    onSaveReview: (Int, String, List<Uri>, Set<String>) -> Unit,
) {
    val progress = scheduleProgress(item, now)
    val category = itineraryCategoryStyle(item.category)
    val dateLabel = Instant.parse(item.scheduledAt).atZone(zoneId).format(DateTimeFormatter.ofPattern("M월 d일 (E)", Locale.KOREAN))
    val progressColor = when (progress) {
        ScheduleProgress.UPCOMING -> Teal
        ScheduleProgress.IN_PROGRESS -> Coral
        ScheduleProgress.COMPLETED -> Leaf
    }
    Card(
        Modifier
            .fillMaxWidth()
            .clickable(enabled = progress == ScheduleProgress.UPCOMING, onClick = onEdit),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = category.containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        border = BorderStroke(1.dp, Border.copy(alpha = 0.7f)),
    ) {
        Column {
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(width = 5.dp, height = 54.dp).clip(RoundedCornerShape(50)).background(category.accentColor))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CategoryBadge(category)
                        Surface(shape = RoundedCornerShape(50), color = progressColor.copy(alpha = 0.14f)) {
                            Text(progress.label, modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp), color = progressColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(Modifier.height(5.dp))
                    Text(item.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Ink)
                    Text("$dateLabel · ${itineraryTimeRange(item, zoneId)}", style = MaterialTheme.typography.bodyMedium, color = Muted)
                    item.place?.let { Text("장소 · $it", color = Muted, style = MaterialTheme.typography.bodySmall) }
                    Text("비용 · ${formatWon(item.costWon)}", color = category.accentColor, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    if (item.notificationEnabled) {
                        Text("${item.notificationMinutesBefore}분 전 알려드려요", color = Coral, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    } else {
                        Text("알림 꺼짐", color = Muted, style = MaterialTheme.typography.labelMedium)
                    }
                }
                YeodamNotificationSwitch(
                    checked = item.notificationEnabled,
                    onCheckedChange = { onToggle(item, it) },
                )
            }
            if (reviewEnabled) {
                HorizontalDivider(color = Border)
                ReviewEditor(item.id, review, onSaveReview)
            }
        }
    }
}

@Composable
private fun CategoryBadge(category: ItineraryCategoryStyle, compact: Boolean = false) {
    Surface(shape = RoundedCornerShape(50), color = category.accentColor.copy(alpha = 0.15f)) {
        Text(
            category.label,
            modifier = Modifier.padding(horizontal = if (compact) 6.dp else 9.dp, vertical = if (compact) 2.dp else 3.dp),
            color = category.accentColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

@Composable
private fun DetailLoadingIndicator() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, Border),
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Teal, strokeWidth = 2.dp)
            Text("후기와 동행 정보를 불러오는 중이에요", color = Muted, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun CreateTripDialog(onDismiss: () -> Unit, onSave: (CreateTripRequest) -> Unit) {
    var title by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    val tomorrow = remember { LocalDate.now().plusDays(1) }
    var start by remember { mutableStateOf(tomorrow) }
    var end by remember { mutableStateOf(tomorrow.plusDays(1)) }
    InputDialog("새 여행", onDismiss, title.isNotBlank() && destination.isNotBlank() && !end.isBefore(start), {
        onSave(CreateTripRequest(title, destination, start.toString(), end.toString()))
    }) {
        Field(title, { title = it }, "여행 이름")
        Field(destination, { destination = it }, "목적지")
        CalendarDateField("시작일", start) { selected ->
            start = selected
            if (end.isBefore(selected)) end = selected
        }
        CalendarDateField("종료일", end) { selected -> end = maxOf(selected, start) }
    }
}

@Composable
private fun TripSettingsDialog(
    trip: Trip,
    busy: Boolean,
    onDismiss: () -> Unit,
    onUpdate: (CreateTripRequest) -> Unit,
) {
    var title by remember(trip.id) { mutableStateOf(trip.title) }
    var destination by remember(trip.id) { mutableStateOf(trip.destination) }
    var start by remember(trip.id) { mutableStateOf(LocalDate.parse(trip.startDate)) }
    var end by remember(trip.id) { mutableStateOf(LocalDate.parse(trip.endDate)) }
    val changed = title != trip.title || destination != trip.destination ||
        start.toString() != trip.startDate || end.toString() != trip.endDate
    val canSave = trip.owner && changed && title.isNotBlank() && destination.isNotBlank() && !end.isBefore(start) && !busy

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = { Text("여행 설정", color = Teal, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (trip.owner) {
                    Field(title, { title = it }, "여행 이름")
                    Field(destination, { destination = it }, "목적지")
                    CalendarDateField("시작일", start) { selected ->
                        start = selected
                        if (end.isBefore(selected)) end = selected
                    }
                    CalendarDateField("종료일", end) { end = maxOf(it, start) }
                    Button(
                        onClick = {
                            onUpdate(CreateTripRequest(title, destination, start.toString(), end.toString(), trip.timezone))
                        },
                        enabled = canSave,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (busy) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        else Text("여행 정보 저장")
                    }
                } else {
                    Text("여행 정보는 여행을 만든 사람만 수정할 수 있어요.", color = Muted)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("닫기") } },
    )
}

@Composable
private fun CalendarDateField(label: String, date: LocalDate, onDateSelected: (LocalDate) -> Unit) {
    var showPicker by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            modifier = Modifier.padding(start = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = Muted,
        )
        OutlinedButton(
            onClick = { showPicker = true },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Border),
            colors = ButtonDefaults.outlinedButtonColors(containerColor = FieldBackground),
            contentPadding = PaddingValues(horizontal = 16.dp),
        ) {
            Text(
                date.format(DateTimeFormatter.ofPattern("yyyy년 M월 d일 (E)", Locale.KOREAN)),
                modifier = Modifier.weight(1f),
                color = Ink,
            )
            Icon(Icons.Outlined.DateRange, contentDescription = "날짜 선택", tint = Teal)
        }
    }
    if (showPicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            onDateSelected(Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate())
                        }
                        showPicker = false
                    },
                ) { Text("선택") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("취소") } },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun AddItemDialog(trip: Trip, onDismiss: () -> Unit, onSave: (CreateItemRequest) -> Unit) {
    val initialWindow = remember(trip.id, trip.items) { nextItineraryWindow(trip) }
    val zoneId = remember(trip.timezone) { ZoneId.of(trip.timezone) }
    val initialStart = remember(initialWindow.first) { Instant.parse(initialWindow.first).atZone(zoneId) }
    val initialEnd = remember(initialWindow.second) { Instant.parse(initialWindow.second).atZone(zoneId) }
    var title by remember { mutableStateOf("") }
    var place by remember { mutableStateOf("") }
    var memo by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("OTHER") }
    var cost by remember { mutableStateOf("") }
    var startDate by remember { mutableStateOf(initialStart.toLocalDate().toString()) }
    var startTime by remember { mutableStateOf(initialStart.toLocalTime().withSecond(0).withNano(0)) }
    var endDate by remember { mutableStateOf(initialEnd.toLocalDate().toString()) }
    var endTime by remember { mutableStateOf(initialEnd.toLocalTime().withSecond(0).withNano(0)) }
    var before by remember { mutableStateOf("30") }
    val startsAt = itineraryInstant(startDate, startTime, zoneId)
    val endsAt = itineraryInstant(endDate, endTime, zoneId)
    val validWindow = startsAt != null && endsAt != null && Instant.parse(endsAt).isAfter(Instant.parse(startsAt))
    val beforeMinutes = before.toIntOrNull()
    val validNotification = beforeMinutes != null && beforeMinutes in 0..10080
    val costWon = cost.toLongOrNull() ?: 0
    val validCost = cost.isBlank() || cost.toLongOrNull() != null
    InputDialog("일정 추가", onDismiss, title.isNotBlank() && validWindow && validNotification && validCost, {
        onSave(
            CreateItemRequest(
                title = title,
                place = place.ifBlank { null },
                memo = memo.ifBlank { null },
                scheduledAt = startsAt!!,
                endsAt = endsAt!!,
                notificationMinutesBefore = beforeMinutes ?: 0,
                category = category,
                costWon = costWon,
            ),
        )
    }) {
        Field(title, { title = it }, "일정 이름")
        Field(place, { place = it }, "장소 (선택)")
        Field(memo, { memo = it }, "메모 (선택)")
        CategorySelector(category) { category = it }
        CostField(cost) { cost = it }
        ScheduleDateTimeFields("시작", startDate, { startDate = it }, startTime, { startTime = it })
        ScheduleDateTimeFields("종료", endDate, { endDate = it }, endTime, { endTime = it })
        MinuteBeforeField(before) { before = it }
    }
}

@Composable
private fun EditItemDialog(
    item: ItineraryItem,
    zoneId: ZoneId,
    onDismiss: () -> Unit,
    onSave: (CreateItemRequest) -> Unit,
) {
    val initialStart = remember(item.id) { Instant.parse(item.scheduledAt).atZone(zoneId) }
    val initialEnd = remember(item.id) { Instant.parse(item.endsAt).atZone(zoneId) }
    var title by remember(item.id) { mutableStateOf(item.title) }
    var place by remember(item.id) { mutableStateOf(item.place.orEmpty()) }
    var memo by remember(item.id) { mutableStateOf(item.memo.orEmpty()) }
    var category by remember(item.id) { mutableStateOf(item.category) }
    var cost by remember(item.id) { mutableStateOf(item.costWon.takeIf { it > 0 }?.toString().orEmpty()) }
    var startDate by remember(item.id) { mutableStateOf(initialStart.toLocalDate().toString()) }
    var startTime by remember(item.id) { mutableStateOf(initialStart.toLocalTime().withSecond(0).withNano(0)) }
    var endDate by remember(item.id) { mutableStateOf(initialEnd.toLocalDate().toString()) }
    var endTime by remember(item.id) { mutableStateOf(initialEnd.toLocalTime().withSecond(0).withNano(0)) }
    var notificationEnabled by remember(item.id) { mutableStateOf(item.notificationEnabled) }
    var before by remember(item.id) { mutableStateOf(item.notificationMinutesBefore.toString()) }
    val startsAt = itineraryInstant(startDate, startTime, zoneId)
    val endsAt = itineraryInstant(endDate, endTime, zoneId)
    val costWon = cost.toLongOrNull() ?: 0
    val hasChanges = title != item.title ||
        place != item.place.orEmpty() ||
        memo != item.memo.orEmpty() ||
        category != item.category ||
        costWon != item.costWon ||
        startsAt != item.scheduledAt ||
        endsAt != item.endsAt ||
        notificationEnabled != item.notificationEnabled ||
        before.toIntOrNull() != item.notificationMinutesBefore
    val validWindow = startsAt != null && endsAt != null && Instant.parse(endsAt).isAfter(Instant.parse(startsAt))
    val beforeMinutes = before.toIntOrNull()
    val validNotification = beforeMinutes != null && beforeMinutes in 0..10080
    val validCost = cost.isBlank() || cost.toLongOrNull() != null
    val canSave = title.isNotBlank() && validWindow && validNotification && validCost && hasChanges

    InputDialog("일정 수정", onDismiss, canSave, {
        onSave(
            CreateItemRequest(
                title = title,
                place = place.ifBlank { null },
                memo = memo.ifBlank { null },
                scheduledAt = startsAt!!,
                endsAt = endsAt!!,
                notificationEnabled = notificationEnabled,
                notificationMinutesBefore = beforeMinutes ?: 0,
                category = category,
                costWon = costWon,
            ),
        )
    }) {
        Field(title, { title = it }, "일정 이름")
        Field(place, { place = it }, "장소 (선택)")
        Field(memo, { memo = it }, "메모 (선택)")
        CategorySelector(category) { category = it }
        CostField(cost) { cost = it }
        ScheduleDateTimeFields("시작", startDate, { startDate = it }, startTime, { startTime = it })
        ScheduleDateTimeFields("종료", endDate, { endDate = it }, endTime, { endTime = it })
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("일정 알림", modifier = Modifier.weight(1f), color = Ink, fontWeight = FontWeight.SemiBold)
            YeodamNotificationSwitch(checked = notificationEnabled, onCheckedChange = { notificationEnabled = it })
        }
        MinuteBeforeField(before) { before = it }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategorySelector(selected: String, onSelected: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(
            "카테고리",
            modifier = Modifier.padding(start = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = Muted,
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            ItineraryCategoryStyles.forEach { category ->
                FilterChip(
                    selected = selected == category.value,
                    onClick = { onSelected(category.value) },
                    label = { Text(category.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = Color.White,
                        selectedContainerColor = category.containerColor,
                        selectedLabelColor = category.accentColor,
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selected == category.value,
                        borderColor = Border,
                        selectedBorderColor = category.accentColor,
                    ),
                )
            }
        }
    }
}

@Composable
private fun CostField(value: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            "비용 (선택)",
            modifier = Modifier.padding(start = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = Muted,
        )
        OutlinedTextField(
            value = value,
            onValueChange = { input -> onChange(input.filter(Char::isDigit).take(12)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            suffix = { Text("원", color = Muted) },
            placeholder = { Text("0", color = Muted) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
            shape = RoundedCornerShape(16.dp),
            colors = yeodamFieldColors(),
        )
    }
}

@Composable
private fun YeodamNotificationSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        thumbContent = null,
        colors = SwitchDefaults.colors(
            checkedThumbColor = Color.White,
            checkedTrackColor = Teal,
            checkedBorderColor = Teal,
            uncheckedThumbColor = Muted,
            uncheckedTrackColor = FieldBackground,
            uncheckedBorderColor = Border,
        ),
    )
}

private fun itineraryInstant(date: String, time: LocalTime, zoneId: ZoneId): String? = runCatching {
    LocalDate.parse(date).atTime(time).atZone(zoneId).toInstant().toString()
}.getOrNull()

@Composable
private fun ScheduleDateTimeFields(
    label: String,
    date: String,
    onDateChange: (String) -> Unit,
    time: LocalTime,
    onTimeChange: (LocalTime) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, modifier = Modifier.padding(start = 6.dp), color = Teal, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ScheduleDateField(date, onDateChange, Modifier.weight(1.6f))
            TimeSpinnerField(time, onTimeChange, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ScheduleDateField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    var showPicker by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.length <= 10) onChange(it) },
        modifier = modifier,
        singleLine = true,
        placeholder = { Text("YYYY-MM-DD") },
        trailingIcon = {
            IconButton(onClick = { showPicker = true }) {
                Icon(Icons.Outlined.DateRange, contentDescription = "날짜 선택", tint = Teal)
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
        shape = RoundedCornerShape(16.dp),
        colors = yeodamFieldColors(),
    )
    if (showPicker) {
        val initialDate = runCatching { LocalDate.parse(value) }.getOrElse { LocalDate.now() }
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let {
                        onChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toString())
                    }
                    showPicker = false
                }) { Text("선택") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("취소") } },
        ) { DatePicker(state = pickerState) }
    }
}

@Composable
private fun TimeSpinnerField(time: LocalTime, onTimeChange: (LocalTime) -> Unit, modifier: Modifier = Modifier) {
    var showPicker by remember { mutableStateOf(false) }
    OutlinedButton(
        onClick = { showPicker = true },
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Border),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = FieldBackground),
    ) { Text(time.format(DateTimeFormatter.ofPattern("HH:mm")), color = Ink, fontWeight = FontWeight.Bold) }
    if (showPicker) {
        var hour by remember(time, showPicker) { mutableIntStateOf(time.hour) }
        var minute by remember(time, showPicker) { mutableIntStateOf(time.minute) }
        AlertDialog(
            onDismissRequest = { showPicker = false },
            title = { Text("시간 선택", color = Teal, fontWeight = FontWeight.ExtraBold) },
            text = {
                AndroidView(
                    modifier = Modifier.fillMaxWidth().height(180.dp),
                    factory = { context ->
                        LinearLayout(context).apply {
                            orientation = LinearLayout.HORIZONTAL
                            gravity = android.view.Gravity.CENTER
                            addView(NumberPicker(context).apply {
                                minValue = 0
                                maxValue = 23
                                value = hour
                                wrapSelectorWheel = true
                                setFormatter { "%02d".format(it) }
                                setOnValueChangedListener { _, _, newValue -> hour = newValue }
                            })
                            addView(NumberPicker(context).apply {
                                minValue = 0
                                maxValue = 59
                                value = minute
                                wrapSelectorWheel = true
                                setFormatter { "%02d".format(it) }
                                setOnValueChangedListener { _, _, newValue -> minute = newValue }
                            })
                        }
                    },
                )
            },
            confirmButton = {
                TextButton(onClick = { onTimeChange(LocalTime.of(hour, minute)); showPicker = false }) { Text("선택") }
            },
            dismissButton = { TextButton(onClick = { showPicker = false }) { Text("취소") } },
        )
    }
}

@Composable
private fun MinuteBeforeField(value: String, onChange: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("알림", modifier = Modifier.padding(start = 6.dp), style = MaterialTheme.typography.labelMedium, color = Muted)
        OutlinedTextField(
            value = value,
            onValueChange = { if (it.all(Char::isDigit)) onChange(it) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            suffix = { Text("분 전", color = Muted) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(16.dp),
            colors = yeodamFieldColors(),
        )
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
    itemId: String,
    review: Review?,
    onSave: (Int, String, List<Uri>, Set<String>) -> Unit,
) {
    var rating by remember(review) { mutableIntStateOf(review?.rating ?: 5) }
    var content by remember(review) { mutableStateOf(review?.content ?: "") }
    var selectedPhotos by remember(review?.updatedAt, review?.photos?.size) { mutableStateOf<List<Uri>>(emptyList()) }
    var removedPhotoIds by remember(review?.updatedAt, review?.photos?.size) { mutableStateOf<Set<String>>(emptySet()) }
    var ratingTouched by remember(review) { mutableStateOf(false) }
    val visiblePhotos = review?.photos.orEmpty().filterNot { it.id in removedPhotoIds }
    val existingCount = visiblePhotos.size
    val hasChanges = if (review == null) {
        ratingTouched || content.isNotBlank() || selectedPhotos.isNotEmpty()
    } else {
        rating != review.rating ||
            content != review.content ||
            selectedPhotos.isNotEmpty() ||
            removedPhotoIds.isNotEmpty()
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        selectedPhotos = (selectedPhotos + uris).distinct().take(5 - existingCount)
    }
    Column(Modifier.background(PaleSky).padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("이 계획의 여담", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = Teal)
        Text("그날의 기분과 기억을 계획별로 남겨보세요.", color = Muted, style = MaterialTheme.typography.bodySmall)
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            (1..5).forEach { score ->
                TextButton(
                    onClick = {
                        rating = score
                        ratingTouched = true
                    },
                    modifier = Modifier.size(40.dp),
                    contentPadding = PaddingValues(0.dp),
                ) { Text(if (score <= rating) "★" else "☆", color = Coral, fontSize = 26.sp) }
            }
        }
        Field(content, { content = it }, "이 계획에서 기억하고 싶은 점")
        visiblePhotos.forEach { photo ->
            ReviewPhotoRow(
                itemId = itemId,
                photoId = photo.id,
                name = photo.originalName,
                onRemove = { removedPhotoIds = removedPhotoIds + photo.id },
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
            onClick = { onSave(rating, content, selectedPhotos, removedPhotoIds) },
            enabled = hasChanges,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.align(Alignment.End),
        ) { Text("후기 저장") }
    }
}

@Composable
private fun ReviewPhotoRow(itemId: String, photoId: String, name: String, onRemove: () -> Unit) {
    Surface(shape = RoundedCornerShape(14.dp), color = Color.White, border = BorderStroke(1.dp, Border)) {
        Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RemoteReviewPhoto(
                itemId = itemId,
                photoId = photoId,
                contentDescription = name,
                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(10.dp)),
            )
            Text(name, modifier = Modifier.weight(1f), maxLines = 1, color = Ink)
            TextButton(onClick = onRemove) { Text("삭제", color = Coral) }
        }
    }
}

@Composable
private fun SelectedPhotoRow(uri: Uri, onRemove: () -> Unit) {
    val context = LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(null, uri) {
        value = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, BitmapFactory.Options().apply { inSampleSize = 4 })
            }
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
        text = {
            Column(
                modifier = Modifier.heightIn(max = 560.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = content,
            )
        },
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
private fun ErrorSnackbar(
    message: String,
    canRetry: Boolean,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.BottomCenter) {
        Snackbar(
            containerColor = Ink,
            contentColor = Color.White,
            shape = RoundedCornerShape(16.dp),
            action = if (canRetry) {
                { TextButton(onClick = onRetry) { Text("다시 시도", color = Sky, fontWeight = FontWeight.Bold) } }
            } else null,
            dismissAction = {
                TextButton(onClick = onDismiss) { Text("닫기", color = Color.White) }
            },
        ) { Text(message) }
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
