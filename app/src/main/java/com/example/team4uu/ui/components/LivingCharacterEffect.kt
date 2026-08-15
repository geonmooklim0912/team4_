package com.example.team4uu.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

// 토킹톰처럼 "살아있는 느낌"을 주는 캐릭터 공용 효과.
// 평소엔 숨쉬듯 살짝 커졌다 작아지고, 탭하면 통통 튀는 스쿼시 애니메이션이 재생됨.
// TestResultScreen과 MainHomeContent(방 무대)가 함께 사용.
fun Modifier.livingCharacterEffect(): Modifier = composed {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val infiniteTransition = rememberInfiniteTransition(label = "breath")
    val breathScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.035f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "breathScale"
    )

    val bounceOffsetPx = remember { Animatable(0f) }
    val squishX = remember { Animatable(1f) }
    val squishY = remember { Animatable(1f) }

    fun onTapped() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            bounceOffsetPx.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                ),
                initialVelocity = with(density) { (-900).dp.toPx() }
            )
        }
        scope.launch {
            squishY.animateTo(0.78f, animationSpec = tween(70))
            squishY.animateTo(
                1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
        scope.launch {
            squishX.animateTo(1.18f, animationSpec = tween(70))
            squishX.animateTo(
                1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    this
        .offset { IntOffset(0, bounceOffsetPx.value.roundToInt()) }
        .graphicsLayer {
            scaleX = breathScale * squishX.value
            scaleY = breathScale * squishY.value
            transformOrigin = TransformOrigin(0.5f, 1f) // 바닥에 붙어 눌리는 느낌
        }
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) { onTapped() }
}
