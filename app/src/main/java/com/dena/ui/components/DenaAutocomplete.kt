package com.dena.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

data class AutocompleteOption(val label: String, val value: String, val flag: String = "")

@Composable
fun DenaAutocomplete(
    value: String,
    options: List<AutocompleteOption>,
    onSelect: (AutocompleteOption) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search…",
) {
    var query by remember(value) { mutableStateOf(value) }
    var expanded by remember { mutableStateOf(false) }
    val filtered = remember(query) {
        if (query.isBlank()) options else options.filter { it.label.contains(query, true) || it.value.contains(query, true) }
    }
    Box(modifier = modifier) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it; expanded = true },
            placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.outline,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )
        DropdownMenu(
            expanded = expanded && filtered.isNotEmpty(),
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(Color(0xFF1E1E1E), RoundedCornerShape(12.dp)),
        ) {
            filtered.take(6).forEach { opt ->
                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (opt.flag.isNotBlank()) Text(opt.flag, modifier = Modifier.padding(end = 8.dp))
                            Text(opt.label, color = Color(0xFFF0F0F0), style = MaterialTheme.typography.bodyMedium)
                        }
                    },
                    onClick = { query = opt.label; expanded = false; onSelect(opt) },
                )
            }
        }
    }
}
