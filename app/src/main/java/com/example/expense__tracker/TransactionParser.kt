package com.example.expense__tracker

import android.content.Context

enum class TransactionType { CREDIT, DEBIT }

data class ParsedTransaction(
    val rawSms: String,
    val amount: Double,
    val type: TransactionType,
    val bank: String,
    val vendor: String,
    val category: String,
    val timestamp: Long
)

private val amountRegex = Regex("(?:Rs\\.?|INR)\\s*([\\d,]+(?:\\.\\d{1,2})?)", RegexOption.IGNORE_CASE)

private val creditKeywords = listOf("credited", "received", "deposited")
private val debitKeywords = listOf("debited", "spent", "withdrawn", "paid", "sent")

private val knownBanks = listOf(
    "Kotak Bank", "Axis Bank", "HDFC Bank", "ICICI Bank", "State Bank of India",
    "SBI", "Yes Bank", "IDFC First Bank", "Punjab National Bank", "Bank of Baroda",
    "Canara Bank", "Union Bank"
)

private val merchantKeywords = listOf(
    "Zomato", "Swiggy", "Amazon", "Flipkart", "Uber", "Ola", "Netflix",
    "Myntra", "BigBasket", "Blinkit", "Zepto", "Paytm", "PhonePe"
)

fun isTransactionSms(sms: String): Boolean {
    val lower = sms.lowercase()
    val hasAmount = amountRegex.containsMatchIn(sms)
    val hasKeyword = (creditKeywords + debitKeywords).any { lower.contains(it) }
    return hasAmount && hasKeyword
}

private fun extractAmount(sms: String): Double? {
    val match = amountRegex.find(sms) ?: return null
    return match.groupValues[1].replace(",", "").toDoubleOrNull()
}

private fun extractType(sms: String): TransactionType? {
    val lower = sms.lowercase()
    return when {
        creditKeywords.any { lower.contains(it) } -> TransactionType.CREDIT
        debitKeywords.any { lower.contains(it) } -> TransactionType.DEBIT
        else -> null
    }
}

private fun extractBank(sms: String): String {
    return knownBanks.firstOrNull { sms.contains(it, ignoreCase = true) } ?: "Unknown Bank"
}

private fun cleanVendorName(raw: String): String {
    var name = raw.trim()
    if (name.contains("@")) name = name.substringBefore("@")
    if (Regex("^cc\\.\\d+", RegexOption.IGNORE_CASE).containsMatchIn(name)) {
        return "Credit Card Bill Payment"
    }
    // strip leftover bank/account phrasing that sometimes leaks into the captured text
    name = name.replace(Regex("your\\s+", RegexOption.IGNORE_CASE), "")
    return name.ifBlank { "Unknown" }
}

private fun extractVendor(sms: String): String {
    val lines = sms.lines().map { it.trim() }.filter { it.isNotBlank() }

    // Strategy 1: structured UPI line e.g. "UPI/P2A/617255170575/GAYATHRI /UTIB/UPI - Axis Bank"
    val upiLine = lines.firstOrNull { it.contains("UPI/") }
    if (upiLine != null) {
        val parts = upiLine.split("/")
        if (parts.size >= 4) {
            val name = parts[3].trim()
            if (name.isNotBlank() && !name.matches(Regex("\\d+"))) {
                return cleanVendorName(name)
            }
        }
    }

    // Strategy 2: "... to <vendor> on <date>" — sent/debited transfers.
    // Checked BEFORE the "from" pattern below, because messages like
    // "Sent Rs.X from your Kotak Bank AC X6973 to VENDOR on DATE" contain both
    // "from" and "to" — without this priority, the "from" pattern would swallow
    // the bank/account text along with the real vendor name.
    val toMatch = Regex("\\bto\\s+(.+?)\\s+on\\s+\\d{2}", RegexOption.IGNORE_CASE).find(sms)
    if (toMatch != null) {
        return cleanVendorName(toMatch.groupValues[1])
    }

    // Strategy 3: "... from <vendor> on <date>" — received/credited transfers
    val fromMatch = Regex("\\bfrom\\s+(.+?)\\s+on\\s+\\d{2}", RegexOption.IGNORE_CASE).find(sms)
    if (fromMatch != null) {
        return cleanVendorName(fromMatch.groupValues[1])
    }

    // Strategy 4: card transactions — merchant sits on its own line right after the date/time line
    val dateLineIndex = lines.indexOfFirst { Regex("\\d{2}-\\d{2}-\\d{2}").containsMatchIn(it) }
    if (dateLineIndex != -1 && dateLineIndex + 1 < lines.size) {
        val candidate = lines[dateLineIndex + 1]
        if (!candidate.contains("Avl", ignoreCase = true) &&
            !candidate.contains("Limit", ignoreCase = true) &&
            !candidate.contains("Not you", ignoreCase = true)
        ) {
            return cleanVendorName(candidate)
        }
    }

    return "Unknown"
}

private fun categorize(vendor: String): String {
    return merchantKeywords.firstOrNull { vendor.contains(it, ignoreCase = true) } ?: vendor
}

fun parseSms(body: String, timestamp: Long): ParsedTransaction? {
    if (!isTransactionSms(body)) return null

    val amount = extractAmount(body) ?: return null
    val type = extractType(body) ?: return null
    val bank = extractBank(body)
    val vendor = extractVendor(body)
    val category = categorize(vendor)

    return ParsedTransaction(body, amount, type, bank, vendor, category, timestamp)
}

fun loadTransactions(context: Context): List<ParsedTransaction> {
    return readSmsMessages(context).mapNotNull { parseSms(it.body, it.timestamp) }
}