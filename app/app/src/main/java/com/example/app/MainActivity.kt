package com.example.app

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.app.ui.theme.AppTheme
import com.example.app.viewmodel.WifiUiState
import com.example.app.viewmodel.ViewModel
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.app.worker.WifiMonitorWorker

class MainActivity : ComponentActivity() {

    private val viewModel: ViewModel by viewModels()
    // WorkManager로 주기적 체크

    // 권한 요청 결과를 처리하는 부분
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // 권한을 허용하면 Wi-Fi 정보 가져오기 시도
                viewModel.getWifiInfo(this)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 앱이 켜지자마자 위치 권한 요청
        requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)

        setContent {
            AppTheme {
                // ViewModel의 상태가 바뀔 때마다 화면이 자동으로 업데이트됨
                val uiState by viewModel.uiState.collectAsState()

                WifiInfoScreen(
                    uiState = uiState,
                    onRefresh = { viewModel.getWifiInfo(this) },
                    onRegister = { viewModel.registerWifiInfo(this) },
                    onEnd = { viewModel.endHomeLog(this) },
                    onDelete = { viewModel.deleteUser(this) }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // 저장된 user_id 로드
        viewModel.loadSavedUserId(this)
        // WorkManager 스케줄 시작
        val req = OneTimeWorkRequestBuilder<WifiMonitorWorker>().build()
        WorkManager.getInstance(this)
            .enqueueUniqueWork(WifiMonitorWorker.WORK_NAME, ExistingWorkPolicy.REPLACE, req)
    }

    override fun onStop() {
        super.onStop()
    }
}

// 화면 UI를 그리는 부분
@Composable
fun WifiInfoScreen(
    uiState: WifiUiState,
    onRefresh: () -> Unit,
    onRegister: () -> Unit,
    onEnd: () -> Unit,
    onDelete: () -> Unit
) {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (uiState.message.isNotEmpty()) {
            Text(text = uiState.message, color = MaterialTheme.colorScheme.error)
        } else {
            Text(text = "SSID: ${uiState.ssid}")
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "BSSID: ${uiState.bssid}")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRefresh) {
            Text("새로고침")
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { onRegister() },
            enabled = uiState.ssid.isNotEmpty()
        ) {
            Text("서버에 등록 (SSID/BSSID)")
        }

        if (uiState.registrationStatus.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = uiState.registrationStatus)
        }

        if (uiState.savedUserId != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = "저장된 user_id: ${uiState.savedUserId}")
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onEnd) {
            Text("WIFI 해제 테스트")
        }

        Spacer(modifier = Modifier.height(12.dp))
        Button(onClick = onDelete, enabled = uiState.savedUserId != null) {
            Text("사용자 삭제 (/user/delete)")
        }
    }
}
