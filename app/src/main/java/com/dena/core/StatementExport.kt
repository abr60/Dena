package com.dena.core

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.widget.Toast
import com.dena.data.debt.Debt
import com.dena.data.transaction.Transaction
import com.dena.core.DenaPreferences
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale

object StatementExport {
    fun exportCsv(context: Context, debt: Debt, txs: List<Transaction>) {
        val sym = CurrencyRegistry.symbolFor(debt.currency)
        val df = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val header = "Contact,Direction,Principal,Remaining,Currency,Opened,Due,Notes\n"
        val debtLine = "\"${debt.contactName}\",${debt.direction},${debt.principalAmount},${debt.remainingBalance},${debt.currency},${df.format(debt.dateOpened)},${debt.dueDate?.let { df.format(it) } ?: ""},\"${debt.notes.replace("\"","\"\"")}\"\n"
        val txHeader = "\nTransactions: Date,Direction,Amount,Note\n"
        val txLines = txs.joinToString("\n") { t -> "${df.format(t.timestamp)},${t.direction},${t.amount},\"${t.note.replace("\"","\"\"")}\"" }
        val csv = header + debtLine + txHeader + txLines + "\n\nFinal balance: $sym ${debt.remainingBalance}\n"
        val file = File(context.getExternalFilesDir(null), "dena-${debt.contactName}-${System.currentTimeMillis()}.csv")
        file.writeText(csv)
        Toast.makeText(context, "CSV saved: ${file.name}", Toast.LENGTH_LONG).show()
        // share
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
        val showDecimals = DenaPreferences(context).showDecimals()
        val pat = if (showDecimals) "%.2f" else "%.0f"
        val df = SimpleDateFormat("dd MMM yyyy", Locale.US)
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 pts
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas
        val titlePaint = Paint().apply { textSize = 18f; isFakeBoldText = true }
        val headPaint = Paint().apply { textSize = 11f; color = android.graphics.Color.GRAY }
        val bodyPaint = Paint().apply { textSize = 11f }
        val boldPaint = Paint().apply { textSize = 11f; isFakeBoldText = true }
        var y = 40f
        canvas.drawText("Dena — Debtor Statement", 40f, y, titlePaint); y += 22f
        canvas.drawText("Contact: ${debt.contactName}  •  ${debt.direction}  •  Opened ${df.format(debt.dateOpened)}", 40f, y, bodyPaint); y += 16f
        canvas.drawText("Remaining: $sym ${String.format(Locale.US, pat, debt.remainingBalance)}  •  Original: $sym ${String.format(Locale.US, pat, debt.principalAmount)}", 40f, y, boldPaint); y += 20f
        canvas.drawText("Transactions", 40f, y, headPaint); y += 14f
        // table header
        canvas.drawText("Date", 40f, y, boldPaint); canvas.drawText("Type", 140f, y, boldPaint); canvas.drawText("Amount", 280f, y, boldPaint); canvas.drawText("Note", 380f, y, boldPaint); y += 12f
        canvas.drawLine(40f, y, 555f, y, Paint().apply { strokeWidth = 1f; color = android.graphics.Color.LTGRAY }); y += 10f
        for (t in txs) {
            if (y > 800f) break
            val dateStr = df.format(t.timestamp)
            val amt = "$sym ${String.format(Locale.US, pat, t.amount)}"
            canvas.drawText(dateStr, 40f, y, bodyPaint)
            canvas.drawText(t.direction, 140f, y, bodyPaint)
            canvas.drawText(amt, 280f, y, bodyPaint)
            canvas.drawText(t.note.take(30), 380f, y, bodyPaint)
            y += 14f
        }
        y += 10f
        canvas.drawText("Final balance: $sym ${String.format(Locale.US, pat, debt.remainingBalance)}", 40f, y, boldPaint)
        doc.finishPage(page)
        try {
            val file = File(context.getExternalFilesDir(null), "dena-${debt.contactName}-${System.currentTimeMillis()}.pdf")
            doc.writeTo(file.outputStream())
            Toast.makeText(context, "PDF saved: ${file.name}", Toast.LENGTH_LONG).show()
            try {
                val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
                val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "application/pdf"; putExtra(android.content.Intent.EXTRA_STREAM, uri); addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(android.content.Intent.createChooser(intent, "Share PDF"))
            } catch (_: Exception) {}
        } catch (e: Exception) {
            Toast.makeText(context, "PDF failed: ${e.message}", Toast.LENGTH_SHORT).show()
        } finally { doc.close() }
    }
}
