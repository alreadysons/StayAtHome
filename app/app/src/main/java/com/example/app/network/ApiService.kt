package com.example.app.network
import com.example.app.data.UserCreateRequest
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
interface ApiService {
    @POST("user/create")
    suspend fun registerUser(@Body request: UserCreateRequest): Response<Unit>
}