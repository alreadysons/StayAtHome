package com.example.app.network
import com.example.app.data.UserCreateRequest
import com.example.app.data.UserResponse
import com.example.app.data.StartLogRequest
import com.example.app.data.LogResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface ApiService {
    @POST("user/create")
    suspend fun registerUser(@Body request: UserCreateRequest): UserResponse

    @POST("log/start")
    suspend fun startLog(@Body request: StartLogRequest): LogResponse

    @POST("log/end")
    suspend fun endLog(@Query("log_id") logId: Int): LogResponse
}
