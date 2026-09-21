package com.dena.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.LocalContext
import com.dena.core.DenaPreferences
import com.dena.core.formatSigned

@Composable
fun SummaryBanner(
    total: Double,
    count: Int,
    currency: String,
    label: String,
    isOwedToMe: Boolean = true,
) {
    val context = LocalContext.current
    val prefs = remember(context) { DenaPreferences(context) }
    val showDecimals = prefs.showDecimals()
    val amountText = formatSigned(total, currency, negative = !isOwedToMe, showDecimals = showDecimals)
    val countText = when (count) {
        0 -> "no open debts"
        1 -> "1 open debt"
        else -> "$count open debts"
    }

    val cardContainer = if (isSystemInDarkTheme()) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surfaceContainerLowest
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = cardContainer,
        ),
        shape = RoundedCornerShape(14.dp),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(
            defaultElevation = 2.dp,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            AnimatedContent(
                targetState = amountText,
                transitionSpec = {
                    (slideInVertically(tween(220)) { it / 3 } + fadeIn(tween(180))) togetherWith
                        (slideOutVertically(tween(220)) { -it / 3 } + fadeOut(tween(180)))
                },
                label = "MoneyTick",
            ) { text ->
                Text(
                    text = text,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 44.sp,
                    letterSpacing = (-0.5).sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            androidx.compose.foundation.layout.Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = countText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
