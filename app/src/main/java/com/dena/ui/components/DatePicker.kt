package com.dena.ui.components

import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import java.util.Calendar
import java.util.TimeZone

/**
 * Dena's monochrome date picker.
 *
 * Replaces the legacy platform [android.app.DatePickerDialog] (Holo-styled,
 * ignores the app theme) with a Material3 [DatePicker] that follows
 * [MaterialTheme] — including dark mode and dynamic Material You colors.
 *
 * Millis convention: Dena stores dates as LOCAL NOON of the day
 * (set(y,m,d,12,0,0); MILLISECOND=0). Material3 operates on UTC-midnight,
 * so we round-trip across that boundary.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DenaDatePickerDialog(
    initialMillis: Long,
    onPick: (Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = localNoonToUtcMidnight(initialMillis),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPick(utcMidnightToLocalNoon(it)) }
                onDismiss()
            }) { Text("Select") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
        colors = DatePickerDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        DatePicker(
            state = state,
            showModeToggle = false,
        )
    }
}

/** Stored local-noon millis → UTC-midnight of the same day (what M3 expects). */
private fun localNoonToUtcMidnight(noonMillis: Long): Long {
    val local = Calendar.getInstance().apply { timeInMillis = noonMillis }
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
    utc.clear()
    utc.set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
    return utc.timeInMillis
}

/** M3 selectedDateMillis (UTC-midnight) → stored local-noon millis convention. */
private fun utcMidnightToLocalNoon(utcMillis: Long): Long {
    val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { timeInMillis = utcMillis }
    val local = Calendar.getInstance()
    local.clear()
    local.set(
        utc.get(Calendar.YEAR),
        utc.get(Calendar.MONTH),
        utc.get(Calendar.DAY_OF_MONTH),
        12, 0, 0,
    )
    local.set(Calendar.MILLISECOND, 0)
    return local.timeInMillis
}