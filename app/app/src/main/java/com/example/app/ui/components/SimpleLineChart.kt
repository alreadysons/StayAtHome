package com.example.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.max

@Composable
fun SimpleLineChart(
    modifier: Modifier = Modifier,
    data: List<Float>,
    lineColor: Color = Color(0xFF8FBC8F),
    fillColor: Color = Color(0x338FBC8F)
) {
    Canvas(modifier = modifier) {
        if (data.isEmpty()) return@Canvas
        val maxVal = max(data.maxOrNull() ?: 0f, 1f)
        val w = size.width
        val h = size.height
        val n = data.size
        val stepX = if (n > 1) w / (n - 1) else 0f

        val points = data.mapIndexed { i, v ->
            val x = i * stepX
            val y = h - (v / maxVal) * h
            Offset(x, y)
        }

        // Fill area
        val fillPath = Path().apply {
            moveTo(0f, h)
            points.forEachIndexed { idx, p -> if (idx == 0) lineTo(p.x, p.y) else lineTo(p.x, p.y) }
            lineTo(w, h)
            close()
        }
        drawPath(path = fillPath, color = fillColor)

        // Line
        val linePath = Path().apply {
            points.forEachIndexed { idx, p -> if (idx == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
        }
        drawPath(path = linePath, color = lineColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round))

        // Points
        points.forEach { p ->
            drawCircle(color = lineColor, radius = 4.dp.toPx(), center = p)
        }
    }
}

