package com.example.app.viewmodel
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app.data.UserCreateRequest
import com.example.app.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// 화면에 표시될 모든 정보를 담는 데이터 클래스
data class WifiUiState(
    val ssid: String = "",
    val bssid: String = "",
    val message: String = "Wi-Fi 정보를 가져오는 중...",
    val registrationStatus: String = ""
)

class ViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(WifiUiState())
    val uiState: StateFlow<WifiUiState> = _uiState.asStateFlow()

    // wifi 정보 가져오는 기능
    fun getWifiInfo(context: Context) {
        // 위치 권한이 없으면 실행 중단
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            _uiState.update { it.copy(message = "Wi-Fi 정보를 보려면 위치 권한이 필요합니다.") }
            return
        }
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)

        if (capabilities != null && capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
            // WifiManager.connectionInfo is deprecated but seems more reliable than transportInfo in some cases.
            // It should work on newer Android versions if ACCESS_FINE_LOCATION is granted.
            @Suppress("DEPRECATION")
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as android.net.wifi.WifiManager
            @Suppress("DEPRECATION")
            val wifiInfo = wifiManager.connectionInfo

            val ssid = wifiInfo?.ssid?.removeSurrounding("\"") ?: ""
            val bssid = wifiInfo?.bssid ?: ""

            if (ssid.isNotEmpty() && ssid != "<unknown ssid>") {
                _uiState.update { it.copy(ssid = ssid, bssid = bssid, message = "") }
            } else {
                _uiState.update { it.copy(message = "SSID를 찾을 수 없습니다. 위치 서비스가 켜져 있는지 확인하세요.") }
            }
        } else {
            _uiState.update { it.copy(message = "Wi-Fi에 연결되어 있지 않습니다.") }
        }
    }

    //wifi 정보 등록하는 기능
    fun registerWifiInfo(userName: String) {
        val currentState = _uiState.value
        if (userName.isBlank() || currentState.ssid.isBlank()) {
            _uiState.update { it.copy(registrationStatus = "이름과 Wi-Fi 정보가 모두 필요합니다.") }
            return
        }

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(registrationStatus = "등록 중...") }
                val request = UserCreateRequest(userName, currentState.ssid, currentState.bssid)
                val response = RetrofitClient.instance.registerUser(request)
                if (response.isSuccessful) {
                    _uiState.update { it.copy(registrationStatus = "등록 성공!") }
                } else {
                    _uiState.update { it.copy(registrationStatus = "등록 실패 (코드: ${response.code()})") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(registrationStatus = "오류 발생: ${e.message}") }
            }
        }
    }
}