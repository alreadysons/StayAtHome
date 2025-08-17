package com.example.app.worker

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.app.data.*
import com.example.app.data.LogResponse
import com.example.app.data.StartLogRequest
import com.example.app.network.RetrofitClient
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

class WifiMonitorWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        try {
            val ctx = applicationContext

            val uid = userIdFlow(ctx).first() ?: return scheduleNext()
            val (homeSsid, homeBssid) = homeWifiFlow(ctx).first()
            if (homeSsid.isNullOrBlank() || homeBssid.isNullOrBlank()) return scheduleNext()

            val (curSsid, curBssid, isWifi) = getCurrentWifi(ctx)
            val isHome = isWifi && curSsid == homeSsid && curBssid.equals(homeBssid, ignoreCase = true)

            val currentLogId = currentLogIdFlow(ctx).first()

            if (isHome) {
                if (currentLogId == null) {
                    try {
                        val log: LogResponse = RetrofitClient.instance.startLog(StartLogRequest(uid))
                        saveCurrentLogId(ctx, log.id)
                    } catch (_: Exception) { /* keep silent; will retry next tick */ }
                }
            } else {
                if (currentLogId != null) {
                    try {
                        RetrofitClient.instance.endLog(currentLogId)
                    } catch (_: Exception) { /* keep silent */ }
                    clearCurrentLogId(ctx)
                }
            }

            return scheduleNext()
        } catch (_: Exception) {
            return scheduleNext()
        }
    }

    private fun scheduleNext(): Result {
        val req = OneTimeWorkRequestBuilder<WifiMonitorWorker>()
            .setInitialDelay(1, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(applicationContext)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, req)
        return Result.success()
    }

    private fun getCurrentWifi(context: Context): Triple<String, String, Boolean> {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork
        val caps = cm.getNetworkCapabilities(network)
        val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

        if (!isWifi) return Triple("", "", false)
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        @Suppress("DEPRECATION")
        val info = wifiManager.connectionInfo
        val ssid = info?.ssid?.removeSurrounding("\"") ?: ""
        val bssid = info?.bssid ?: ""
        return Triple(ssid, bssid, true)
    }

    companion object {
        const val WORK_NAME = "wifi_monitor_worker"
    }
}

