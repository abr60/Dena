package com.dena.core

import com.dena.data.debt.Debt
import com.dena.data.transaction.Transaction
import com.dena.data.template.MessageTemplate
import android.content.Context
import android.util.Base64
import java.nio.charset.StandardCharsets

object BackupHelper {
    data class BackupData(
        val debts: List<Debt>,
        val txs: List<Transaction>,
        val templates: List<MessageTemplate>,
        val preferences: org.json.JSONObject?,
    )

    fun exportProfileToJson(
        debts: List<Debt>,
        txs: List<Transaction>,
        templates: List<MessageTemplate> = emptyList(),
        prefsSnapshot: org.json.JSONObject? = null,
    ): String {
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
                put("contactPhone", d.contactPhone ?: org.json.JSONObject.NULL)
                put("relationship", d.relationship)
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
        val tplArr = org.json.JSONArray()
        for (tpl in templates) {
            tplArr.put(org.json.JSONObject().apply {
                put("id", tpl.id)
                put("name", tpl.name)
                put("body", tpl.body)
                put("createdAt", tpl.createdAt)
            })
        }
        val root = org.json.JSONObject().apply {
            put("format", "dena-backup")
            put("version", 2)
            put("exportedAt", System.currentTimeMillis())
            put("debts", debtsArr)
            put("transactions", txsArr)
            put("templates", tplArr)
            if (prefsSnapshot != null) put("preferences", prefsSnapshot)
        }
        return root.toString(2)
    }

    /** Capture current user-facing preferences from SharedPreferences. Excludes device/runtime keys. */
    fun capturePreferences(context: Context): org.json.JSONObject {
        val p = DenaPreferences(context)
        return org.json.JSONObject().apply {
            put("language", p.getLanguage())
            put("currency", p.getCurrency())
            put("show_decimals", p.showDecimals())
            put("show_percentage", p.showPercentage())
            put("show_date_headers", p.showDateHeaders())
            put("show_contact_number", p.showContactNumber())
            put("show_manual_phone_field", p.showManualPhoneField())
            put("terminology_mode", p.getTerminologyMode())
            put("follow_system_theme", p.isFollowSystemTheme())
            put("dark_mode", p.getDarkMode())
            put("dynamic_colors_enabled", p.isDynamicColorsEnabled())
            put("dynamic_scheme", p.getDynamicScheme())
            put("palette_id", p.getPaletteId())
            put("palette_enabled", p.isPaletteEnabled())
            put("font_scale", p.getFontScale().toDouble())
            put("display_scale", p.getDisplayScale().toDouble())
            put("app_font", p.getAppFont())
            put("history_clean_days", p.getHistoryCleanDays())
            put("backup_schedule", p.getBackupSchedule())
            put("onboarding_done", p.isOnboardingDone())
            put("unlocked", p.isUnlocked())
        }
    }

