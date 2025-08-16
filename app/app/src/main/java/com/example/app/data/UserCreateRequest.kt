package com.example.app.data

data class UserCreateRequest(
    val user_name : String,
    val home_ssid: String,
    val home_bssid: String
)