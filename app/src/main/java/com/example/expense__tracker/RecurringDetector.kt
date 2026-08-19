package com.example.expense__tracker

data class RecurringTransaction(
    val vendor: String,
    val amount: Double,
    val frequency: String,
    val totalPaid: Double,
    val count: Int,
    val nextExpected: String
)

fun detectRecurring(transactions: List<ParsedTransaction>): List<RecurringTransaction> {
    return transactions
        .filter { it.type == TransactionType.DEBIT }
        .groupBy { it.category }
        .mapNotNull { (vendor, txns) ->
            if (txns.size < 2) return@mapNotNull null

            val avgAmount = txns.sumOf { it.amount } / txns.size

            // Check if amounts are similar within 10%
            val allSimilar = txns.all {
                kotlin.math.abs(it.amount - avgAmount) / avgAmount < 0.10
            }
            if (!allSimilar) return@mapNotNull null

            // Calculate average gap in days
            val sorted = txns.sortedBy { it.timestamp }
            val gaps   = sorted.zipWithNext()
                .map { (a, b) -> (b.timestamp - a.timestamp) / (1000L * 60 * 60 * 24) }
            val avgGap = gaps.average()

            val frequency = when {
                avgGap in 25.0..35.0  -> "Monthly"
                avgGap in 6.0..8.0    -> "Weekly"
                avgGap in 13.0..16.0  -> "Bi-weekly"
                avgGap in 85.0..95.0  -> "Quarterly"
                avgGap in 1.0..3.0    -> "Daily"
                else                   -> return@mapNotNull null
            }

            // Estimate next payment
            val lastTxn      = sorted.last().timestamp
            val nextExpected = java.util.Date(lastTxn + (avgGap * 24 * 60 * 60 * 1000).toLong())
            val nextStr      = java.text.SimpleDateFormat("dd MMM yyyy",
                java.util.Locale.getDefault()).format(nextExpected)

            RecurringTransaction(
                vendor      = vendor,
                amount      = avgAmount,
                frequency   = frequency,
                totalPaid   = txns.sumOf { it.amount },
                count       = txns.size,
                nextExpected = nextStr
            )
        }
        .sortedByDescending { it.totalPaid }
}
