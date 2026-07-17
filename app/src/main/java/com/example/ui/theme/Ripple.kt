package com.example.ui.theme

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.RippleAlpha
import androidx.compose.ui.graphics.Color

@OptIn(ExperimentalMaterial3Api::class)
val WhiteRippleConfiguration = RippleConfiguration(
    color = Color.White,
    rippleAlpha = RippleAlpha(
        pressedAlpha = 0.4f,
        focusedAlpha = 0.24f,
        draggedAlpha = 0.16f,
        hoveredAlpha = 0.08f
    )
)

@OptIn(ExperimentalMaterial3Api::class)
val PrimaryRippleConfiguration = RippleConfiguration(
    color = RedPrimary,
    rippleAlpha = RippleAlpha(
        pressedAlpha = 0.4f,
        focusedAlpha = 0.24f,
        draggedAlpha = 0.16f,
        hoveredAlpha = 0.08f
    )
)
