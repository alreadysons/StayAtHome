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
import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest

class MainActivity : ComponentActivity() {

    private val viewModel: ViewModel by viewModels()
    private lateinit var connectivityManager: ConnectivityManager
    private val wifiNetworkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            // Wi‑Fi 네트워크가 사용 가능해지면 호출됨
            runOnUiThread {
                viewModel.onWifiAvailable(this@MainActivity)
            }
        }

        override fun onLost(network: Network) {
            // Wi‑Fi 네트워크가 끊어졌을 때 호출됨
            runOnUiThread {
                viewModel.onWifiLost()
            }
        }
    }

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
                    onRegister = { userName -> viewModel.registerWifiInfo(userName) },
                    onEnd = { viewModel.endHomeLog() }
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val request = NetworkRequest.Builder()
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .build()
        connectivityManager.registerNetworkCallback(request, wifiNetworkCallback)
    }

    override fun onStop() {
        super.onStop()
        try {
            connectivityManager.unregisterNetworkCallback(wifiNetworkCallback)
        } catch (_: Exception) { }
    }
}

// 화면 UI를 그리는 부분
@Composable
fun WifiInfoScreen(
    uiState: WifiUiState,
    onRefresh: () -> Unit,
    onRegister: (String) -> Unit,
    onEnd: () -> Unit
) {
    var userName by remember { mutableStateOf("") }

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

        Spacer(modifier = Modifier.height(32.dp))
        OutlinedTextField(
            value = userName,
            onValueChange = { userName = it },
            label = { Text("사용자 이름") },
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { onRegister(userName) },
            enabled = uiState.ssid.isNotEmpty() && userName.isNotEmpty()
        ) {
            Text("서버에 등록")
        }

        if (uiState.registrationStatus.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = uiState.registrationStatus)
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onEnd) {
            Text("WIFI 해제 테스트")
        }
    }
}
