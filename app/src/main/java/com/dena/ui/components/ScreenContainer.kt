package com.dena.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ScreenContainer(
    title: String,
    searchQuery: String? = null,
    onSearchChange: ((String) -> Unit)? = null,
    searchPlaceholder: String = "Search debtor…",
    content: @Composable () -> Unit,
) {
    var searchOpen by rememberSaveable { mutableStateOf(false) }
    // auto-open if query present
    if (searchQuery != null && searchQuery.isNotEmpty() && !searchOpen) searchOpen = true

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp),
    ) {
        // Header: app name left, search right — search replaces title inline, no layout shift below
        Row(
            modifier = Modifier.fillMaxWidth().height(56.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (searchOpen && onSearchChange != null) {
                OutlinedTextField(
                    value = searchQuery ?: "",
                    onValueChange = onSearchChange,
                    placeholder = { Text(searchPlaceholder) },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        IconButton(onClick = {
                            onSearchChange("")
                            searchOpen = false
                        }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close search")
                        }
                    },
                    singleLine = true,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                )
            } else {
                // Symmetrical: 48dp leading spacer balances trailing search icon (48dp)
                Spacer(modifier = Modifier.width(48.dp))
                Text(
                    text = "DENA",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace,
                    letterSpacing = 3.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                if (onSearchChange != null) {
                    IconButton(
                        onClick = { searchOpen = true },
                        modifier = Modifier.offset(y = 4.dp),
                    ) {
                        Icon(Icons.Filled.Search, contentDescription = "Search")
                    }
                } else {
                    Spacer(modifier = Modifier.width(48.dp))
                }
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = 12.dp, bottom = 20.dp),
        ) {
            content()
        }
    }
}
