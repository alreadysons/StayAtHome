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
import com.example.app.data.UserResponse
import com.example.app.data.StartLogRequest
import com.example.app.data.LogResponse
import com.example.app.network.RetrofitClient
import com.example.app.data.saveUserId
import com.example.app.data.userIdFlow
import com.example.app.data.clearUserId
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
    val registrationStatus: String = "",
    val savedUserId: Int? = null
)

class ViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(WifiUiState())
    val uiState: StateFlow<WifiUiState> = _uiState.asStateFlow()

    // 임시 저장(추후 DataStore로 교체 권장)
    private var userId: Int? = null
    private var currentLogId: Int? = null
    private var homeSsid: String? = null
    private var homeBssid: String? = null

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

    //wifi 정보 등록
    fun registerWifiInfo(context: Context) {
        val currentState = _uiState.value
        // 이미 저장된 사용자라면 중복 등록 방지
        if (userId != null || currentState.savedUserId != null) {
            _uiState.update { it.copy(registrationStatus = "이미 등록된 사용자입니다 (user_id=${userId ?: currentState.savedUserId})") }
            return
        }
        if (currentState.ssid.isBlank()) {
            _uiState.update { it.copy(registrationStatus = "Wi‑Fi 정보가 필요합니다.") }
            return
        }

        viewModelScope.launch {
            try {
                _uiState.update { it.copy(registrationStatus = "등록 중...") }
                val request = UserCreateRequest(currentState.ssid, currentState.bssid)
                val res: UserResponse = RetrofitClient.instance.registerUser(request)
                userId = res.id
                homeSsid = res.home_ssid
                homeBssid = res.home_bssid
                // DataStore에 userId 저장
                saveUserId(context, res.id)
                _uiState.update { it.copy(savedUserId = res.id) }
                _uiState.update { it.copy(registrationStatus = "등록 성공 (user_id=${res.id})") }

                // 등록 직후 현재 Wi‑Fi가 집과 일치하면 즉시 로그 시작
                val now = _uiState.value
                if (currentLogId == null && isHomeWifi(now.ssid, now.bssid)) {
                    startHomeLog()
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(registrationStatus = "오류 발생: ${e.message}") }
            }
        }
    }

    // 저장된 user_id 로드하여 표시
    fun loadSavedUserId(context: Context) {
        viewModelScope.launch {
            userIdFlow(context).collect { id ->
                if (id != null) {
                    userId = id
                    _uiState.update { it.copy(savedUserId = id) }
                }
            }
        }
    }

    //wifi 연결 시 호출
    fun startHomeLog() {
        val uid = userId ?: return
        if (currentLogId != null) return // 이미 시작됨
        viewModelScope.launch {
            try {
                val log: LogResponse = RetrofitClient.instance.startLog(StartLogRequest(uid))
                currentLogId = log.id
                // TODO: DataStore에 currentLogId 저장
            } catch (_: Exception) { /* no-op */ }
        }
    }

    //wifi 해제 시 호출
    fun endHomeLog() {
        val logId = currentLogId ?: return
        viewModelScope.launch {
            try {
                RetrofitClient.instance.endLog(logId)
                currentLogId = null
                // TODO: DataStore에서 currentLogId 제거
            } catch (_: Exception) { /* no-op */ }
        }
    }

    // 네트워크 콜백에서 호출: 현재 Wi‑Fi가 집인지 확인 후 start
    fun onWifiAvailable(context: Context) {
        getWifiInfo(context)
        val state = _uiState.value
        if (isHomeWifi(state.ssid, state.bssid)) {
            // 집 Wi‑Fi에 연결됨 → 시작 시도
            startHomeLog()
        } else {
            // 집이 아닌 Wi‑Fi에 연결됨 → 진행 중이면 종료
            if (currentLogId != null) endHomeLog()
        }
    }

    private fun isHomeWifi(currentSsid: String, currentBssid: String): Boolean {
        val hb = homeBssid
        if (!hb.isNullOrBlank() && currentBssid.equals(hb, ignoreCase = true)) return true
        val hs = homeSsid
        return !hs.isNullOrBlank() && currentSsid == hs
    }

    // 네트워크 콜백에서 호출: wifi 해제 시 종료 시도
    fun onWifiLost() {
        if (currentLogId != null) {
            endHomeLog()
        }
    }

    // 사용자 삭제 및 DataStore에서 user_id 제거
    fun deleteUser(context: Context) {
        val uid = userId ?: _uiState.value.savedUserId
        if (uid == null) {
            _uiState.update { it.copy(registrationStatus = "삭제할 사용자 없음") }
            return
        }
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(registrationStatus = "삭제 중...") }
                val res = RetrofitClient.instance.deleteUser(uid)
                // 성공 시 상태/저장소 초기화
                userId = null
                currentLogId = null
                homeSsid = null
                homeBssid = null
                clearUserId(context)
                _uiState.update { it.copy(savedUserId = null, registrationStatus = "삭제 성공 (user_id=${res.id})") }
            } catch (e: Exception) {
                _uiState.update { it.copy(registrationStatus = "삭제 실패: ${e.message}") }
            }
        }
    }
}
