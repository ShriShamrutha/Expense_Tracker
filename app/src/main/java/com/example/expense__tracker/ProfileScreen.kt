package com.example.expense__tracker

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth

@Composable
fun ProfileScreen(
    totalSpentThisRange: Double,
    fromDateMillis: Long,       // ← NEW: pass date range for CSV
    toDateMillis: Long,         // ← NEW: pass date range for CSV
    onLogout: () -> Unit
) {
    val context     = LocalContext.current
    val user        = FirebaseAuth.getInstance().currentUser
    val savedBudget = BudgetManager.getBudget(context)

    var budgetInput      by remember {
        mutableStateOf(if (savedBudget > 0) savedBudget.toInt().toString() else "")
    }
    var showLogoutDialog  by remember { mutableStateOf(false) }
    var exportMessage     by remember { mutableStateOf("") }
    var showExportDialog  by remember { mutableStateOf(false) }

    val isOverBudget = savedBudget > 0 && totalSpentThisRange > savedBudget
    val percent      = if (savedBudget > 0)
        (totalSpentThisRange / savedBudget * 100).toInt() else 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBg)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(40.dp))

        Text("Profile & Settings", fontSize = 24.sp,
            fontWeight = FontWeight.Bold, color = TextPrimary)

        // ── User card ─────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBg)
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(36.dp))
                    .background(Brush.linearGradient(listOf(AccentGreen, AccentGreen2))),
                contentAlignment = Alignment.Center
            ) { Text("👤", fontSize = 32.sp) }

            Text(user?.phoneNumber ?: "Unknown", color = TextPrimary,
                fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text("Firebase Authenticated  ✓", color = AccentGreen, fontSize = 12.sp)
        }

        // ── Budget card ───────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBg)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Monthly Budget", color = TextPrimary,
                fontWeight = FontWeight.SemiBold, fontSize = 15.sp)

            if (savedBudget > 0) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Spent this period", color = TextSecondary, fontSize = 13.sp)
                        Text(formatAmount(totalSpentThisRange),
                            color = if (isOverBudget) Color(0xFFFF6B6B) else AccentGreen,
                            fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    Row(modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Budget limit", color = TextSecondary, fontSize = 13.sp)
                        Text(formatAmount(savedBudget), color = TextPrimary,
                            fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                    // Progress bar
                    Box(modifier = Modifier.fillMaxWidth().height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF253150))) {
                        Box(modifier = Modifier
                            .fillMaxWidth(
                                (totalSpentThisRange / savedBudget).toFloat().coerceAtMost(1f)
                            )
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(when {
                                isOverBudget -> Color(0xFFFF6B6B)
                                percent > 80 -> Color(0xFFFF922B)
                                else         -> AccentGreen
                            }))
                    }
                    when {
                        isOverBudget -> Text(
                            "🚨 Over budget by ${formatAmount(totalSpentThisRange - savedBudget)}!",
                            color = Color(0xFFFF6B6B), fontSize = 12.sp,
                            fontWeight = FontWeight.Medium)
                        percent > 80 -> Text(
                            "⚠️ $percent% used — ${formatAmount(savedBudget - totalSpentThisRange)} remaining.",
                            color = Color(0xFFFF922B), fontSize = 12.sp)
                        percent > 0  -> Text(
                            "✅ On track — ${formatAmount(totalSpentThisRange)} of ${formatAmount(savedBudget)} ($percent%)",
                            color = AccentGreen, fontSize = 12.sp)
                    }
                }
                HorizontalDivider(color = FieldBorder, thickness = 0.5.dp)
            }

            Text("Set a new budget limit:", color = TextSecondary, fontSize = 13.sp)

            OutlinedTextField(
                value = budgetInput,
                onValueChange = { budgetInput = it },
                label = { Text("Budget Amount (₹)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors
            )

            Row(modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        val amount = budgetInput.toDoubleOrNull()
                        if (amount != null && amount > 0) {
                            BudgetManager.saveBudget(context, amount)
                            Toast.makeText(context,
                                "Budget saved: ${formatAmount(amount)}",
                                Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Enter a valid amount",
                                Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                ) { Text("Save", fontWeight = FontWeight.Bold, color = Color(0xFF0F1629)) }

                OutlinedButton(
                    onClick = {
                        BudgetManager.saveBudget(context, 0.0)
                        budgetInput = ""
                        Toast.makeText(context, "Budget cleared", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f).height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF6B6B)),
                    border = BorderStroke(1.dp, Color(0xFFFF6B6B))
                ) { Text("Clear", fontWeight = FontWeight.Bold) }
            }
        }

        // ── Export CSV card ───────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBg)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Export Transactions", color = TextPrimary,
                fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(
                "Exports only the transactions in your currently selected date range " +
                        "(${formatDate(fromDateMillis)} → ${formatDate(toDateMillis)}) as a CSV file.",
                color = TextSecondary, fontSize = 13.sp, lineHeight = 18.sp
            )
            Button(
                onClick = {
                    // ← passes date range so only filtered data is exported
                    exportMessage  = exportToCsv(context, fromDateMillis, toDateMillis)
                    showExportDialog = true
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4D96FF))
            ) {
                Text("📄  Export to CSV  (${formatDate(fromDateMillis)} → ${formatDate(toDateMillis)})",
                    fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.White)
            }
        }

        // ── About card ────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(CardBg)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("About", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            InfoRow("App",         "Expense Tracker")
            InfoRow("Version",     "1.0.0")
            InfoRow("Tech",        "Kotlin + Jetpack Compose")
            InfoRow("Auth",        "Firebase Phone Auth")
            InfoRow("SMS Parsing", "Regex transaction detection")
            InfoRow("Charts",      "MPAndroidChart")
        }

        // ── Logout ────────────────────────────────────────
        Button(
            onClick = { showLogoutDialog = true },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6B6B))
        ) {
            Text("Logout", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
        }

        Spacer(Modifier.height(16.dp))
    }

    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            containerColor = CardBg,
            title = { Text("Export Result", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text  = { Text(exportMessage, color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("OK", color = AccentGreen, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            containerColor = CardBg,
            title = { Text("Logout?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text  = { Text("You'll need to verify your number again to log in.",
                color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = {
                    FirebaseAuth.getInstance().signOut()
                    showLogoutDialog = false
                    onLogout()
                }) { Text("Logout", color = Color(0xFFFF6B6B), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = TextSecondary, fontSize = 13.sp)
        Text(value,  color = TextPrimary,  fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}
