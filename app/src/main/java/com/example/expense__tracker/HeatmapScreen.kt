package com.example.expense__tracker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HeatmapScreen(transactions: List<ParsedTransaction>) {

    val debits = transactions.filter { it.type == TransactionType.DEBIT }

    // Group spending by day
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val dailySpend = debits
        .groupBy { sdf.format(Date(it.timestamp)) }
        .mapValues { (_, t) -> t.sumOf { it.amount } }

    val maxSpend = dailySpend.values.maxOrNull() ?: 1.0

    // Current month calendar
    val cal = Calendar.getInstance()
    cal.set(Calendar.DAY_OF_MONTH, 1)
    val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1
    val daysInMonth    = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    val year           = cal.get(Calendar.YEAR)
    val month          = cal.get(Calendar.MONTH)
    val monthName      = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBg)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(40.dp))

        Text("🗓️ Spending Heatmap",
            fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        Text(monthName, fontSize = 14.sp, color = AccentGreen)

        if (transactions.isEmpty()) {
            Text("Scan SMS from Dashboard first to see your spending heatmap.",
                color = TextSecondary, fontSize = 14.sp)
            return@Column
        }

        // ── Calendar grid ─────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBg)
                .padding(16.dp)
        ) {
            Text("Daily Spending This Month", color = TextPrimary,
                fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(Modifier.height(12.dp))

            // Day headers
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("S", "M", "T", "W", "T", "F", "S").forEach { day ->
                    Text(day,
                        modifier = Modifier.weight(1f),
                        color = TextSecondary, fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(6.dp))

            val totalCells = firstDayOfWeek + daysInMonth
            val rows       = (totalCells + 6) / 7

            for (row in 0 until rows) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                ) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        val day       = cellIndex - firstDayOfWeek + 1

                        if (day < 1 || day > daysInMonth) {
                            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                        } else {
                            val dateKey  = "%04d-%02d-%02d".format(year, month + 1, day)
                            val spent    = dailySpend[dateKey] ?: 0.0
                            val fraction = (spent / maxSpend).toFloat()

                            val bgColor = when {
                                spent == 0.0     -> Color(0xFF253150)
                                fraction < 0.25f -> Color(0xFF1B5E20)
                                fraction < 0.50f -> Color(0xFF388E3C)
                                fraction < 0.75f -> Color(0xFFFF922B)
                                else             -> Color(0xFFFF6B6B)
                            }

                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(bgColor),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text("$day", color = Color.White,
                                    fontSize = 10.sp, fontWeight = FontWeight.Medium)
                                if (spent > 0) {
                                    Text(
                                        if (spent >= 1000) "₹${(spent/1000).toInt()}k"
                                        else "₹${spent.toInt()}",
                                        color = Color.White, fontSize = 7.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Colour legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Less spend", color = TextSecondary, fontSize = 10.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        Color(0xFF253150), Color(0xFF1B5E20),
                        Color(0xFF388E3C), Color(0xFFFF922B), Color(0xFFFF6B6B)
                    ).forEach { c ->
                        Box(modifier = Modifier
                            .size(16.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(c))
                    }
                }
                Text("More spend", color = TextSecondary, fontSize = 10.sp)
            }
        }

        // ── Top spending days ─────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBg)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("🔥 Highest Spending Days", color = TextPrimary,
                fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            HorizontalDivider(color = FieldBorder, thickness = 0.5.dp)

            val topDays = dailySpend.toList()
                .sortedByDescending { it.second }.take(5)

            if (topDays.isEmpty()) {
                Text("No spending data found.", color = TextSecondary, fontSize = 13.sp)
            } else {
                topDays.forEachIndexed { i, (date, amt) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("${i + 1}.", color = TextSecondary, fontSize = 12.sp)
                            Text(date, color = TextPrimary, fontSize = 13.sp)
                        }
                        Text(formatAmount(amt), color = Color(0xFFFF6B6B),
                            fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // ── Daily average card ────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBg)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Monthly Stats", color = TextPrimary,
                fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            HorizontalDivider(color = FieldBorder, thickness = 0.5.dp)

            val activeDays = dailySpend.size
            val avgDaily   = if (activeDays > 0) debits.sumOf { it.amount } / activeDays else 0.0

            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Days with spending", color = TextSecondary, fontSize = 13.sp)
                Text("$activeDays days", color = TextPrimary,
                    fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Average per active day", color = TextSecondary, fontSize = 13.sp)
                Text(formatAmount(avgDaily), color = AccentGreen,
                    fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Total this month", color = TextSecondary, fontSize = 13.sp)
                Text(formatAmount(debits.sumOf { it.amount }), color = Color(0xFFFF6B6B),
                    fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}
