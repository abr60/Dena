package com.dena.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.dena.BuildConfig
import com.dena.core.ReleaseInfo
import com.dena.core.UpdateChecker
import com.dena.ui.components.SectionHeader
import com.dena.ui.components.SettingsGroup
import com.dena.ui.components.SettingsSubpageScaffold
import kotlinx.coroutines.launch

@Composable
fun UpdateSubpage(onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentVersion = BuildConfig.VERSION_NAME
    val prefs = remember(context) { com.dena.core.DenaPreferences(context) }
    var checking by remember { mutableStateOf(false) }
    var info by remember { mutableStateOf<ReleaseInfo?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var hasChecked by remember { mutableStateOf(false) }
    var storedCheck by remember { mutableLongStateOf(prefs.getLastUpdateCheck()) }
    var storedStatus by remember { mutableStateOf(prefs.getLastUpdateStatus()) }
    var storedTag by remember { mutableStateOf(prefs.getLastUpdateTag()) }

    fun check() {
        checking = true
        error = null
        scope.launch {
            val now = System.currentTimeMillis()
            UpdateChecker.fetchLatest()
                .onSuccess {
                    info = it
                    hasChecked = true
                    val newer = UpdateChecker.isNewer(it.tagName, currentVersion)
                    prefs.setLastUpdateCheck(now)
                    prefs.setLastUpdateStatus(
                        if (newer) com.dena.core.DenaPreferences.UPDATE_STATUS_AVAILABLE
                        else com.dena.core.DenaPreferences.UPDATE_STATUS_UP_TO_DATE
                    )
                    prefs.setLastUpdateTag(if (newer) it.tagName else "")
                    storedCheck = now
                    storedStatus = prefs.getLastUpdateStatus()
                    storedTag = prefs.getLastUpdateTag()
                }
                .onFailure {
                    error = it.message ?: "Failed to check"
                    hasChecked = true
                    prefs.setLastUpdateCheck(now)
                    prefs.setLastUpdateStatus(com.dena.core.DenaPreferences.UPDATE_STATUS_FAILED)
                    storedCheck = now
                    storedStatus = com.dena.core.DenaPreferences.UPDATE_STATUS_FAILED
                }
            checking = false
        }
    }

    LaunchedEffect(Unit) { check() }

    SettingsSubpageScaffold(title = "App Updates", onBack = onBack) {
        // Current version card
        SettingsGroup {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Installed", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("v$currentVersion", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Text("Tap Check for updates to see the latest release on GitHub.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // Last-check status (written by the silent auto-check on app open and by manual checks)
        SettingsGroup {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Last checked", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    if (storedCheck == 0L) "Never"
                    else java.text.SimpleDateFormat("MMM d, yyyy • h:mm a", java.util.Locale.US).format(java.util.Date(storedCheck)),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    when (storedStatus) {
                        com.dena.core.DenaPreferences.UPDATE_STATUS_AVAILABLE ->
                            if (storedTag.isNotBlank()) "Update available: $storedTag" else "Update available"
                        com.dena.core.DenaPreferences.UPDATE_STATUS_UP_TO_DATE -> "You're up to date."
                        com.dena.core.DenaPreferences.UPDATE_STATUS_FAILED -> "Last check failed — try again."
                        else -> "Automatic checks run when you open the app."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        when {
            checking -> {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("Checking for updates…", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            error != null -> {
                SettingsGroup {
                    Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Couldn't check for updates", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        Text(error ?: "", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Button(onClick = { check() }, shape = RoundedCornerShape(12.dp)) { Text("Try again") }
                    }
                }
            }
            info != null -> {
                val rel = info!!
                val newer = UpdateChecker.isNewer(rel.tagName, currentVersion)
                val sizeLabel = UpdateChecker.formatSize(rel.sizeBytes)
                if (newer) {
                    SectionHeader("UPDATE AVAILABLE")
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(rel.tagName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            if (sizeLabel.isNotEmpty()) Text("Update available → $sizeLabel", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            else Text("Update available", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            if (rel.body.isNotBlank()) {
                                Text(rel.body.take(400), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Spacer(Modifier.height(4.dp))
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_VIEW, rel.downloadUrl.toUri())
                                    context.startActivity(intent)
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary),
                            ) { Text("Download update") }
                            TextButton(onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, rel.htmlUrl.toUri())
                                context.startActivity(intent)
                            }) { Text("View release notes") }
                        }
                    }
                } else {
                    SettingsGroup {
                        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("You're up to date", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("Latest: ${rel.tagName} • Installed: v$currentVersion", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (sizeLabel.isNotEmpty()) Text(sizeLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    OutlinedButton(onClick = {
                        val intent = Intent(Intent.ACTION_VIEW, rel.htmlUrl.toUri())
                        context.startActivity(intent)
                    }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("View on GitHub") }
                }
                OutlinedButton(onClick = { check() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), enabled = !checking) {
                    Text(if (hasChecked) "Check again" else "Check for updates")
                }
            }
        }

        if (!checking && hasChecked && info == null && error == null) {
            OutlinedButton(onClick = { check() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) { Text("Check for updates") }
        }
    }
}
