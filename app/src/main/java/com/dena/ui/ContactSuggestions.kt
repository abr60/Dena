package com.dena.ui

import android.provider.ContactsContract
import java.util.Locale

data class ContactSuggestion(val name: String, val phone: String?)

fun queryDeviceContacts(context: android.content.Context, query: String): List<ContactSuggestion> {
    val result = LinkedHashMap<String, ContactSuggestion>()
    val esc = query.replace("%", "\\%").replace("_", "\\_")
    val like = "%$esc%"
    val isDigits = query.filter { it.isDigit() }.length >= 3
    val sel = if (isDigits) "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ? ESCAPE '\\' OR ${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ? ESCAPE '\\'"
              else "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ? ESCAPE '\\'"
    val args = if (isDigits) arrayOf(like, like) else arrayOf(like)
    try {
        context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
            sel, args, "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC",
        )?.use { c ->
            val nIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val pIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (c.moveToNext() && result.size < 8) {
                val n = if (nIdx >= 0) c.getString(nIdx)?.trim() else null
                if (n.isNullOrBlank()) continue
                val key = n.lowercase(Locale.US)
                if (result.containsKey(key)) continue
                val p = if (pIdx >= 0) c.getString(pIdx) else null
                result[key] = ContactSuggestion(n, p?.takeIf { it.isNotBlank() })
            }
        }
    } catch (_: Exception) {}
    return result.values.toList()
}
