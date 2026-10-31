package com.nsr.qilin.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nsr.qilin.ui.Spacing
import com.nsr.qilin.ui.rememberLiquidGlassBorder
import com.nsr.qilin.ui.theme.CornerRadius
import com.nsr.qilin.ui.theme.IconSize
import com.nsr.qilin.ui.theme.RevealOnEnter

@Composable
fun EmptyState(
    title: String,
    subtitle: String? = null,
    icon: ImageVector? = null,
    painter: Painter? = null,
    actionLabel: String? = null,
    actionIcon: ImageVector? = null,
    onAction: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        RevealOnEnter(index = 0, distance = 18.dp) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier.padding(horizontal = Spacing.xxxl)
        ) {
            if (icon != null || painter != null) {
                val isLight = MaterialTheme.colorScheme.background.luminance() > 0.5f
                val iconBorderBrush = rememberLiquidGlassBorder(isLight)
                val iconBgBrush = Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = if (isLight) 0.12f else 0.16f),
                        MaterialTheme.colorScheme.primary.copy(alpha = if (isLight) 0.03f else 0.04f)
                    )
                )

                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(CornerRadius.card))
                        .background(iconBgBrush)
                        .border(
                            width = 1.dp,
                            brush = iconBorderBrush,
                            shape = RoundedCornerShape(CornerRadius.card)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(34.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                        )
                    } else if (painter != null) {
                        Icon(
                            painter = painter,
                            contentDescription = null,
                            modifier = Modifier.size(34.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xs))

            Text(
                text = title,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                textAlign = TextAlign.Center
            )

            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }

            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                ColorOSPillButton(
                    text = actionLabel,
                    onClick = onAction,
                    height = 46.dp,
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = if (actionIcon != null) {
                        {
                            Icon(
                                imageVector = actionIcon,
                                contentDescription = null,
                                modifier = Modifier.size(IconSize.sm)
                            )
                        }
                    } else null
                )
            }

            if (secondaryActionLabel != null && onSecondaryAction != null) {
                ColorOSTextButton(
                    text = secondaryActionLabel,
                    onClick = onSecondaryAction
                )
            }
        }
        }
    }
}
