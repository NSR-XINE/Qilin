package com.mydonghua.app.ui.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mydonghua.app.R
import com.mydonghua.app.ui.theme.OnePlusSansFamily

@Composable
fun AppLoadingScreen(
    isDark: Boolean = true,
    isAmoled: Boolean = false,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when {
        isAmoled -> Color(0xFF000000)
        isDark -> Color(0xFF060A12)
        else -> Color(0xFFF6F8FC)
    }
    val textColor = if (isDark) Color.White else Color(0xFF121826)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_dragon_emblem_trimmed),
                contentDescription = "My Donghua",
                modifier = Modifier
                    .width(80.dp)
                    .height(64.5.dp),
                contentScale = ContentScale.FillBounds
            )

            Spacer(modifier = Modifier.height(18.dp))

            Text(
                text = "My Donghua",
                fontFamily = OnePlusSansFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                color = textColor
            )

            Spacer(modifier = Modifier.height(24.dp))

            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color(0xFF0082FB),
                strokeWidth = 2.dp
            )
        }
    }
}
