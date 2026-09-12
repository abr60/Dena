package com.dena.core

import java.util.Currency
import java.util.Locale

data class AppCurrency(
    val code: String,
    val name: String,
    val symbol: String,
)

object CurrencyRegistry {

    // Fallback map for currencies where Currency.symbol is empty or equals code
    private val fallbackSymbol = mapOf(
        "BDT" to "৳", "INR" to "₹", "PKR" to "₨", "NPR" to "₨", "LKR" to "₨",
        "USD" to "$", "EUR" to "€", "GBP" to "£", "JPY" to "¥", "CNY" to "¥",
        "KRW" to "₩", "TRY" to "₺", "RUB" to "₽", "BRL" to "R$", "AUD" to "A$",
        "CAD" to "C$", "CHF" to "CHF", "SEK" to "kr", "NOK" to "kr", "DKK" to "kr",
        "PLN" to "zł", "THB" to "฿", "VND" to "₫", "PHP" to "₱", "IDR" to "Rp",
        "MYR" to "RM", "SGD" to "S$", "HKD" to "HK$", "NZD" to "NZ$", "ZAR" to "R",
        "AED" to "د.إ", "SAR" to "﷼", "EGP" to "£", "NGN" to "₦", "KES" to "KSh",
        "GHS" to "₵", "ETB" to "Br", "MAD" to "MAD", "XOF" to "CFA", "XAF" to "FCFA",
    )

    val all: List<AppCurrency> by lazy {
        val currencies = try {
            Currency.getAvailableCurrencies().mapNotNull { cur ->
                val code = cur.currencyCode
                // Display name in English for searchability
                val name = try { cur.getDisplayName(Locale.ENGLISH) } catch (_: Exception) { code }
                val sym = try {
                    val s = cur.symbol
                    if (s.isBlank() || s == code) fallbackSymbol[code] ?: s else s
                } catch (_: Exception) { fallbackSymbol[code] ?: code }
                AppCurrency(code, name, sym ?: code)
            }.sortedBy { it.code }
        } catch (_: Exception) {
            fallbackList()
        }
        // Ensure major currencies are at top if not already sorted, but keep alphabetical for search predictability
        // Also ensure fallback list currencies missing from device are added
        val codes = currencies.map { it.code }.toSet()
        val missing = fallbackList().filter { it.code !in codes }
        (currencies + missing).sortedBy { it.code }
    }

    private fun fallbackList(): List<AppCurrency> = listOf(
        AppCurrency("USD", "US Dollar", "$"),
        AppCurrency("EUR", "Euro", "€"),
        AppCurrency("GBP", "British Pound", "£"),
        AppCurrency("JPY", "Japanese Yen", "¥"),
        AppCurrency("BDT", "Bangladeshi Taka", "৳"),
        AppCurrency("INR", "Indian Rupee", "₹"),
        AppCurrency("PKR", "Pakistani Rupee", "₨"),
        AppCurrency("CNY", "Chinese Yuan", "¥"),
        AppCurrency("AUD", "Australian Dollar", "A$"),
        AppCurrency("CAD", "Canadian Dollar", "C$"),
        AppCurrency("BRL", "Brazilian Real", "R$"),
        AppCurrency("RUB", "Russian Ruble", "₽"),
        AppCurrency("TRY", "Turkish Lira", "₺"),
        AppCurrency("KRW", "South Korean Won", "₩"),
        AppCurrency("AED", "UAE Dirham", "د.إ"),
        AppCurrency("SAR", "Saudi Riyal", "﷼"),
    )

    fun symbolFor(code: String): String {
        val upper = code.uppercase(Locale.US)
        return fallbackSymbol[upper] ?: try {
            Currency.getInstance(upper).symbol.takeIf { it.isNotBlank() } ?: upper
        } catch (_: Exception) { fallbackSymbol[upper] ?: upper }
    }

    fun find(code: String): AppCurrency? = all.find { it.code.equals(code, ignoreCase = true) }

    fun displayLabel(code: String): String {
        val c = find(code) ?: return "$code (${symbolFor(code)})"
        return "${c.code} (${c.symbol}) — ${c.name}"
    }

    fun shortLabel(code: String): String {
        val c = find(code)
        return if (c != null) "${c.code} (${c.symbol})" else "$code (${symbolFor(code)})"
    }
}
