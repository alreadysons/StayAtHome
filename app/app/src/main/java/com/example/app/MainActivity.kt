package com.example.app

import android.Manifest
import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.app.ui.theme.AppTheme
import com.example.app.viewmodel.WifiUiState
import com.example.app.viewmodel.ViewModel
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.app.worker.WifiMonitorWorker
import com.example.app.data.WeeklyStatsResponse
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.concurrent.TimeUnit
import com.example.app.ui.components.SimpleLineChart

class MainActivity : ComponentActivity() {

    private val viewModel: ViewModel by viewModels()

    // ✅ [수정] 권한 거부 시 사용자에게 피드백을 제공하도록 수정
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // 권한을 허용하면 Wi-Fi 정보 가져오기 시도
                viewModel.getWifiInfo(this)
            } else {
                // 권한 거부 시 사용자에게 알림
                Toast.makeText(this, "Wi-Fi 정보를 얻으려면 위치 권한이 필요합니다.", Toast.LENGTH_LONG).show()
            }
        }

    @RequiresApi(Build.VERSION_CODES.O)
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 앱이 켜지자마자 위치 권한 요청
        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)

        setContent {
            AppTheme {
                val uiState by viewModel.uiState.collectAsState()
                var currentTab by remember { mutableStateOf(BottomTab.Home) }

                Surface(modifier = Modifier.fillMaxSize()) {
                    // 배경색 (Beige)
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFF5F5DC))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Header
                            TopAppBar(
                                title = {
                                    Text(
                                        when (currentTab) {
                                            BottomTab.Home -> "Stayathome"
                                            BottomTab.Stats -> "통계"
                                            BottomTab.Settings -> "설정"
                                        }
                                    )
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = Color(0xFF8FBC8F),
                                    titleContentColor = Color.White
                                )
                            )

                            // Content
                            Box(modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(16.dp)) {
                                when (currentTab) {
                                    BottomTab.Home -> HomeScreen(
                                        uiState = uiState,
                                        onShowWifiInfo = { viewModel.getWifiInfo(this@MainActivity) },
                                        onRegister = { viewModel.registerWifiInfo(this@MainActivity) },
                                        onRefresh = { viewModel.getWifiInfo(this@MainActivity) }
                                    )
                                    BottomTab.Stats -> StatsScreen(
                                        weekly = viewModel.weeklyStats.collectAsState().value,
                                        error = viewModel.weeklyStatsError.collectAsState().value,
                                        onLoad = { viewModel.fetchWeeklyStats(this@MainActivity) },
                                        onRetry = { viewModel.fetchWeeklyStats(this@MainActivity) }
                                    )
                                    BottomTab.Settings -> SettingsScreen(
                                        uiState = uiState,
                                        onRefresh = { viewModel.getWifiInfo(this@MainActivity) },
                                        onDelete = { viewModel.deleteUser(this@MainActivity) }
                                    )
                                }
                            }

                            // Bottom Nav
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color.White.copy(alpha = 0.8f))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                BottomNavButton(
                                    text = "통계",
                                    active = currentTab == BottomTab.Stats,
                                    onClick = { currentTab = BottomTab.Stats }
                                )
                                BottomNavButton(
                                    text = "홈",
                                    active = currentTab == BottomTab.Home,
                                    onClick = { currentTab = BottomTab.Home }
                                )
                                BottomNavButton(
                                    text = "설정",
                                    active = currentTab == BottomTab.Settings,
                                    onClick = { currentTab = BottomTab.Settings }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.loadSavedUserId(this)
        // ✅ [수정] 일회성 작업(OneTime)을 주기적인 작업(Periodic)으로 변경
        // 15분마다 Wi-Fi 상태를 모니터링
        val workRequest = PeriodicWorkRequestBuilder<WifiMonitorWorker>(15, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                WifiMonitorWorker.WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP, // 기존 작업이 있으면 유지하고 새로 추가하지 않음
                workRequest
            )
    }

    override fun onStop() {
        super.onStop()
    }
}

enum class BottomTab { Home, Stats, Settings }

@Composable
private fun BottomNavButton(text: String, active: Boolean, onClick: () -> Unit) {
    val activeBg = Color(0xFF6B8E6B)
    val inactiveText = Color(0xFF4B5563)
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (active) activeBg else Color.Transparent,
            contentColor = if (active) Color(0xFFF5F5DC) else inactiveText
        ),
        shape = MaterialTheme.shapes.small
    ) { Text(text) }
}

@Composable
fun HomeScreen(
    uiState: WifiUiState,
    onShowWifiInfo: () -> Unit,
    onRegister: () -> Unit,
    onRefresh: () -> Unit
) {
    var showFlow by remember { mutableStateOf(false) }
    var showSuccess by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { onShowWifiInfo() }

    Column(
        modifier = Modifier
            .fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Current connected Wi‑Fi card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
                .background(Color.White.copy(alpha = 0.6f), shape = MaterialTheme.shapes.large)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("현재 연결된 Wi‑Fi", style = MaterialTheme.typography.titleMedium, color = Color(0xFF374151))
                Spacer(Modifier.weight(1f))
                OutlinedButton(
                    onClick = onRefresh,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF6B8E6B)),
                    border = BorderStroke(1.dp, Color(0xFF6B8E6B))
                ) { Text("새로고침") }
            }
            Spacer(Modifier.height(8.dp))
            if (uiState.message.isNotBlank()) {
                Text(uiState.message, color = MaterialTheme.colorScheme.error)
            } else {
                Text(
                    text = uiState.ssid.ifBlank { "SSID 없음" },
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFF8FBC8F)
                )
                if (uiState.bssid.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text("BSSID: ${uiState.bssid}", color = Color(0xFF6B7280))
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        // Center big button
        Button(
            onClick = { showFlow = true; showSuccess = false },
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8FBC8F)),
            modifier = Modifier.size(120.dp)
        ) { Text("Wi‑Fi", color = Color.White) }
        Spacer(modifier = Modifier.height(8.dp))
        Text("Wi‑Fi 등록하기", color = Color(0xFF4B5563))

        // Flow panel
        if (showFlow) {
            Spacer(Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White.copy(alpha = 0.5f), shape = MaterialTheme.shapes.medium)
                    .padding(16.dp)
            ) {
                Text("현재 연결된 Wi‑Fi", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Text(uiState.ssid.ifBlank { uiState.message })
                Spacer(Modifier.height(8.dp))
                Text("이 Wi‑Fi를 '집'으로 등록하시겠습니까?", color = Color(0xFF6B7280))
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        onRegister();
                        showFlow = false; showSuccess = true
                    },
                    enabled = uiState.ssid.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8FBC8F))
                ) { Text("등록", color = Color.White) }
            }
        }

        if (showSuccess) {
            Spacer(Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFD1FAE5), shape = MaterialTheme.shapes.medium)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) { Text("성공적으로 등록되었습니다.", color = Color(0xFF065F46)) }
        }

    }
}

