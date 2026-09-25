package com.dena.core

object TemplateEngine {
    fun render(
        template: String,
        name: String,
        amount: String,
        relationship: String,
        dueDate: String?,
    ): String {
        var out = template
        out = out.replace("{name}", name)
        out = out.replace("{amount}", amount)
        out = out.replace("{relationship}", relationship)
        out = out.replace("{dueDate}", dueDate ?: "")
        // legacy compatibility: {duePart}
        out = out.replace("{duePart}", if (dueDate != null) " — due $dueDate" else "")
        return out
    }

    val defaultTemplates = listOf(
        "Gentle reminder" to "Hi {name},\n\njust a friendly reminder that {amount} is still outstanding{duePart}. Could you settle when convenient?\n\nThank you!",
        "I Owe — heads up" to "Hi {name},\n\nquick heads-up — I owe you {amount}{duePart}. Repaying soon, thanks for your patience!",
        "Family — warm" to "Hi {name},\n\nas family, a gentle nudge that {amount} is pending{duePart}. Let me know when works to settle — no rush.\n\nThanks!",
    )
}