    fun applyPreferences(context: Context, prefsJson: org.json.JSONObject) {
        val p = DenaPreferences(context)
        if (prefsJson.has("language")) p.setLanguage(prefsJson.optString("language", DenaPreferences.LANG_EN))
        if (prefsJson.has("currency")) p.setCurrency(prefsJson.optString("currency", DenaPreferences.CURRENCY_BDT))
        if (prefsJson.has("show_decimals")) p.setShowDecimals(prefsJson.optBoolean("show_decimals", false))
        if (prefsJson.has("show_percentage")) p.setShowPercentage(prefsJson.optBoolean("show_percentage", false))
        if (prefsJson.has("show_date_headers")) p.setShowDateHeaders(prefsJson.optBoolean("show_date_headers", false))
        if (prefsJson.has("show_contact_number")) p.setShowContactNumber(prefsJson.optBoolean("show_contact_number", false))
        if (prefsJson.has("show_manual_phone_field")) p.setShowManualPhoneField(prefsJson.optBoolean("show_manual_phone_field", false))
        if (prefsJson.has("terminology_mode")) p.setTerminologyMode(prefsJson.optString("terminology_mode", DenaPreferences.TERM_LENT_BORROWED))
        if (prefsJson.has("follow_system_theme")) p.setFollowSystemTheme(prefsJson.optBoolean("follow_system_theme", true))
        if (prefsJson.has("dark_mode")) p.setDarkMode(prefsJson.optBoolean("dark_mode", false))
        if (prefsJson.has("dynamic_colors_enabled")) p.setDynamicColorsEnabled(prefsJson.optBoolean("dynamic_colors_enabled", false))
        if (prefsJson.has("dynamic_scheme")) p.setDynamicScheme(prefsJson.optString("dynamic_scheme", DenaPreferences.DYNAMIC_SYSTEM))
        if (prefsJson.has("palette_id")) p.setPaletteId(prefsJson.optString("palette_id", ""))
        if (prefsJson.has("palette_enabled")) p.setPaletteEnabled(prefsJson.optBoolean("palette_enabled", false))
        if (prefsJson.has("font_scale")) p.setFontScale(prefsJson.optDouble("font_scale", 1.0).toFloat())
        if (prefsJson.has("display_scale")) p.setDisplayScale(prefsJson.optDouble("display_scale", 1.0).toFloat())
        if (prefsJson.has("app_font")) p.setAppFont(prefsJson.optString("app_font", DenaPreferences.FONT_SPACEGROTESK))
        if (prefsJson.has("history_clean_days")) p.setHistoryCleanDays(prefsJson.optInt("history_clean_days", 0))
        if (prefsJson.has("backup_schedule")) p.setBackupSchedule(prefsJson.optString("backup_schedule", DenaPreferences.SCHEDULE_DISABLED))
        if (prefsJson.has("onboarding_done")) p.setOnboardingDone(prefsJson.optBoolean("onboarding_done", false))
        if (prefsJson.has("unlocked")) p.setUnlocked(prefsJson.optBoolean("unlocked", false))
        // mark templates as seeded so an empty-template backup doesn't silently re-seed defaults
        p.setTemplatesSeeded(true)
    }

    fun decodeBackupString(input: String): String? {
        val trimmed = input.trim()
        if (trimmed.startsWith("{")) return trimmed
        val b64 = if (trimmed.endsWith(".dena")) trimmed.removeSuffix(".dena") else trimmed
        return try {
            val bytes = Base64.decode(b64, Base64.NO_WRAP)
            val decoded = String(bytes, StandardCharsets.UTF_8)
            if (decoded.trimStart().startsWith("{")) decoded else null
        } catch (_: Exception) { null }
    }

    fun parseBackup(jsonStr: String): BackupData? {
        return try {
            val json = org.json.JSONObject(jsonStr)
            val debtsArr = json.optJSONArray("debts") ?: org.json.JSONArray()
            val txsArr = json.optJSONArray("transactions") ?: org.json.JSONArray()
            val tplArr = json.optJSONArray("templates")
            val prefsObj = json.optJSONObject("preferences")
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
                        contactPhone = if (o.isNull("contactPhone")) null else o.optString("contactPhone", "").ifBlank { null },
                        relationship = o.optString("relationship", "other"),
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
            val templates = mutableListOf<MessageTemplate>()
            if (tplArr != null) {
                for (i in 0 until tplArr.length()) {
                    val o = tplArr.getJSONObject(i)
                    templates.add(
                        MessageTemplate(
                            id = o.optLong("id", 0),
                            name = o.optString("name", ""),
                            body = o.optString("body", ""),
                            createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                        )
                    )
                }
            }
            BackupData(debts, txs, templates, prefsObj)
        } catch (e: Exception) {
            e.printStackTrace(); null
        }
    }

    /** Legacy compat: delegate to parseBackup. */
    fun importProfileFromString(jsonStr: String, onDebts: (List<Debt>) -> Unit, onTxs: (List<Transaction>) -> Unit): Boolean {
        val data = parseBackup(jsonStr) ?: return false
        onDebts(data.debts); onTxs(data.txs); return true
    }
}
