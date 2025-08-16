package com.example.app.data

data class UserResponse(
    val user_name: String,
    val home_ssid: String,
    val home_bssid: String,
    val id: Int  // user_id
)