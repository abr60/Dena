package com.dena.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private data class OnboardPage(val icon: ImageVector, val title: String, val desc: String)

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val pages = listOf(
        OnboardPage(Icons.Filled.AccountBalanceWallet, "Track Every Promise", "Record debts lent and borrowed with amounts, due dates, and notes — all offline, all yours."),
        OnboardPage(Icons.Filled.Category, "Stay Organized", "Group by debtor, filter by status, and reuse smart templates to log debts in seconds."),
        OnboardPage(Icons.Filled.Backup, "Never Lose Data", "Export a JSON backup or schedule automatic backups so your ledger survives any device change."),
    )
    val pager = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(top = 12.dp), horizontalArrangement = Arrangement.End) {
            if (pager.currentPage < pages.lastIndex) {
                TextButton(onClick = onDone) { Text("Skip") }
            }
        }
        HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { idx ->
            val p = pages[idx]
            Column(modifier = Modifier.fillMaxSize().padding(top = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Box(modifier = Modifier.size(96.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                    Icon(p.icon, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Spacer(Modifier.height(24.dp))
                Text(p.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                Text(p.desc, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 16.dp)) {
            repeat(pages.size) { i ->
                Box(modifier = Modifier.size(if (i == pager.currentPage) 24.dp else 8.dp, 8.dp).clip(RoundedCornerShape(4.dp)).background(if (i == pager.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant))
            }
        }
        Button(
            onClick = {
                if (pager.currentPage < pages.lastIndex) scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } else onDone()
            },
            modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp)
        ) { Text(if (pager.currentPage == pages.lastIndex) "Get Started" else "Next", fontWeight = FontWeight.SemiBold) }
        Spacer(Modifier.height(24.dp))
    }
}
