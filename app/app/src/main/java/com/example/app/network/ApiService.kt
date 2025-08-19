package com.example.app.network
import com.example.app.data.UserCreateRequest
import com.example.app.data.UserResponse
import com.example.app.data.StartLogRequest
import com.example.app.data.LogResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.DELETE
import retrofit2.http.Path
import retrofit2.http.PUT
import com.example.app.data.WeeklyStatsResponse

interface ApiService {
    @POST("user/create")
    suspend fun registerUser(@Body request: UserCreateRequest): UserResponse

    @POST("log/start")
    suspend fun startLog(@Body request: StartLogRequest): LogResponse

    @POST("log/end")
    suspend fun endLog(@Query("log_id") logId: Int): LogResponse

    @DELETE("user/delete/{user_id}")
    suspend fun deleteUser(@Path("user_id") userId: Int): UserResponse

    @PUT("user/{user_id}/home_wifi")
    suspend fun updateHomeWifi(
        @Path("user_id") userId: Int,
        @Body request: UserCreateRequest
    ): UserResponse

    @GET("statistics/weekly/{user_id}")
    suspend fun getWeeklyStats(
        @Path("user_id") userId: Int
    ): WeeklyStatsResponse
}
