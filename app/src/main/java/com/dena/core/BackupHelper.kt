package com.dena.core

import com.dena.data.debt.Debt
import com.dena.data.transaction.Transaction
import android.util.Base64
import java.nio.charset.StandardCharsets

object BackupHelper {
    fun exportProfileToString(debts: List<Debt>, txs: List<Transaction>): String {
        val json = buildString {
            append("{\"debts\":")
            append(debtsToJson(debts))
            append(",\"transactions\":")
            append(txsToJson(txs))
            append(",\"exportedAt\":${System.currentTimeMillis()}}")
        }
        return Base64.encodeToString(json.toByteArray(StandardCharsets.UTF_8), Base64.NO_WRAP) + ".dena"
    }

    fun exportProfileToJson(debts: List<Debt>, txs: List<Transaction>): String {
        val debtsArr = org.json.JSONArray()
        for (d in debts) {
            debtsArr.put(org.json.JSONObject().apply {
                put("id", d.id)
                put("contactName", d.contactName)
                put("direction", d.direction)
                put("principalAmount", d.principalAmount)
                put("remainingBalance", d.remainingBalance)
                put("currency", d.currency)
                put("dateOpened", d.dateOpened)
                put("dueDate", d.dueDate ?: org.json.JSONObject.NULL)
                put("category", d.category)
                put("notes", d.notes)
                put("isClosed", d.isClosed)
                put("creationDate", d.creationDate)
                put("createdAt", d.createdAt)
                put("updatedAt", d.updatedAt)
            })
        }
        val txsArr = org.json.JSONArray()
        for (t in txs) {
            txsArr.put(org.json.JSONObject().apply {
                put("id", t.id)
                put("debtId", t.debtId)
                put("amount", t.amount)
                put("direction", t.direction)
                put("timestamp", t.timestamp)
                put("note", t.note)
                put("stableId", t.stableId)
            })
        }
        val root = org.json.JSONObject().apply {
            put("debts", debtsArr)
            put("transactions", txsArr)
        }
        return root.toString(2) // 2-space indent
    }

    fun decodeBackupString(input: String): String? {
        val trimmed = input.trim()
        // Raw JSON (new export format) — return as-is without attempting Base64
        if (trimmed.startsWith("{")) return trimmed
        // Legacy Base64 format — strip .dena suffix, decode, verify result is JSON
        val b64 = if (trimmed.endsWith(".dena")) trimmed.removeSuffix(".dena") else trimmed
        return try {
            val bytes = Base64.decode(b64, Base64.NO_WRAP)
            val decoded = String(bytes, StandardCharsets.UTF_8)
            if (decoded.trimStart().startsWith("{")) decoded else null
        } catch (_: Exception) {
            null
        }
    }

    fun debtsToJson(debts: List<Debt>): String =
        debts.joinToString(prefix = "[", postfix = "]", separator = ",") { d ->
            """{"id":${d.id},"contactName":${jsonStr(d.contactName)},"direction":${jsonStr(d.direction)},"principalAmount":${d.principalAmount},"remainingBalance":${d.remainingBalance},"currency":${jsonStr(d.currency)},"dateOpened":${d.dateOpened},"dueDate":${d.dueDate},"category":${jsonStr(d.category)},"notes":${jsonStr(d.notes)},"isClosed":${d.isClosed},"creationDate":${d.creationDate},"createdAt":${d.createdAt},"updatedAt":${d.updatedAt}}"""
        }

    fun txsToJson(txs: List<Transaction>): String =
        txs.joinToString(prefix = "[", postfix = "]", separator = ",") { t ->
            """{"id":${t.id},"debtId":${t.debtId},"amount":${t.amount},"direction":${jsonStr(t.direction)},"timestamp":${t.timestamp},"note":${jsonStr(t.note)},"stableId":${jsonStr(t.stableId)}}"""
        }

    private fun jsonStr(s: String): String = "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""

    // Very small json parser for import — extract debts/transactions arrays naively using regex-free manual scan would be heavy.
    // We delegate to org.json if available (Android has it). Use org.json.
    fun importProfileFromString(jsonStr: String, onDebts: (List<Debt>) -> Unit, onTxs: (List<Transaction>) -> Unit): Boolean {
        return try {
            val json = org.json.JSONObject(jsonStr)
            val debtsArr = json.optJSONArray("debts") ?: org.json.JSONArray()
            val txsArr = json.optJSONArray("transactions") ?: org.json.JSONArray()
            val debts = mutableListOf<Debt>()
            for (i in 0 until debtsArr.length()) {
                val o = debtsArr.getJSONObject(i)
                debts.add(
                    Debt(
                        id = o.optLong("id", 0),
                        contactName = o.optString("contactName", ""),
                        contactAvatar = null,
                        direction = o.optString("direction", "owed_to_me"),
                        principalAmount = o.optDouble("principalAmount", 0.0),
                        remainingBalance = o.optDouble("remainingBalance", 0.0),
                        currency = o.optString("currency", "BDT"),
                        dateOpened = o.optLong("dateOpened", System.currentTimeMillis()),
                        dueDate = if (o.isNull("dueDate")) null else o.optLong("dueDate"),
                        category = o.optString("category", "Other"),
                        notes = o.optString("notes", ""),
                        isClosed = o.optBoolean("isClosed", false),
                        creationDate = o.optLong("creationDate", o.optLong("dateOpened", System.currentTimeMillis())),
                        createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
                    )
                )
            }
            val txs = mutableListOf<Transaction>()
            for (i in 0 until txsArr.length()) {
                val o = txsArr.getJSONObject(i)
                txs.add(
                    Transaction(
                        id = o.optLong("id", 0),
                        debtId = o.optLong("debtId", 0),
                        amount = o.optDouble("amount", 0.0),
                        direction = o.optString("direction", "payment_received"),
                        timestamp = o.optLong("timestamp", System.currentTimeMillis()),
                        note = o.optString("note", ""),
                        stableId = o.optString("stableId", ""),
                    )
                )
            }
            onDebts(debts)
            onTxs(txs)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
