package com.dena.ui.components

import android.content.Intent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.dena.core.ReleaseInfo
import com.dena.core.UpdateChecker

@Composable
fun UpdateAvailableDialog(
    info: ReleaseInfo,
    onDismiss: () -> Unit,
    onDismissVersion: () -> Unit,
) {
    val ctx = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Update available: ${info.tagName}", fontWeight = FontWeight.SemiBold) },
        text = {
            val size = UpdateChecker.formatSize(info.sizeBytes)
            val subtitle = if (size.isNotEmpty()) "${info.name} • $size" else info.name
            val body = info.body.take(600).ifBlank { "A new version is available on GitHub." }
            Text("$subtitle\n\n$body", style = MaterialTheme.typography.bodyMedium)
        },
        confirmButton = {
            TextButton(onClick = {
                val intent = Intent(Intent.ACTION_VIEW, android.net.Uri.parse(info.htmlUrl))
                ctx.startActivity(intent)
                onDismiss()
            }) { Text("Update") }
        },
        dismissButton = {
            TextButton(onClick = onDismissVersion) { Text("Dismiss") }
        },
    )
}
