package com.dena.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
    val showDecimals = DenaPreferences(LocalContext.current).showDecimals()
    val amountText = formatSigned(total, currency, negative = !isOwedToMe, showDecimals = showDecimals)
    val countText = if (count == 0) "no people"
    else if (count == 1) "across 1 person"
    else "across $count people"

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
        ) {
            // Large total — match Debt Tracker font: ~42sp extra bold, tight line height
            Text(
                text = amountText,
                fontSize = 42.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 44.sp,
                letterSpacing = (-0.5).sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
            )
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