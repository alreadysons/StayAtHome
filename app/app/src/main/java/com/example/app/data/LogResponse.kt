package com.example.app.data

data class LogResponse(
    val user_id: Int,
    val id: Int,              // log_id
    val start_time: String,
    val end_time: String?
)

