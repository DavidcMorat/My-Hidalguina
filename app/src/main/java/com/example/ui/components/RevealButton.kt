package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.sqrt

@Composable
fun RevealButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    revealColor: Color,
    contentColor: Color,
    content: @Composable RowScope.() -> Unit
) {
    val progress = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = null, // Deshabilita el ripple por defecto
                onClick = {
                    if (!progress.isRunning) {
                        coroutineScope.launch {
                            // Fase 1: El círculo del color de revelado crece desde el centro
                            progress.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                            )
                            onClick() // Ejecutamos la acción cuando está completamente cubierto
                            
                            // Fase 2: El círculo del color original vuelve a crecer encima
                            progress.animateTo(
                                targetValue = 2f,
                                animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
                            )
                            // Reinicio silencioso
                            progress.snapTo(0f)
                        }
                    }
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            // Calculamos el radio máximo para cubrir todas las esquinas desde el centro
            val maxRadius = sqrt((size.width / 2) * (size.width / 2) + (size.height / 2) * (size.height / 2))

            if (progress.value > 0f) {
                drawCircle(
                    color = revealColor,
                    radius = (progress.value.coerceIn(0f, 1f)) * maxRadius,
                    center = center
                )
            }
            if (progress.value > 1f) {
                drawCircle(
                    color = backgroundColor,
                    radius = ((progress.value - 1f).coerceIn(0f, 1f)) * maxRadius,
                    center = center
                )
            }
        }
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                content()
            }
        }
    }
}
