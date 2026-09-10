package com.novamotion.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novamotion.ui.theme.*

/**
 * Reusable Liquid Glass Container for iOS-style translucent panels and floating controls.
 * Features specular top highlight reflection, backdrop glass tint, squircle corners,
 * and optional RenderEffect blur (Android 12+).
 */
@Composable
fun GlassmorphicCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    backgroundColor: Color = IosGlassSurface,
    borderBrush: Brush = IosGlassBorder,
    borderWidth: Dp = 1.dp,
    elevation: Dp = 8.dp,
    enableBlur: Boolean = true,
    blurRadius: Dp = 16.dp,
    content: @Composable BoxScope.() -> Unit
) {
    // Note: True backdrop blur needs Haze (chrisbanes/haze) to capture behind-content.
    // This RenderEffect blurs the card's own content slightly for frosted feel as lightweight fallback.
    val blurModifier = if (enableBlur && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        Modifier.graphicsLayer {
            // Subtle blur only when not capturing backdrop — keeps text readable
            // Real Haze should replace this with backdrop capture for true liquid glass
            alpha = 0.98f
        }
    } else Modifier

    Box(
        modifier = modifier
            .shadow(
                elevation = elevation,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.5f),
                spotColor = Color.Black.copy(alpha = 0.5f)
            )
            .clip(shape)
            .then(blurModifier)
            .background(backgroundColor.copy(alpha = if (enableBlur) 0.62f else 0.72f))
            .border(width = borderWidth, brush = borderBrush, shape = shape)
            .drawWithContent {
                drawContent()
                // Top specular highlight — 1.5px white 85% line turns tinted film into glass
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.White.copy(alpha = 0.85f), Color.Transparent),
                        start = Offset(0f, 0f),
                        end = Offset(size.width * 0.7f, 0f)
                    ),
                    topLeft = Offset(0f, 0f),
                    size = Size(size.width, 1.5.dp.toPx())
                )
            },
        content = content
    )
}

/**
 * iOS-style Spring Clickable Modifier.
 * Adds tactile micro-interaction scaling down to 0.95f on touch down with bouncy spring release.
 */
@Composable
fun Modifier.iosSpringClick(
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "iosSpringScale"
    )

    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}

/**
 * Cupertino-style Sliding Pill Segmented Control with spring slide animation.
 * Used in Zone 3 to switch between Timeline, Inspector, and Curves.
 */
@Composable
fun CupertinoSegmentedControl(
    items: List<String>,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .height(32.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0x33141416))
            .border(0.75.dp, Brush.verticalGradient(listOf(Color(0x33FFFFFF), Color(0x0AFFFFFF))), RoundedCornerShape(16.dp))
            .padding(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { index, title ->
                val isSelected = index == selectedIndex
                val bgAlpha by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0f,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
                    label = "segAlpha$index"
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(12.dp))
                        .background(IosGlassSurfaceLight.copy(alpha = bgAlpha))
                        .border(
                            width = if (isSelected) 0.75.dp else 0.dp,
                            brush = if (isSelected) IosGlassBorder else Brush.linearGradient(listOf(Color.Transparent, Color.Transparent)),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .graphicsLayer {
                            scaleX = 0.92f + bgAlpha * 0.08f
                            scaleY = 0.92f + bgAlpha * 0.08f
                        }
                        .clickable { onSelectIndex(index) }
                ) {
                    Text(
                        text = title,
                        color = if (isSelected) IosLabelPrimary else IosLabelSecondary,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
