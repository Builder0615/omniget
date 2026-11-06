package com.openselena.omniget.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun Modifier.paperWell(shape: Shape = RoundedCornerShape(22.dp)): Modifier {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val shadow = if (dark) Color.Black.copy(alpha = 0.18f) else Color(0xFF6E5537).copy(alpha = 0.10f)
    val highlight = Color.White.copy(alpha = if (dark) 0.055f else 0.72f)
    return this
        .clip(shape)
        .background(MaterialTheme.colorScheme.surfaceVariant)
        .drawWithCache {
            val outline = shape.createOutline(size, layoutDirection, this)
            val innerShadow = Brush.linearGradient(
                colors = listOf(shadow, shadow.copy(alpha = shadow.alpha * 0.45f), Color.Transparent),
                start = Offset.Zero,
                end = Offset(size.width * 0.55f, size.height * 1.5f),
            )
            val innerHighlight = Brush.verticalGradient(
                colors = listOf(Color.Transparent, highlight),
                startY = size.height * 0.35f,
                endY = size.height,
            )
            onDrawWithContent {
                drawContent()
                drawOutline(outline, innerShadow, alpha = 0.12f, style = Stroke(6.dp.toPx()))
                drawOutline(outline, innerShadow, alpha = 0.18f, style = Stroke(3.dp.toPx()))
                drawOutline(outline, innerShadow, alpha = 0.26f, style = Stroke(1.dp.toPx()))
                drawOutline(
                    outline = outline,
                    brush = innerHighlight,
                    style = Stroke(0.9.dp.toPx()),
                )
            }
        }
}

@Composable
fun Modifier.paperRaised(
    shape: Shape = RoundedCornerShape(22.dp),
    elevation: Dp = 6.dp,
    color: Color = MaterialTheme.colorScheme.surface,
): Modifier {
    return paperRelief(shape, elevation, color, pressed = 0f)
}

@Composable
private fun Modifier.paperRelief(
    shape: Shape,
    elevation: Dp,
    color: Color,
    pressed: Float,
): Modifier {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val castShadow = if (dark) Color.Black.copy(alpha = 0.50f) else Color(0xFF5E4A34).copy(alpha = 0.22f)
    val edgeShadow = if (dark) Color.Black.copy(alpha = 0.28f) else Color(0xFF5E4A34).copy(alpha = 0.16f)
    val highlight = Color.White.copy(alpha = if (dark) 0.14f else 0.82f)
    return this
        .shadow(elevation, shape, clip = false, ambientColor = castShadow, spotColor = castShadow)
        .drawWithCache {
            val outline = shape.createOutline(size, layoutDirection, this)
            val lightOffset = 0.65.dp.toPx() * (1f - pressed)
            onDrawBehind {
                translate(left = -lightOffset, top = -lightOffset) {
                    drawOutline(outline, highlight, alpha = 0.55f * (1f - pressed))
                }
                translate(left = lightOffset, top = lightOffset) {
                    drawOutline(outline, edgeShadow, alpha = 0.48f * (1f - pressed))
                }
            }
        }
        .background(color, shape)
        .clip(shape)
        .drawWithCache {
            val outline = shape.createOutline(size, layoutDirection, this)
            val bevel = Brush.linearGradient(
                colors = listOf(
                    lerp(highlight, edgeShadow, pressed),
                    Color.Transparent,
                    lerp(edgeShadow.copy(alpha = edgeShadow.alpha * 0.65f), highlight, pressed),
                ),
                start = Offset.Zero,
                end = Offset(size.width * 0.4f, size.height),
            )
            val depression = Brush.linearGradient(
                colors = listOf(edgeShadow, Color.Transparent),
                start = Offset.Zero,
                end = Offset(size.width * 0.25f, size.height),
            )
            onDrawWithContent {
                drawContent()
                drawOutline(outline, bevel, alpha = 0.78f, style = Stroke(1.2.dp.toPx()))
                if (pressed > 0f) {
                    drawOutline(outline, depression, alpha = pressed * 0.10f, style = Stroke(7.dp.toPx()))
                    drawOutline(outline, depression, alpha = pressed * 0.22f, style = Stroke(2.dp.toPx()))
                }
            }
        }
}

@Composable
private fun Modifier.paperButtonSurface(
    interactionSource: InteractionSource,
    shape: Shape,
    accent: Boolean,
    enabled: Boolean,
    selected: Boolean = false,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val depression by animateFloatAsState(
        targetValue = if (pressed || selected) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 900f),
        label = "paper-button-depression",
    )
    val scheme = MaterialTheme.colorScheme
    val fill = if (accent) {
        lerp(scheme.primaryContainer, Color.Black, depression * 0.07f)
    } else {
        lerp(scheme.surface, scheme.surfaceVariant, depression)
    }
    return this
        .pressMotion(interactionSource)
        .graphicsLayer { alpha = if (enabled) 1f else 0.48f }
        .paperRelief(
            shape = shape,
            elevation = (6f - depression * 5f).dp,
            color = fill,
            pressed = depression,
        )
}

@Composable
fun PaperButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Boolean = false,
    selected: Boolean = false,
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val foreground = when {
        accent -> MaterialTheme.colorScheme.onPrimaryContainer
        selected -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    CompositionLocalProvider(LocalContentColor provides foreground) {
        ProvideTextStyle(MaterialTheme.typography.labelLarge) {
            Row(
                modifier = modifier
                    .defaultMinSize(minHeight = 48.dp)
                    .paperButtonSurface(interactionSource, CircleShape, accent, enabled, selected)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        enabled = enabled,
                        role = Role.Button,
                        onClick = onClick,
                    )
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    }
}

@Composable
fun PaperIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    accent: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val foreground = if (accent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    CompositionLocalProvider(LocalContentColor provides foreground) {
        Box(
            modifier = modifier
                .defaultMinSize(minWidth = 44.dp, minHeight = 44.dp)
                .paperButtonSurface(interactionSource, CircleShape, accent, enabled)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                ),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

@Composable
fun Modifier.pressMotion(interactionSource: InteractionSource): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = 0.85f, stiffness = 700f),
        label = "paper-press-scale",
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
        translationY = (1f - scale) * 18.dp.toPx()
    }
}

@Composable
fun PaperPressable(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(16.dp),
    role: Role? = Role.Button,
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val depression by animateFloatAsState(
        targetValue = if (pressed) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.9f, stiffness = 900f),
        label = "paper-touch-depression",
    )
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val well = MaterialTheme.colorScheme.surfaceVariant
    val shadow = if (dark) Color.Black.copy(alpha = 0.24f) else Color(0xFF6E5537).copy(alpha = 0.16f)
    val highlight = Color.White.copy(alpha = if (dark) 0.09f else 0.72f)
    Box(
        modifier = modifier
            .pressMotion(interactionSource)
            .clip(shape)
            .drawWithCache {
                val outline = shape.createOutline(size, layoutDirection, this)
                val bevel = Brush.linearGradient(
                    colors = listOf(shadow, Color.Transparent, highlight),
                    start = Offset.Zero,
                    end = Offset(size.width * 0.4f, size.height),
                )
                onDrawWithContent {
                    drawOutline(outline, well, alpha = depression * 0.32f)
                    drawContent()
                    drawOutline(outline, bevel, alpha = depression * 0.8f, style = Stroke(1.4.dp.toPx()))
                }
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = role,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
        content = content,
    )
}
