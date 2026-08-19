package com.example.expense__tracker

import android.content.Context
import android.os.Environment
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

// ✅ NEW — takes context + date range
fun exportToCsv(
    context: Context,
    fromDateMillis: Long,
    toDateMillis: Long
): String {
    return try {
        val all = loadTransactions(context)
        val transactions = all.filter {
            it.timestamp in fromDateMillis..toDateMillis
        }

        if (transactions.isEmpty())
            return "No transactions found in the selected date range."

        val sdf      = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
        val fileName = "ExpenseTracker_${
            java.text.SimpleDateFormat("yyyyMMdd_HHmm", java.util.Locale.getDefault())
                .format(java.util.Date())
        }.csv"

        val downloadsDir = android.os.Environment
            .getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        if (!downloadsDir.exists()) downloadsDir.mkdirs()

        val file = java.io.File(downloadsDir, fileName)

        java.io.FileWriter(file).use { writer ->
            writer.appendLine("Date,Vendor,Bank,Category,Type,Amount")
            transactions.sortedByDescending { it.timestamp }.forEach { t ->
                writer.appendLine(
                    "${sdf.format(java.util.Date(t.timestamp))}," +
                            "\"${t.vendor}\"," +
                            "\"${t.bank}\"," +
                            "\"${t.category}\"," +
                            "${t.type}," +
                            "${t.amount}"
                )
            }
        }

        "✅ Exported ${transactions.size} transactions\n" +
                "Period: ${formatDate(fromDateMillis)} → ${formatDate(toDateMillis)}\n" +
                "Saved to Downloads/$fileName"

    } catch (e: Exception) {
        "Export failed: ${e.message}"
    }
}
