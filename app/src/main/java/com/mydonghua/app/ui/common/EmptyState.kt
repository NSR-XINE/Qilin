package com.mydonghua.app.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mydonghua.app.ui.Spacing
import com.mydonghua.app.ui.theme.ButtonHeight
import com.mydonghua.app.ui.theme.IconSize

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
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            modifier = Modifier.padding(horizontal = Spacing.xxxl)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(IconSize.hero),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
                )
            } else if (painter != null) {
                Icon(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier.size(IconSize.hero),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xs))

            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )

            if (subtitle != null) {
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
            }

            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.height(ButtonHeight.md)
                ) {
                    if (actionIcon != null) {
                        Icon(
                            imageVector = actionIcon,
                            contentDescription = null,
                            modifier = Modifier.size(IconSize.sm)
                        )
                        Spacer(modifier = Modifier.width(Spacing.sm))
                    }
                    Text(text = actionLabel)
                }
            }

            if (secondaryActionLabel != null && onSecondaryAction != null) {
                TextButton(onClick = onSecondaryAction) {
                    Text(
                        text = secondaryActionLabel,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
