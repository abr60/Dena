package com.dena.core

import java.util.Locale
import kotlin.math.abs

fun formatCurrencyRaw(amount: Double, symbol: String, showDecimals: Boolean): String {
    val pattern = if (showDecimals) "%,.2f" else "%,.0f"
    val formatted = String.format(Locale.US, pattern, abs(amount)).replace(",", " ")
    return if (amount < 0) "-$symbol $formatted" else "$symbol $formatted"
}

fun formatSigned(amount: Double, symbol: String, negative: Boolean, showDecimals: Boolean): String {
    val pattern = if (showDecimals) "%,.2f" else "%,.0f"
    val fmt = String.format(Locale.US, pattern, abs(amount)).replace(",", " ")
    return if (negative) "-$symbol $fmt" else "$symbol $fmt"
}
