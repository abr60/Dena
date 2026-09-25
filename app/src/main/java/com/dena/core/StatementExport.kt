package com.dena.core

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import com.dena.data.debt.Debt
import com.dena.data.transaction.Transaction
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

object StatementExport {
    fun exportCsv(context: Context, debt: Debt, txs: List<Transaction>) {
        val sym = CurrencyRegistry.symbolFor(debt.currency)
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val header = "Contact,Direction,Principal,Remaining,Currency,Opened,Due,Notes\n"
        val debtLine = "\"${debt.contactName}\",${debt.direction},${debt.principalAmount},${debt.remainingBalance},${debt.currency},${df.format(debt.dateOpened)},${debt.dueDate?.let { df.format(it) } ?: ""},\"${debt.notes.replace("\"","\"\"")}\"\n"
        val txHeader = "\nTransactions: Date,Direction,Amount,Note\n"
        val txLines = txs.joinToString("\n") { t -> "${df.format(t.timestamp)},${t.direction},${t.amount},\"${t.note.replace("\"","\"\"")}\"" }
        val csv = header + debtLine + txHeader + txLines + "\n\nFinal balance: $sym ${debt.remainingBalance}\n"
        val dateTag = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(System.currentTimeMillis())
        val safeName = sanitizeFileName(debt.contactName)
        val file = File(context.getExternalFilesDir(null), "dena-$safeName-$dateTag.csv")
        file.writeText(csv)
        Toast.makeText(context, "CSV saved: ${file.absolutePath}", Toast.LENGTH_LONG).show()
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                type = "text/csv"; putExtra(android.content.Intent.EXTRA_STREAM, uri); addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(intent, "Share statement"))
        } catch (_: Exception) {}
    }

    fun exportPdf(context: Context, debt: Debt, txs: List<Transaction>) {
        val sym = CurrencyRegistry.symbolFor(debt.currency)
        val showDecimals = try { DenaPreferences(context).showDecimals() } catch (_: Exception) { true }
        val df = SimpleDateFormat("dd MMM yyyy", Locale.US)
        val dfShort = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.US)

        val sorted = txs.sortedBy { it.timestamp }

        val pageW = 595f; val pageH = 842f; val margin = 45f
        val usableW = pageW - 2 * margin
        val colDateW = usableW * 0.20f
        val colTypeW = usableW * 0.30f
        val colAmountW = usableW * 0.20f
        val colNoteW = usableW * 0.30f
        val xDate = margin
        val xType = xDate + colDateW
        val xAmount = xType + colTypeW
        val xNote = xAmount + colAmountW

        // Fintech Minimal — solid header banner, full-row soft band by tx type, amounts ink-only
        val cCardBg = Color.parseColor("#F7F9FB")
        val cCardBorder = Color.parseColor("#E2E8F0")
        val cTheadBg = Color.parseColor("#1E293B")
        val cRowGreen = Color.parseColor("#F0F7F1")
        val cRowRed = Color.parseColor("#FDF4F3")
        val cRowBorder = Color.parseColor("#E9EDF2")
        val cTfootBorder = Color.parseColor("#0F172A")
        val cMuted = Color.parseColor("#64748B")
        val cInk = Color.parseColor("#0F172A")
        val cGreen = Color.parseColor("#15803D")
        val cRed = Color.parseColor("#B91C1C")
        val cTypeGreen = Color.parseColor("#166534")
        val cTypeRed = Color.parseColor("#9F1239")

        val tfSans = Typeface.create("sans-serif", Typeface.NORMAL)
        val tfSansMedium = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        val tfSansBold = Typeface.create("sans-serif", Typeface.BOLD)

        val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = tfSansMedium; textSize = 19f; color = cInk }
        val genPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = tfSans; textSize = 8.5f; color = cMuted }
        val cardFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = cCardBg }
        val cardStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = cCardBorder; strokeWidth = 1f }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = tfSans; textSize = 8.5f; color = cMuted }
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = tfSansMedium; textSize = 9f; color = cInk }
        val valueGreenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = tfSansMedium; textSize = 9f; color = cGreen }
        val phonePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = tfSans; textSize = 8f; color = cMuted }
        val theadBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = cTheadBg }
        val theadTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = tfSansBold; textSize = 10.5f; color = Color.WHITE }
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = tfSans; textSize = 9.5f; color = cInk }
        val bodyGreenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = tfSans; textSize = 9.5f; color = cGreen }
        val bodyRedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = tfSans; textSize = 9.5f; color = cRed }
        val typeGreenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = tfSansMedium; textSize = 9.5f; color = cTypeGreen }
        val typeRedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = tfSansMedium; textSize = 9.5f; color = cTypeRed }
        val rowGreenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = cRowGreen }
        val rowRedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL; color = cRowRed }
        val rowBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = cRowBorder; strokeWidth = 0.75f }
        val tfootPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = tfSansBold; textSize = 10.5f; color = cInk }
        val tfootGreenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = tfSansBold; textSize = 10.5f; color = cGreen }
        val tfootBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = cTfootBorder; strokeWidth = 2f }
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = tfSans; textSize = 8f; color = cMuted }
        val contTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = tfSansMedium; textSize = 10f; color = cMuted }

        val rowH = 22f
        val headerH = 26f
        val tfootH = 24f
        val footerH = 20f
        val cardH = 86f
        val titleH = 26f
        val genH = 16f

        val chunks: List<List<Transaction>>
        if (sorted.isEmpty()) {
            chunks = listOf(emptyList())
        } else {
            val list = mutableListOf<List<Transaction>>()
            var idx = 0
            var pageIdx = 0
            while (idx < sorted.size) {
                val isFirst = pageIdx == 0
                val topOverhead = if (isFirst) titleH + genH + cardH + 16f + headerH else 20f + headerH
                val availableForRowsBase = pageH - margin - footerH - topOverhead
                val remaining = sorted.size - idx
                val capacityNoTfoot = (availableForRowsBase / rowH).toInt()
                val capacityWithTfoot = ((availableForRowsBase - tfootH - 6f) / rowH).toInt()
                val isProbablyLast = remaining <= capacityNoTfoot
                val capacity = if (isProbablyLast) {
                    if (remaining <= capacityWithTfoot) remaining else capacityWithTfoot.coerceAtLeast(1)
                } else {
                    capacityNoTfoot.coerceAtLeast(1)
                }
                val take = minOf(capacity, remaining)
                list.add(sorted.subList(idx, idx + take))
                idx += take
                pageIdx++
                if (pageIdx > 100) break
            }
            chunks = list
        }
        val totalPages = chunks.size
        val doc = PdfDocument()

        for (pageIdx in chunks.indices) {
            val pageRows = chunks[pageIdx]
            val isFirst = pageIdx == 0
            val isLast = pageIdx == totalPages - 1
            val pageInfo = PdfDocument.PageInfo.Builder(pageW.toInt(), pageH.toInt(), pageIdx + 1).create()
            val page = doc.startPage(pageInfo)
            val canvas = page.canvas
            canvas.drawColor(Color.WHITE)

            var y = margin

            val stmtKind = if (debt.direction == "owed_to_me") "Debtor Statement" else "Creditor Statement"
            if (isFirst) {
                canvas.drawText("$stmtKind \u2014 ${debt.contactName}", margin, y + 15f, titlePaint)
                y += titleH
                canvas.drawText(dfShort.format(Date()), margin, y + 10f, genPaint)
                y += genH
                val cardTop = y
                val cardBottom = cardTop + cardH
                val cardLeft = margin
                val cardRight = margin + usableW
                canvas.drawRoundRect(cardLeft, cardTop, cardRight, cardBottom, 4f, 4f, cardFillPaint)
                canvas.drawRoundRect(cardLeft, cardTop, cardRight, cardBottom, 4f, 4f, cardStrokePaint)
                val colW = usableW / 3f
                val padLeft = 12f
                val openedStr = df.format(Date(debt.dateOpened))
                val originalStr = formatAmt(debt.principalAmount, sym, showDecimals)
                val remainingStr = formatAmt(debt.remainingBalance, sym, showDecimals)
                val statusStr = if (debt.isClosed) "Settled" else "Open"
                val dueStr = debt.dueDate?.let { df.format(Date(it)) } ?: "\u2014"
                val contactPhone = debt.contactPhone?.trim().orEmpty()
                val phoneStr = if (contactPhone.isBlank()) "\u2014" else contactPhone

                val r0LabelY = cardTop + 16f
                val r0ValueY = cardTop + 28f
                canvas.drawText("Phone", cardLeft + padLeft, r0LabelY, labelPaint)
                canvas.drawText(ellipsize(phoneStr, colW - padLeft - 8f, valuePaint), cardLeft + padLeft, r0ValueY, valuePaint)
                canvas.drawText("Opened Date", cardLeft + colW + padLeft, r0LabelY, labelPaint)
                canvas.drawText(openedStr, cardLeft + colW + padLeft, r0ValueY, valuePaint)
                canvas.drawText("Original Amount", cardLeft + 2 * colW + padLeft, r0LabelY, labelPaint)
                canvas.drawText(originalStr, cardLeft + 2 * colW + padLeft, r0ValueY, valuePaint)

                val r1LabelY = cardTop + 52f
                val r1ValueY = cardTop + 64f
                canvas.drawText("Remaining Amount", cardLeft + padLeft, r1LabelY, labelPaint)
                canvas.drawText(remainingStr, cardLeft + padLeft, r1ValueY, valuePaint)
                canvas.drawText("Status", cardLeft + colW + padLeft, r1LabelY, labelPaint)
                val statusPaint = if (debt.isClosed) valueGreenPaint else valuePaint
                canvas.drawText(statusStr, cardLeft + colW + padLeft, r1ValueY, statusPaint)
                canvas.drawText("Due Date", cardLeft + 2 * colW + padLeft, r1LabelY, labelPaint)
                canvas.drawText(dueStr, cardLeft + 2 * colW + padLeft, r1ValueY, valuePaint)

                y = cardBottom + 16f
            } else {
                canvas.drawText("$stmtKind \u2014 ${debt.contactName}  (cont.)", margin, y + 12f, contTitlePaint)
                y += 20f
            }

            val headerTop = y
            canvas.drawRect(margin, headerTop, margin + usableW, headerTop + headerH, theadBgPaint)
            val headerTextY = headerTop + 17f
            canvas.drawText("Date", xDate + 10f, headerTextY, theadTextPaint)
            canvas.drawText("Type", xType + 10f, headerTextY, theadTextPaint)
            run {
                val amtHeader = "Amount"
                val w = theadTextPaint.measureText(amtHeader)
                canvas.drawText(amtHeader, xAmount + colAmountW - 10f - w, headerTextY, theadTextPaint)
            }
            canvas.drawText("Note", xNote + 10f, headerTextY, theadTextPaint)
            y = headerTop + headerH

            for (t in pageRows) {
                val rowTop = y
                val rowBottom = rowTop + rowH
                val isPayment = t.direction != "debt_added"
                canvas.drawRect(margin, rowTop, margin + usableW, rowBottom, if (isPayment) rowGreenPaint else rowRedPaint)
                canvas.drawLine(margin, rowBottom, margin + usableW, rowBottom, rowBorderPaint)
                val textY = rowTop + 15f
                canvas.drawText(df.format(Date(t.timestamp)), xDate + 10f, textY, bodyPaint)
                canvas.drawText(humanizeTxDirection(t.direction), xType + 10f, textY, bodyPaint)
                val amtStr = formatAmt(t.amount, sym, showDecimals)
                val amtW = bodyPaint.measureText(amtStr)
                canvas.drawText(amtStr, xAmount + colAmountW - 10f - amtW, textY, bodyPaint)
                val noteRaw = if (t.note.isBlank()) "\u2014" else t.note
                val noteCellW = colNoteW - 20f
                val noteStr = ellipsize(noteRaw, noteCellW, bodyPaint)
                canvas.drawText(noteStr, xNote + 10f, textY, bodyPaint)
                y = rowBottom
            }

            if (pageRows.isEmpty() && totalPages == 1) {
                val msg = "No transactions yet"
                canvas.drawText(msg, margin + 10f, y + 15f, bodyPaint)
                y += rowH
                canvas.drawLine(margin, y, margin + usableW, y, rowBorderPaint)
            }

            if (isLast) {
                canvas.drawLine(margin, y, margin + usableW, y, tfootBorderPaint)
                val footTextY = y + 16f
                canvas.drawText("Final Balance", xDate + 10f, footTextY, tfootPaint)
                val finalAmt = formatAmt(debt.remainingBalance, sym, showDecimals)
                val amtPaint = if (debt.isClosed) tfootGreenPaint else tfootPaint
                val w = amtPaint.measureText(finalAmt)
                canvas.drawText(finalAmt, xAmount + colAmountW - 10f - w, footTextY, amtPaint)
                y += tfootH
            }

            val pgText = "Page ${pageIdx + 1} of $totalPages"
            val pgW = footerPaint.measureText(pgText)
            canvas.drawText(pgText, pageW / 2f - pgW / 2f, pageH - 22f, footerPaint)

            doc.finishPage(page)
        }

        try {
            val dateTag = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            val safeName = sanitizeFileName(debt.contactName)
            val file = File(context.getExternalFilesDir(null), "dena-$safeName-$dateTag.pdf")
            doc.writeTo(file.outputStream())
            Toast.makeText(context, "PDF saved: ${file.absolutePath}", Toast.LENGTH_LONG).show()
            try {
                val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "application/pdf"; putExtra(android.content.Intent.EXTRA_STREAM, uri); addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(android.content.Intent.createChooser(intent, "Share PDF"))
            } catch (_: Exception) {}
        } catch (e: Exception) {
            Toast.makeText(context, "PDF failed: ${e.message}", Toast.LENGTH_LONG).show()
        } finally { doc.close() }
    }

    private fun humanizeTxDirection(raw: String): String = when (raw) {
        "debt_added" -> "Debt Added"
        "payment_received" -> "Payment Received"
        "payment_made" -> "Payment Made"
        else -> raw.split("_").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
    }

    private fun formatAmt(amount: Double, symbol: String, showDecimals: Boolean): String {
        val pattern = if (showDecimals) "%,.2f" else "%,.0f"
        val formatted = String.format(Locale.US, pattern, abs(amount)).replace(",", " ")
        return if (amount < 0) "-$symbol $formatted" else "$symbol $formatted"
    }

    private fun sanitizeFileName(name: String): String {
        val s = name.trim().ifBlank { "contact" }
        return s.replace(Regex("[^A-Za-z0-9._-]+"), "_").take(40)
    }

    private fun ellipsize(text: String, maxWidth: Float, paint: Paint): String {
        if (paint.measureText(text) <= maxWidth) return text
        val ellipsis = "\u2026"
        val ellW = paint.measureText(ellipsis)
        var end = text.length
        while (end > 0) {
            val sub = text.substring(0, end)
            if (paint.measureText(sub) + ellW <= maxWidth) return sub + ellipsis
            end--
        }
        return ellipsis
    }
}
