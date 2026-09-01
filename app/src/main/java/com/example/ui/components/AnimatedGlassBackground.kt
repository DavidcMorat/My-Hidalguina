package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.ThemeState

@Composable
fun AnimatedGlassBackground(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glass_bg")

    // Animating offsets for fluid mesh gradient look
    val offset1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset1"
    )

    val offset2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "offset2"
    )

    val isDark = ThemeState.isDarkTheme

    // Vibrant Glass Colors based on theme
    val color1 = if (isDark) Color(0xFF4A0000) else Color(0xFF8B0000)
    val color2 = if (isDark) Color(0xFF6B1A1A) else Color(0xFFD32F2F)
    val color3 = if (isDark) Color(0xFF8B4500) else Color(0xFFFFB300)
    val bgColor = if (isDark) Color(0xFF1E0505) else Color(0xFFFFF3E0)

    Box(modifier = modifier.fillMaxSize().background(bgColor)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Large blurred blobs moving around
            val radius = width * 1.2f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color1, Color.Transparent),
                    center = Offset(width * offset1, height * 0.2f),
                    radius = radius
                ),
                radius = radius,
                center = Offset(width * offset1, height * 0.2f)
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color2, Color.Transparent),
                    center = Offset(width * 0.8f, height * offset2),
                    radius = radius * 0.8f
                ),
                radius = radius * 0.8f,
                center = Offset(width * 0.8f, height * offset2)
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(color3, Color.Transparent),
                    center = Offset(width * offset2, height * 0.8f),
                    radius = radius * 0.9f
                ),
                radius = radius * 0.9f,
                center = Offset(width * offset2, height * 0.8f)
            )
        }
        content()
    }
}
