package com.dena.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class SelectOption(
    val label: String,
    val swatches: List<Color> = emptyList(),
    val group: String? = null,
    val originalIndex: Int = -1,
    // legacy single-dot support (unused now but kept for compat)
    val dotColor: Color? = null,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DenaSelect(
    value: String,
    options: List<SelectOption>,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = options.find { it.label == value }
    val selectedSwatches = selected?.swatches ?: emptyList()
    val bringRequester = remember { BringIntoViewRequester() }
    val innerScroll = rememberScrollState()

    LaunchedEffect(expanded) {
        if (expanded) {
            // let layout settle then ask parent scroll (SettingsSubpageScaffold's verticalScroll) to bring dropdown into view
            kotlinx.coroutines.delay(80)
            try { bringRequester.bringIntoView() } catch (_: Exception) {}
        } else {
            innerScroll.scrollTo(0)
        }
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                .clickable { expanded = !expanded }
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selectedSwatches.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                    selectedSwatches.take(5).forEach { c ->
                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(c).border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape))
                    }
                }
                Spacer(Modifier.size(8.dp))
            } else if (selected?.dotColor != null) {
                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(selected.dotColor))
                Spacer(Modifier.size(8.dp))
            }
            Text(
                text = value.ifBlank { placeholder },
                style = MaterialTheme.typography.bodyMedium,
                color = if (value.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (expanded) "▲" else "▼",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (expanded) {
            Spacer(Modifier.size(6.dp))
            Column(
                modifier = Modifier.fillMaxWidth()
                    .heightIn(max = 280.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .bringIntoViewRequester(bringRequester)
                    .verticalScroll(innerScroll)
                    .padding(vertical = 6.dp),
            ) {
                // Group by `group` preserving category order; fallback to flat if no groups
                val hasGroups = options.any { it.group != null }
                if (hasGroups) {
                    val order = listOf("Everyday", "Mood", "Retro", "Minimal")
                    val grouped = options.groupBy { it.group }
                    // Render in defined order, then any leftovers
                    val orderedKeys = (order.filter { grouped.containsKey(it) } + grouped.keys.filter { it !in order })
                    orderedKeys.forEach { g ->
                        val groupOpts = grouped[g] ?: emptyList()
                        if (g != null) {
                            Text(
                                text = g.uppercase(),
                                style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.8.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                        groupOpts.forEach { opt ->
                            val isSelected = opt.label == value
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                                    .clickable {
                                        expanded = false
                                        val idx = if (opt.originalIndex >= 0) opt.originalIndex else options.indexOf(opt)
                                        onSelect(idx)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 9.dp),
                            ) {
                                if (opt.swatches.isNotEmpty()) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                        opt.swatches.take(5).forEach { c ->
                                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(c).border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), CircleShape))
                                        }
                                    }
                                    Spacer(Modifier.size(8.dp))
                                } else if (opt.dotColor != null) {
                                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(opt.dotColor))
                                    Spacer(Modifier.size(8.dp))
                                }
                                Text(
                                    text = opt.label,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    modifier = Modifier.weight(1f),
                                )
                                if (isSelected) {
                                    Text(
                                        text = "✓",
                                        color = MaterialTheme.colorScheme.primary,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }
                    }
                } else {
                    options.forEachIndexed { idx, opt ->
                        val isSelected = opt.label == value
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                                .clickable {
                                    expanded = false
                                    val realIdx = if (opt.originalIndex >= 0) opt.originalIndex else idx
                                    onSelect(realIdx)
                                }
                                .padding(horizontal = 14.dp, vertical = 9.dp),
                        ) {
                            if (opt.swatches.isNotEmpty()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    opt.swatches.take(5).forEach { c ->
                                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(c))
                                    }
                                }
                                Spacer(Modifier.size(8.dp))
                            }
                            Text(
                                text = opt.label,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                modifier = Modifier.weight(1f),
                            )
                            if (isSelected) Text("✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
