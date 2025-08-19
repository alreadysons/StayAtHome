package com.example.app.data

data class WeeklyStatsResponse(
    val week_start: String,
    val week_end: String,
    val daily_hours: Map<String, Float>,
    val weekly_total: Float,
    val weekly_average: Float
)

