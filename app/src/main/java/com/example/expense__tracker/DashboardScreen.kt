package com.example.expense__tracker

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.*

enum class DashboardTab { SUMMARY, TRANSACTIONS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    allTransactions: List<ParsedTransaction>,
    hasScanned: Boolean,
    fromDateMillis: Long,
    toDateMillis: Long,
    onTransactionsLoaded: (List<ParsedTransaction>) -> Unit,
    onFromDateChanged: (Long) -> Unit,
    onToDateChanged: (Long) -> Unit
) {
    val context          = LocalContext.current
    var activeTab        by remember { mutableStateOf(DashboardTab.SUMMARY) }
    var showFromPicker   by remember { mutableStateOf(false) }
    var showToPicker     by remember { mutableStateOf(false) }
    var isRescanning     by remember { mutableStateOf(false) }
    var rescanMessage    by remember { mutableStateOf("") }
    var showRescanDialog by remember { mutableStateOf(false) }
    var searchQuery      by remember { mutableStateOf("") }

    val budget = BudgetManager.getBudget(context)

    val filtered = remember(allTransactions, fromDateMillis, toDateMillis) {
        allTransactions.filter { it.timestamp in fromDateMillis..toDateMillis }
    }

    val totalSpent    = filtered.filter { it.type == TransactionType.DEBIT }.sumOf { it.amount }
    val totalReceived = filtered.filter { it.type == TransactionType.CREDIT }.sumOf { it.amount }
    val totalTxns     = filtered.size
    val debitTxns     = filtered.count { it.type == TransactionType.DEBIT }
    val creditTxns    = filtered.count { it.type == TransactionType.CREDIT }
    val isOverBudget  = budget > 0 && totalSpent > budget

    val sortedTxns by remember(filtered, searchQuery) {
        derivedStateOf {
            filtered
                .filter {
                    searchQuery.isBlank() ||
                            it.vendor.contains(searchQuery, ignoreCase = true) ||
                            it.bank.contains(searchQuery, ignoreCase = true) ||
                            it.category.contains(searchQuery, ignoreCase = true)
                }
                .sortedByDescending { it.timestamp }
        }
    }

    LaunchedEffect(totalSpent, fromDateMillis, toDateMillis) {
        NotificationHelper.checkAndNotify(context, totalSpent, budget)
    }

    val debitByVendor = filtered
        .filter { it.type == TransactionType.DEBIT }
        .groupBy { it.category }
        .mapValues { (_, t) -> t.sumOf { it.amount } to t.size }
        .toList().sortedByDescending { it.second.first }

    val creditByVendor = filtered
        .filter { it.type == TransactionType.CREDIT }
        .groupBy { it.vendor }
        .mapValues { (_, t) -> t.sumOf { it.amount } to t.size }
        .toList().sortedByDescending { it.second.first }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            val loaded = loadTransactions(context)
            onTransactionsLoaded(loaded)
            val spent = loaded
                .filter { it.timestamp in fromDateMillis..toDateMillis }
                .filter { it.type == TransactionType.DEBIT }
                .sumOf { it.amount }
            NotificationHelper.checkAndNotify(context, spent, BudgetManager.getBudget(context))
        } else {
            Toast.makeText(context, "SMS permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(AppBg)) {

        if (!hasScanned) {
            // ── Pre-scan CTA ──────────────────────────────
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier.size(88.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(listOf(AccentGreen, AccentGreen2))),
                    contentAlignment = Alignment.Center
                ) {
                    Text("₹", fontSize = 40.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(24.dp))
                Text("Expense Tracker", fontSize = 26.sp,
                    fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Scan your SMS inbox to auto-detect\ncredit & debit transactions",
                    color = TextSecondary, fontSize = 14.sp,
                    textAlign = TextAlign.Center, lineHeight = 20.sp
                )
                Spacer(Modifier.height(40.dp))
                Button(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(
                                context, Manifest.permission.READ_SMS)
                            == PackageManager.PERMISSION_GRANTED
                        ) {
                            val loaded = loadTransactions(context)
                            onTransactionsLoaded(loaded)
                            val spent = loaded
                                .filter { it.timestamp in fromDateMillis..toDateMillis }
                                .filter { it.type == TransactionType.DEBIT }
                                .sumOf { it.amount }
                            NotificationHelper.checkAndNotify(
                                context, spent, BudgetManager.getBudget(context))
                        } else {
                            smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                ) {
                    Text("Scan SMS for Transactions", fontWeight = FontWeight.Bold,
                        fontSize = 16.sp, color = Color(0xFF0F1629))
                }
                Spacer(Modifier.height(16.dp))
                Text("We only read transaction SMS — nothing is uploaded",
                    color = TextSecondary, fontSize = 12.sp, textAlign = TextAlign.Center)
            }

        } else {

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 52.dp, bottom = 32.dp)
            ) {

                // Title
                item {
                    Text("Dashboard", fontSize = 26.sp,
                        fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(Modifier.height(4.dp))
                    Text("${filtered.size} transactions found",
                        color = TextSecondary, fontSize = 13.sp)
                }

                // Budget warning
                if (isOverBudget) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFF3D1A1A))
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("⚠️", fontSize = 24.sp)
                            Column {
                                Text("Budget Exceeded!", color = Color(0xFFFF6B6B),
                                    fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text(
                                    "Spent ${formatAmount(totalSpent)} of ${formatAmount(budget)} " +
                                            "(+${formatAmount(totalSpent - budget)} over)",
                                    color = Color(0xFFFFAAAA), fontSize = 12.sp)
                            }
                        }
                    }
                } else if (budget > 0) {
                    item {
                        val percent  = (totalSpent / budget * 100).toInt()
                        val barColor = if (percent > 80) Color(0xFFFF922B) else AccentGreen
                        Column(
                            modifier = Modifier.fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(CardBg).padding(14.dp)
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Monthly Budget", color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("${formatAmount(totalSpent)} / ${formatAmount(budget)} ($percent%)",
                                    color = barColor, fontSize = 12.sp)
                            }
                            Spacer(Modifier.height(8.dp))
                            Box(modifier = Modifier.fillMaxWidth().height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Color(0xFF253150))) {
                                Box(modifier = Modifier
                                    .fillMaxWidth((totalSpent/budget).toFloat().coerceAtMost(1f))
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(barColor))
                            }
                            if (percent in 80..99) {
                                Spacer(Modifier.height(6.dp))
                                Text("⚠️ Approaching budget limit",
                                    color = Color(0xFFFF922B), fontSize = 11.sp)
                            }
                        }
                    }
                }

                // Date range card
                item {
                    Column(modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBg).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Select Time Frame", color = TextPrimary,
                            fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Row(modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            DateChip("From", formatDate(fromDateMillis), Modifier.weight(1f)) {
                                showFromPicker = true }
                            DateChip("To", formatDate(toDateMillis), Modifier.weight(1f)) {
                                showToPicker = true }
                        }
                        OutlinedButton(
                            onClick = {
                                if (ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.READ_SMS)
                                    == PackageManager.PERMISSION_GRANTED
                                ) {
                                    isRescanning = true
                                    val prev  = allTransactions.size
                                    val fresh = loadTransactions(context)
                                    onTransactionsLoaded(fresh)
                                    val spent = fresh
                                        .filter { it.timestamp in fromDateMillis..toDateMillis }
                                        .filter { it.type == TransactionType.DEBIT }
                                        .sumOf { it.amount }
                                    NotificationHelper.checkAndNotify(
                                        context, spent, BudgetManager.getBudget(context))
                                    isRescanning  = false
                                    rescanMessage = if (fresh.size - prev > 0)
                                        "Found ${fresh.size - prev} new transaction(s)!"
                                    else "No new transactions. All ${fresh.size} are up to date."
                                    showRescanDialog = true
                                } else {
                                    smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentGreen),
                            border = BorderStroke(1.dp, AccentGreen),
                            enabled = !isRescanning
                        ) {
                            Text(if (isRescanning) "Scanning…" else "🔄  Re-scan SMS",
                                fontWeight = FontWeight.Medium)
                        }
                    }
                }

                // Account summary
                item {
                    Column(modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardBg).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Account Summary", color = TextPrimary,
                            fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            SummaryCard("Total Spent",    totalSpent,
                                Color(0xFFFF6B6B), Modifier.weight(1f))
                            SummaryCard("Total Received", totalReceived,
                                AccentGreen,       Modifier.weight(1f))
                        }
                        HorizontalDivider(color = FieldBorder, thickness = 0.5.dp)
                        Row(Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween) {
                            StatPill("Total",   totalTxns,  TextSecondary)
                            StatPill("Debits",  debitTxns,  Color(0xFFFF6B6B))
                            StatPill("Credits", creditTxns, AccentGreen)
                        }
                    }
                }

                // Tab toggle
                item {
                    Row(modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardBg).padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        TabButton("Summary", activeTab == DashboardTab.SUMMARY,
                            Modifier.weight(1f)) { activeTab = DashboardTab.SUMMARY }
                        TabButton("Each Transaction", activeTab == DashboardTab.TRANSACTIONS,
                            Modifier.weight(1f)) { activeTab = DashboardTab.TRANSACTIONS }
                    }
                }

                // ════ SUMMARY TAB ════════════════════════
                if (activeTab == DashboardTab.SUMMARY) {

                    item {
                        Row(modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF2A1A1A))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("💸  Money Spent", color = Color(0xFFFF6B6B),
                                fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("${debitByVendor.size} vendors · ${formatAmount(totalSpent)}",
                                color = Color(0xFFFF6B6B), fontSize = 12.sp)
                        }
                    }
                    if (debitByVendor.isEmpty()) {
                        item {
                            Text("No debit transactions in this range.",
                                color = TextSecondary, textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))
                        }
                    } else {
                        items(debitByVendor) { (vendor, pair) ->
                            VendorSummaryRow(vendor, pair.first, pair.second, totalSpent, true)
                        }
                    }

                    item {
                        Spacer(Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF0D2A1F))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("💰  Money Received", color = AccentGreen,
                                fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("${creditByVendor.size} sources · ${formatAmount(totalReceived)}",
                                color = AccentGreen, fontSize = 12.sp)
                        }
                    }
                    if (creditByVendor.isEmpty()) {
                        item {
                            Text("No credit transactions in this range.",
                                color = TextSecondary, textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))
                        }
                    } else {
                        items(creditByVendor) { (vendor, pair) ->
                            VendorSummaryRow(vendor, pair.first, pair.second, totalReceived, false)
                        }
                    }
                }

                // ════ TRANSACTIONS TAB ════════════════════
                if (activeTab == DashboardTab.TRANSACTIONS) {

                    // ── Search bar (single, no duplicate) ─
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("Search vendor, bank or category") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = fieldColors
                        )

                        if (searchQuery.isNotBlank()) {
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "${sortedTxns.size} result(s) for \"$searchQuery\"",
                                color = AccentGreen, fontSize = 12.sp
                            )

                            // ── Filtered totals mini card ─
                            if (sortedTxns.isNotEmpty()) {
                                Spacer(Modifier.height(6.dp))
                                val searchDebit  = sortedTxns
                                    .filter { it.type == TransactionType.DEBIT }
                                    .sumOf { it.amount }
                                val searchCredit = sortedTxns
                                    .filter { it.type == TransactionType.CREDIT }
                                    .sumOf { it.amount }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(CardBg)
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Spent:", color = TextSecondary, fontSize = 12.sp)
                                    Text(formatAmount(searchDebit),
                                        color = Color(0xFFFF6B6B),
                                        fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text("Received:", color = TextSecondary, fontSize = 12.sp)
                                    Text(formatAmount(searchCredit),
                                        color = AccentGreen,
                                        fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // Column headers
                    item {
                        Row(modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF253150))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Date / Vendor", color = TextSecondary, fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(2f))
                            Text("Debit", color = Color(0xFFFF6B6B), fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                            Text("Credit", color = AccentGreen, fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                        }
                    }

                    if (sortedTxns.isEmpty()) {
                        item {
                            Text(
                                if (searchQuery.isBlank()) "No transactions in this range."
                                else "No results for \"$searchQuery\".",
                                color = TextSecondary, textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
                            )
                        }
                    } else {
                        items(sortedTxns, key = { it.timestamp.toString() + it.vendor }) { txn ->
                            TransactionRow(txn)
                        }
                    }

                    // Footer — uses search-filtered totals
                    item {
                        val footerDebit  = sortedTxns
                            .filter { it.type == TransactionType.DEBIT }
                            .sumOf { it.amount }
                        val footerCredit = sortedTxns
                            .filter { it.type == TransactionType.CREDIT }
                            .sumOf { it.amount }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF253150))
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            if (searchQuery.isNotBlank()) {
                                Text(
                                    "Filtered total for \"$searchQuery\"",
                                    color = AccentGreen, fontSize = 11.sp,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    if (searchQuery.isBlank()) "TOTAL" else "TOTAL (filtered)",
                                    color = TextPrimary, fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(2f)
                                )
                                Text(formatAmount(footerDebit),
                                    color = Color(0xFFFF6B6B),
                                    fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1f))
                                Text(formatAmount(footerCredit),
                                    color = AccentGreen,
                                    fontSize = 13.sp, fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.End,
                                    modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }

    // Rescan dialog
    if (showRescanDialog) {
        AlertDialog(
            onDismissRequest = { showRescanDialog = false },
            containerColor = CardBg,
            title = { Text("Re-scan Complete", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text  = { Text(rescanMessage, color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { showRescanDialog = false }) {
                    Text("OK", color = AccentGreen, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // From date picker
    if (showFromPicker) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showFromPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onFromDateChanged(startOfLocalDay(it)) }
                    showFromPicker = false
                }) { Text("OK", color = AccentGreen) }
            },
            dismissButton = {
                TextButton(onClick = { showFromPicker = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        ) { DatePicker(state = state) }
    }

    // To date picker
    if (showToPicker) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showToPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { onToDateChanged(endOfLocalDay(it)) }
                    showToPicker = false
                }) { Text("OK", color = AccentGreen) }
            },
            dismissButton = {
                TextButton(onClick = { showToPicker = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        ) { DatePicker(state = state) }
    }
}

// ── Sub-composables ───────────────────────────────────────────

@Composable
fun TabButton(text: String, selected: Boolean,
              modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) AccentGreen else Color.Transparent,
            contentColor   = if (selected) Color(0xFF0F1629) else TextSecondary),
        elevation = ButtonDefaults.buttonElevation(0.dp)
    ) {
        Text(text, fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun StatPill(label: String, count: Int, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("$count", color = color, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(label, color = TextSecondary, fontSize = 11.sp)
    }
}

@Composable
fun VendorSummaryRow(vendor: String, total: Double, txnCount: Int,
                     grandTotal: Double, isDebit: Boolean) {
    val accent   = if (isDebit) Color(0xFFFF6B6B) else AccentGreen
    val barColor = if (isDebit) listOf(Color(0xFFFF6B6B), Color(0xFFFF9999))
    else         listOf(AccentGreen, AccentGreen2)
    val fraction = if (grandTotal > 0) (total / grandTotal).toFloat() else 0f

    Column(modifier = Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .background(CardBg).padding(14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(vendor, color = TextPrimary,
                    fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Spacer(Modifier.height(2.dp))
                Text("$txnCount transaction${if (txnCount != 1) "s" else ""}",
                    color = TextSecondary, fontSize = 11.sp)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatAmount(total), color = accent,
                    fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("%.1f%%".format(fraction * 100),
                    color = TextSecondary, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(10.dp))
        Box(modifier = Modifier.fillMaxWidth().height(4.dp)
            .clip(RoundedCornerShape(2.dp)).background(Color(0xFF253150))) {
            Box(modifier = Modifier.fillMaxWidth(fraction).fillMaxHeight()
                .clip(RoundedCornerShape(2.dp))
                .background(Brush.linearGradient(barColor)))
        }
    }
}

@Composable
fun TransactionRow(txn: ParsedTransaction) {
    val isDebit = txn.type == TransactionType.DEBIT
    val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
        .format(Date(txn.timestamp))
    Row(modifier = Modifier.fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(CardBg)
        .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(2f)) {
            Text(txn.vendor, color = TextPrimary, fontSize = 13.sp,
                fontWeight = FontWeight.Medium, maxLines = 1)
            Spacer(Modifier.height(2.dp))
            Text(txn.bank,  color = TextSecondary, fontSize = 10.sp, maxLines = 1)
            Text(dateStr,   color = TextSecondary, fontSize = 10.sp)
        }
        Text(if (isDebit) formatAmount(txn.amount) else "—",
            color = if (isDebit) Color(0xFFFF6B6B) else Color(0xFF3D5080),
            fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End, modifier = Modifier.weight(1f))
        Text(if (!isDebit) formatAmount(txn.amount) else "—",
            color = if (!isDebit) AccentGreen else Color(0xFF3D5080),
            fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}

@Composable
fun DateChip(label: String, date: String,
             modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color(0xFF253150), contentColor = TextPrimary),
        border = BorderStroke(1.dp, FieldBorder),
        contentPadding = PaddingValues(horizontal = 12.dp)
    ) {
        Text("📅", fontSize = 14.sp)
        Spacer(Modifier.width(6.dp))
        Column {
            Text(label, color = TextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            Text(date,  color = TextPrimary,   fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun SummaryCard(label: String, amount: Double,
                accent: Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier
        .clip(RoundedCornerShape(16.dp))
        .background(Color(0xFF253150)).padding(14.dp)
    ) {
        Text(label, color = TextSecondary, fontSize = 11.sp)
        Spacer(Modifier.height(6.dp))
        Text(formatAmount(amount), color = accent,
            fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}