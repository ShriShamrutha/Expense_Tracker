package com.example.expense__tracker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AiInsightsScreen(filteredTransactions: List<ParsedTransaction>) {

    val context    = LocalContext.current
    val debits     = filteredTransactions.filter { it.type == TransactionType.DEBIT }
    val credits    = filteredTransactions.filter { it.type == TransactionType.CREDIT }
    val totalSpent = debits.sumOf { it.amount }
    val totalRcvd  = credits.sumOf { it.amount }
    val budget     = BudgetManager.getBudget(context)

    val topVendors = debits
        .groupBy { it.category }
        .mapValues { (_, t) -> t.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }
        .take(5)

    // Generate insights locally
    val insights = generateLocalInsights(
        debits, credits, totalSpent, totalRcvd, budget, topVendors
    )

    // Health score
    val health = calculateHealthScore(filteredTransactions, budget)

    // Recurring transactions
    val recurring = detectRecurring(filteredTransactions)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBg)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(40.dp))

        Text("🤖 AI Financial Advisor",
            fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text("Smart insights based on your transactions",
            fontSize = 13.sp, color = AccentGreen)

        if (filteredTransactions.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBg).padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("📊", fontSize = 40.sp)
                Spacer(Modifier.height(8.dp))
                Text("No transactions found",
                    color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text("Scan SMS from Dashboard first to get AI insights.",
                    color = TextSecondary, fontSize = 13.sp)
            }
            return@Column
        }

        // ── Summary card ──────────────────────────────
        Column(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBg).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("📋 Your Financial Summary",
                color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            HorizontalDivider(color = FieldBorder, thickness = 0.5.dp)

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total Spent",    color = TextSecondary, fontSize = 13.sp)
                Text(formatAmount(totalSpent), color = Color(0xFFFF6B6B),
                    fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total Received", color = TextSecondary, fontSize = 13.sp)
                Text(formatAmount(totalRcvd), color = AccentGreen,
                    fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Net Balance",    color = TextSecondary, fontSize = 13.sp)
                val net = totalRcvd - totalSpent
                Text(formatAmount(net),
                    color = if (net >= 0) AccentGreen else Color(0xFFFF6B6B),
                    fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Transactions",   color = TextSecondary, fontSize = 13.sp)
                Text("${filteredTransactions.size}",
                    color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        // ── Health score card ─────────────────────────
        Column(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBg).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("💯 Financial Health Score",
                color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            HorizontalDivider(color = FieldBorder, thickness = 0.5.dp)

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(40.dp))
                        .background(Color(health.color)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("${health.score}", color = Color.White,
                            fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("/100", color = Color.White, fontSize = 10.sp)
                    }
                }
                Column {
                    Text(health.grade, color = Color(health.color),
                        fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("Based on your spending habits",
                        color = TextSecondary, fontSize = 12.sp)
                }
            }

            // Progress bar
            Box(
                modifier = Modifier.fillMaxWidth().height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF253150))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(health.score / 100f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(health.color))
                )
            }

            health.insights.forEach { ins ->
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF253150))
                        .padding(10.dp)
                ) {
                    Text(ins, color = TextPrimary, fontSize = 13.sp, lineHeight = 18.sp)
                }
            }
        }

        // ── AI Insights card ──────────────────────────
        Column(
            modifier = Modifier.fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBg).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("💡 Smart Spending Insights",
                color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            HorizontalDivider(color = FieldBorder, thickness = 0.5.dp)

            insights.forEach { insight ->
                Column(
                    modifier = Modifier.fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF253150))
                        .padding(12.dp)
                ) {
                    Text(insight, color = TextPrimary, fontSize = 13.sp, lineHeight = 20.sp)
                }
            }
        }

        // ── Top spending vendors ───────────────────────
        if (topVendors.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBg).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("🏆 Top Spending Categories",
                    color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                HorizontalDivider(color = FieldBorder, thickness = 0.5.dp)

                topVendors.forEachIndexed { i, (vendor, amt) ->
                    val pct = if (totalSpent > 0) (amt / totalSpent * 100).toInt() else 0
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${i + 1}. $vendor", color = TextPrimary, fontSize = 13.sp)
                            Text("${formatAmount(amt)}  ($pct%)",
                                color = Color(0xFFFF6B6B),
                                fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Box(
                            modifier = Modifier.fillMaxWidth().height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFF253150))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(pct / 100f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color(0xFFFF6B6B))
                            )
                        }
                    }
                }
            }
        }

        // ── Recurring transactions ────────────────────
        if (recurring.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CardBg).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("🔄 Recurring Payments Detected",
                    color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text("${recurring.size} subscription(s) / EMI(s) found",
                    color = TextSecondary, fontSize = 12.sp)
                HorizontalDivider(color = FieldBorder, thickness = 0.5.dp)

                recurring.forEach { r ->
                    Column(
                        modifier = Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF253150))
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(r.vendor, color = TextPrimary,
                                fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text(formatAmount(r.amount),
                                color = AccentGreen,
                                fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("${r.frequency} · ${r.count} payments",
                                color = TextSecondary, fontSize = 11.sp)
                            Text("Next: ${r.nextExpected}",
                                color = TextSecondary, fontSize = 11.sp)
                        }
                        Text("Total paid: ${formatAmount(r.totalPaid)}",
                            color = Color(0xFFFF6B6B), fontSize = 11.sp)
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}

// ── Generate insights locally without API ─────────────────────
fun generateLocalInsights(
    debits: List<ParsedTransaction>,
    credits: List<ParsedTransaction>,
    totalSpent: Double,
    totalRcvd: Double,
    budget: Double,
    topVendors: List<Pair<String, Double>>
): List<String> {
    val insights = mutableListOf<String>()

    // Insight 1: Income vs spending
    if (totalRcvd > 0) {
        val ratio = (totalSpent / totalRcvd * 100).toInt()
        when {
            ratio > 90 -> insights.add(
                "Spending Alert: You spent $ratio% of your income this period. " +
                        "This is very high — try to keep spending below 70% of income.")
            ratio > 70 -> insights.add(
                " High Spending: You spent $ratio% of your income. " +
                        "Consider cutting back on non-essential purchases to improve savings.")
            ratio < 50 -> insights.add(
                "Excellent Control: You spent only $ratio% of your income. " +
                        "You are saving well — consider investing the surplus.")
            else -> insights.add(
                "Good Balance: You spent $ratio% of income. " +
                        "Try to push this below 60% by reducing discretionary spending.")
        }
    }

    // Insight 2: Top vendor advice
    topVendors.firstOrNull()?.let { (vendor, amt) ->
        val pct = if (totalSpent > 0) (amt / totalSpent * 100).toInt() else 0
        if (pct > 30) {
            insights.add(
                "Dominant Spend: $vendor accounts for $pct% of your total spending " +
                        "(${formatAmount(amt)}). Consider if this aligns with your priorities.")
        } else {
            insights.add(
                "Top Category: Your highest spend is $vendor at ${formatAmount(amt)} ($pct%). " +
                        "This is a reasonable proportion of your total spending.")
        }
    }

    // Insight 3: Budget advice
    if (budget > 0) {
        val used = (totalSpent / budget * 100).toInt()
        when {
            totalSpent > budget -> insights.add(
                "Budget Exceeded: You went ${formatAmount(totalSpent - budget)} over your " +
                        "${formatAmount(budget)} budget. Set stricter daily limits next month.")
            used > 80 -> insights.add(
                " Budget Warning: You have used $used% of your ${formatAmount(budget)} budget. " +
                        "Only ${formatAmount(budget - totalSpent)} remaining — spend carefully.")
            else -> insights.add(
                " Budget On Track: You have used $used% of your budget. " +
                        "${formatAmount(budget - totalSpent)} remaining — great discipline!")
        }
    } else {
        insights.add(
            "Set a Budget: You haven't set a monthly budget yet. " +
                    "Go to Profile → Set Budget to track your spending limits automatically.")
    }

    // Insight 4: Net balance
    val net = totalRcvd - totalSpent
    when {
        net > 0 -> insights.add(
            "Positive Balance: You have a net surplus of ${formatAmount(net)} this period. " +
                    "Consider putting this into savings or investments.")
        net < 0 -> insights.add(
            " Negative Balance: You spent ${formatAmount(-net)} more than you received. " +
                    "Review your expenses and identify areas to cut back immediately.")
        else -> insights.add(
            "⚖Balanced: Your income and spending are almost equal this period. " +
                    "Try to create a positive surplus by reducing a few expenses.")
    }

    // Insight 5: Transaction frequency
    val txnCount = debits.size
    when {
        txnCount > 50 -> insights.add(
            "High Activity: You made $txnCount debit transactions this period — " +
                    "that's very frequent. Many small purchases add up. Track daily spending carefully.")
        txnCount > 20 -> insights.add(
            "Active Spender: $txnCount transactions this period. " +
                    "Review small recurring purchases — they often go unnoticed but add up significantly.")
        else -> insights.add(
            "Focused Spending: Only $txnCount transactions this period. " +
                    "Low transaction count usually means better spending control.")
    }

    return insights
}
