package com.example.expense__tracker

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.HorizontalBarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChartsScreen(
    filteredTransactions: List<ParsedTransaction>,
    fromDateMillis: Long,
    toDateMillis: Long
) {
    val debits  = filteredTransactions.filter { it.type == TransactionType.DEBIT }
    val credits = filteredTransactions.filter { it.type == TransactionType.CREDIT }

    val totalSpent    = debits.sumOf { it.amount }
    val totalReceived = credits.sumOf { it.amount }

    // Group debits by category for pie + bar charts
    val categoryTotals = debits
        .groupBy { it.category }
        .mapValues { (_, txns) -> txns.sumOf { it.amount } }
        .toList()
        .sortedByDescending { it.second }

    // Group by month for trend chart
    val monthSdf  = SimpleDateFormat("MMM yy", Locale.getDefault())
    val monthlyTotals = debits
        .groupBy { monthSdf.format(Date(it.timestamp)) }
        .mapValues { (_, txns) -> txns.sumOf { it.amount } }
        .toList()
        .sortedBy { monthSdf.parse(it.first)?.time ?: 0L }

    // Colors for charts
    val chartColors = listOf(
        AndroidColor.parseColor("#00C896"),
        AndroidColor.parseColor("#FF6B6B"),
        AndroidColor.parseColor("#FFD93D"),
        AndroidColor.parseColor("#4D96FF"),
        AndroidColor.parseColor("#FF922B"),
        AndroidColor.parseColor("#A855F7"),
    )

    val dateSdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBg)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(48.dp))

        // Title
        Text("Analytics", fontSize = 26.sp,
            fontWeight = FontWeight.Bold, color = Color.White)

        // Date range indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("🗓️", fontSize = 16.sp)
            Text(
                "${dateSdf.format(Date(fromDateMillis))}  →  ${dateSdf.format(Date(toDateMillis))}",
                color = Color(0xFF00C896), fontSize = 13.sp, fontWeight = FontWeight.Medium
            )
        }

        if (filteredTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1C2540)),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("📊", fontSize = 40.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("No data for selected range",
                        color = Color(0xFFABB8D4), fontSize = 14.sp)
                    Text("Go to Dashboard and scan SMS first",
                        color = Color(0xFFABB8D4), fontSize = 12.sp)
                }
            }
            return@Column
        }

        // ── PIE CHART ─────────────────────────────────────────
        if (categoryTotals.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1C2540))
                    .padding(16.dp)
            ) {
                Text(
                    "Spending by Category  (${debits.size} transactions)",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
                Spacer(Modifier.height(12.dp))

                val top6 = categoryTotals.take(6)

                AndroidView(
                    factory = { ctx ->
                        PieChart(ctx).apply {
                            // ── Fix: no percentage labels, no entry labels ──
                            setUsePercentValues(false)
                            setDrawEntryLabels(false)
                            description.isEnabled = false
                            isRotationEnabled    = true
                            isHighlightPerTapEnabled = true
                            holeRadius           = 42f
                            transparentCircleRadius = 47f
                            setHoleColor(AndroidColor.parseColor("#1C2540"))
                            setTransparentCircleColor(AndroidColor.parseColor("#1C2540"))

                            // Centre text
                            centerText       = "Spend\nBreakdown"
                            setCenterTextColor(AndroidColor.WHITE)
                            setCenterTextSize(12f)

                            // Hide legend — we draw our own below
                            legend.isEnabled = false

                            // Entries
                            val entries = top6.map { (cat, amt) ->
                                PieEntry(amt.toFloat(), cat)
                            }
                            val dataSet = PieDataSet(entries, "").apply {
                                colors = chartColors.take(top6.size)
                                sliceSpace    = 2f
                                selectionShift = 6f
                                // ── Fix: hide value labels on slices ──
                                setDrawValues(false)
                            }
                            data = PieData(dataSet)
                            invalidate()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                )

                // Custom legend table — amount only, no % sign
                Spacer(Modifier.height(12.dp))
                top6.forEachIndexed { i, (cat, amt) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(12.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(Color(chartColors[i % chartColors.size].toLong() or 0xFF000000L))
                            )
                            Text(cat, color = Color.White, fontSize = 13.sp,
                                modifier = Modifier.widthIn(max = 220.dp))
                        }
                        // ── Fix: show ₹ amount not raw float ──
                        Text(
                            formatAmount(amt),
                            color = Color(0xFFFF6B6B),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // ── HORIZONTAL BAR CHART ──────────────────────────────
        if (categoryTotals.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1C2540))
                    .padding(16.dp)
            ) {
                Text("Spending Breakdown",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp)
                Spacer(Modifier.height(12.dp))

                val top5 = categoryTotals.take(5)
                val barHeight = (top5.size * 52 + 40).coerceAtLeast(200)

                AndroidView(
                    factory = { ctx ->
                        HorizontalBarChart(ctx).apply {
                            description.isEnabled = false
                            legend.isEnabled       = false
                            setTouchEnabled(false)
                            setFitBars(true)

                            // ── Fix: hide value labels inside/above bars ──
                            setDrawValueAboveBar(false)

                            // X axis (left — category names)
                            xAxis.apply {
                                position       = XAxis.XAxisPosition.BOTTOM
                                setDrawGridLines(false)
                                setDrawAxisLine(false)
                                granularity    = 1f
                                isGranularityEnabled = true
                                textColor      = AndroidColor.parseColor("#ABB8D4")
                                textSize       = 10f
                                valueFormatter = object : ValueFormatter() {
                                    override fun getFormattedValue(value: Float): String {
                                        val idx = value.toInt()
                                        return if (idx in top5.indices)
                                            top5[idx].first.take(14) else ""
                                    }
                                }
                            }

                            // Y axis (bottom — amounts)
                            axisLeft.apply {
                                setDrawGridLines(true)
                                gridColor   = AndroidColor.parseColor("#253150")
                                textColor   = AndroidColor.parseColor("#ABB8D4")
                                textSize    = 9f
                                valueFormatter = object : ValueFormatter() {
                                    override fun getFormattedValue(value: Float): String =
                                        if (value >= 1000) "₹${(value/1000).toInt()}k"
                                        else "₹${value.toInt()}"
                                }
                            }
                            axisRight.isEnabled = false

                            val entries = top5.mapIndexed { i, (_, amt) ->
                                BarEntry(i.toFloat(), amt.toFloat())
                            }
                            val dataSet = BarDataSet(entries, "").apply {
                                colors    = chartColors.take(top5.size)
                                barBorderWidth = 0f
                                // ── Fix: no value text drawn on bars ──
                                setDrawValues(false)
                            }
                            data = BarData(dataSet).apply {
                                barWidth = 0.5f
                            }
                            invalidate()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barHeight.dp)
                )
            }
        }

        // ── MONTHLY TREND BAR CHART ───────────────────────────
        if (monthlyTotals.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF1C2540))
                    .padding(16.dp)
            ) {
                Text("Monthly Spending Trend",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp)
                Spacer(Modifier.height(12.dp))

                AndroidView(
                    factory = { ctx ->
                        BarChart(ctx).apply {
                            description.isEnabled = false
                            legend.isEnabled       = false
                            setTouchEnabled(false)

                            // ── Fix: no value labels above bars ──
                            setDrawValueAboveBar(false)

                            // X axis — month labels
                            xAxis.apply {
                                position           = XAxis.XAxisPosition.BOTTOM
                                setDrawGridLines(false)
                                setDrawAxisLine(false)
                                granularity        = 1f
                                isGranularityEnabled = true
                                // ── Fix: rotate labels to prevent overlap ──
                                labelRotationAngle = -30f
                                textColor          = AndroidColor.parseColor("#ABB8D4")
                                textSize           = 9f
                                valueFormatter     = object : ValueFormatter() {
                                    override fun getFormattedValue(value: Float): String {
                                        val idx = value.toInt()
                                        return if (idx in monthlyTotals.indices)
                                            monthlyTotals[idx].first else ""
                                    }
                                }
                            }

                            // Y axis
                            axisLeft.apply {
                                setDrawGridLines(true)
                                gridColor   = AndroidColor.parseColor("#253150")
                                textColor   = AndroidColor.parseColor("#ABB8D4")
                                textSize    = 9f
                                valueFormatter = object : ValueFormatter() {
                                    override fun getFormattedValue(value: Float): String =
                                        if (value >= 1000) "₹${(value/1000).toInt()}k"
                                        else "₹${value.toInt()}"
                                }
                            }
                            axisRight.isEnabled = false

                            val entries = monthlyTotals.mapIndexed { i, (_, amt) ->
                                BarEntry(i.toFloat(), amt.toFloat())
                            }
                            val dataSet = BarDataSet(entries, "").apply {
                                color = AndroidColor.parseColor("#00C896")
                                // ── Fix: no value text above bars ──
                                setDrawValues(false)
                            }
                            data = BarData(dataSet).apply {
                                barWidth = 0.5f
                            }
                            animateY(800)
                            invalidate()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                )
            }
        }

        // ── QUICK STATS ────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1C2540))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Quick Stats  (selected range)",
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp)

            val topCategory = categoryTotals.firstOrNull()
            val net = totalReceived - totalSpent

            listOf(
                Triple("Total Transactions",   "${filteredTransactions.size}",   Color(0xFF00C896)),
                Triple("Debit count → total",  "${debits.size}  →  ${formatAmount(totalSpent)}", Color(0xFF00C896)),
                Triple("Credit count → total", "${credits.size}  →  ${formatAmount(totalReceived)}", Color(0xFF00C896)),
                Triple("Net Balance",           formatAmount(net),  if (net >= 0) Color(0xFF00C896) else Color(0xFFFF6B6B)),
                Triple("Biggest spend category",topCategory?.first ?: "—", Color(0xFFFFD93D)),
                Triple("Biggest spend amount",  topCategory?.let { formatAmount(it.second) } ?: "—", Color(0xFFFF6B6B)),
            ).forEach { (label, value, color) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(label, color = Color(0xFFABB8D4), fontSize = 13.sp,
                        modifier = Modifier.weight(1f))
                    Text(value, color = color,
                        fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}