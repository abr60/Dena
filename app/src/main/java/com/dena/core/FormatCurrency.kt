package com.dena.core

import android.content.Context
import java.util.Locale
import kotlin.math.abs

fun formatCurrency(context: Context, amount: Double, symbol: String, withSign: Boolean = false): String {
    val prefs = DenaPreferences(context)
    val showDecimals = prefs.showDecimals()
    val useSign = withSign && amount < 0
    val absAmt = abs(amount)
    val pattern = if (showDecimals) "%,.2f" else "%,.0f"
    val formatted = String.format(Locale.US, pattern, absAmt).replace(",", " ")
    val amtText = if (useSign) "-$symbol $formatted" else "$symbol $formatted"
    // if withSign false, amount already signed externally; just return symbol + formatted with sign
    return if (!withSign && amount < 0) "-$symbol $formatted" else amtText
}

fun formatCurrencyRaw(amount: Double, symbol: String, showDecimals: Boolean): String {
    val pattern = if (showDecimals) "%,.2f" else "%,.0f"
    val formatted = String.format(Locale.US, pattern, abs(amount)).replace(",", " ")
    return if (amount < 0) "-$symbol $formatted" else "$symbol $formatted"
}

fun formatTakaSigned(amount: Double, symbol: String, isOwedToMe: Boolean, showDecimals: Boolean = true): String {
    val pattern = if (showDecimals) "%,.2f" else "%,.0f"
    val absAmt = abs(amount)
    val fmt = String.format(Locale.US, pattern, absAmt).replace(",", " ")
    val sign = if (isOwedToMe) "" else "-"
    return "$sign$symbol $fmt"
}

fun formatSigned(amount: Double, symbol: String, negative: Boolean, showDecimals: Boolean): String {
    val pattern = if (showDecimals) "%,.2f" else "%,.0f"
    val fmt = String.format(Locale.US, pattern, abs(amount)).replace(",", " ")
    return if (negative) "-$symbol $fmt" else "$symbol $fmt"
}