@RequiresApi(Build.VERSION_CODES.O)
@SuppressLint("DefaultLocale")
@Composable
fun StatsScreen(
    weekly: WeeklyStatsResponse?,
    error: String?,
    onLoad: () -> Unit,
    onRetry: () -> Unit
) {
    LaunchedEffect(Unit) { onLoad() }

    // ✅ [수정] 데이터 파싱 실패에 대비한 상태 변수 추가
    var isDataError by remember { mutableStateOf(false) }

    // remember를 사용하여 weekly 데이터가 변경될 때만 파싱을 시도
    val chartData = remember(weekly) {
        if (weekly == null) return@remember null
        try {
            val fmt = DateTimeFormatter.ISO_LOCAL_DATE
            val start = LocalDate.parse(weekly.week_start, fmt)
            val labels = (0..6).map { d -> start.plusDays(d.toLong()).toString() }
            val values = labels.map { key -> weekly.daily_hours[key] ?: 0f }
            isDataError = false // 성공 시 에러 상태 초기화
            Pair(values, weekly.weekly_average)
        } catch (e: DateTimeParseException) {
            // 파싱 실패 시 에러 상태를 true로 설정
            isDataError = true
            null
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text("주간 체류 시간 통계", style = MaterialTheme.typography.titleLarge, color = Color(0xFF374151))
        Spacer(Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(Color.White.copy(alpha = 0.6f), shape = MaterialTheme.shapes.large)
                .padding(12.dp),
            contentAlignment = Alignment.Center // 자식 요소를 중앙에 배치
        ) {
            when {
                error != null -> {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("불러오기 실패", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Text(error.ifBlank { "서버 오류가 발생했습니다." }, color = Color(0xFF6B7280))
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8FBC8F), contentColor = Color.White)
                        ) { Text("다시 시도") }
                    }
                }
                isDataError -> {
                    Text("데이터를 불러오는 데 실패했습니다.", color = Color.Red)
                }
                chartData != null -> {
                    val (values, _) = chartData
                    Column(Modifier.fillMaxSize()) {
                        SimpleLineChart(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            data = values,
                            lineColor = Color(0xFF8FBC8F),
                            fillColor = Color(0x338FBC8F)
                        )
                        Spacer(Modifier.height(8.dp))
                        val dayNames = listOf("월","화","수","목","금","토","일")
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            dayNames.forEach { Text(it, color = Color(0xFF6B7280)) }
                        }
                    }
                }
                else -> {
                    Text("불러오는 중...", color = Color(0xFF6B7280))
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.6f), shape = MaterialTheme.shapes.large)
                .padding(16.dp)
        ) {
            if (error != null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Button(
                        onClick = onRetry,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8FBC8F), contentColor = Color.White)
                    ) { Text("다시 시도") }
                }
            } else if (chartData != null && !isDataError) {
                val (values, avg) = chartData
                val max = values.maxOrNull() ?: 0f
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("평균 시간", color = Color(0xFF6B7280))
                        Text(String.format("%.1f 시간", avg), color = Color(0xFF8FBC8F), style = MaterialTheme.typography.titleLarge)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("가장 긴 시간", color = Color(0xFF6B7280))
                        Text(String.format("%.1f 시간", max), color = Color(0xFF8FBC8F), style = MaterialTheme.typography.titleLarge)
                    }
                }
            } else if (!isDataError) {
                Text("이번 주 요약을 불러오는 중...", color = Color(0xFF6B7280))
            }
        }
    }
}

@Composable
fun SettingsScreen(
    uiState: WifiUiState,
    onRefresh: () -> Unit,
    onDelete: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Text("설정", style = MaterialTheme.typography.titleLarge, color = Color(0xFF374151))
        Spacer(Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.6f), shape = MaterialTheme.shapes.large)
                .padding(16.dp)
        ) {
            Text("현재 접속 정보", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(uiState.ssid.ifBlank { uiState.message })
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onRefresh,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF8FBC8F),
                    contentColor = Color.White
                )
            ) { Text("새로고침") }
        }
        Spacer(Modifier.height(12.dp))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.6f), shape = MaterialTheme.shapes.large)
                .padding(16.dp)
        ) {
            Text("등록된 Wi‑Fi", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            if (!uiState.homeSsid.isNullOrBlank()) {
                Text(uiState.homeSsid!!)
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = onDelete,
                    enabled = uiState.savedUserId != null,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("삭제")
                }
            } else {
                Text("등록된 집 Wi‑Fi가 없습니다.", color = Color(0xFF6B7280))
            }
        }
    }
}
