package com.mydonghua.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import dev.chrisbanes.haze.hazeChild
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.mydonghua.app.R
import com.mydonghua.app.ui.LocalHazeState
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls

@Composable
fun LiquidGlassNavBar(
    currentRoute: String,
    isDark: Boolean,
    backdrop: Backdrop,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
            .clip(RoundedCornerShape(30.dp))
            .drawBackdropCustomShape(
                backdrop = backdrop,
                shape = RoundedCornerShape(30.dp),
            )
            .background(
                color = tint.copy(alpha = 0.05f),
                shape = RoundedCornerShape(30.dp),
            )
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(30.dp),
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        NavBarIcon(
            iconRes = R.drawable.ic_home,
            selected = currentRoute == "home",
            onClick = { onNavigate("home") },
        )
        NavBarIcon(
            iconRes = R.drawable.ic_airing,
            selected = currentRoute == "airing",
            onClick = { onNavigate("airing") },
        )
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f),
                    shape = CircleShape,
                )
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onNavigate("add_edit_show") },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = "Add",
                modifier = Modifier.size(28.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        NavBarIcon(
            iconRes = R.drawable.ic_complete,
            selected = currentRoute == "complete",
            onClick = { onNavigate("complete") },
        )
        NavBarIcon(
            iconRes = R.drawable.ic_stopped,
            selected = currentRoute == "stopped",
            onClick = { onNavigate("stopped") },
        )
    }
}

@Composable
private fun RowScope.NavBarIcon(
    iconRes: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier.size(48.dp),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .hazeChild(state = LocalHazeState.current, shape = RoundedCornerShape(10.dp))
                    .background(
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(10.dp)
                    )
            )
        }
        IconButton(
            onClick = onClick,
            modifier = Modifier.size(48.dp),
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun Modifier.drawBackdropCustomShape(
    backdrop: Backdrop,
    shape: Shape,
): Modifier =
    this.drawBackdrop(
        backdrop = backdrop,
        effects = {
            colorControls(saturation = 1.5f)
            blur(4f.dp.toPx())
        },
        shape = { shape },
    )
