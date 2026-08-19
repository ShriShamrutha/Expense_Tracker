package com.example.expense__tracker

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.expense__tracker.ui.theme.Expense__TrackerTheme
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

// ── Brand tokens ──────────────────────────────────────────────
val AppBg         = Color(0xFF0F1629)
val CardBg        = Color(0xFF1C2540)
val AccentGreen   = Color(0xFF00C896)
val AccentGreen2  = Color(0xFF00E5A0)
val FieldBg       = Color(0xFF253150)
val FieldBorder   = Color(0xFF3D5080)
val TextPrimary   = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFABB8D4)

enum class AppScreen { SPLASH, LOGIN, OTP, HOME }
enum class HomeTab   { DASHBOARD, CHARTS, HEATMAP, AI, PROFILE }

private const val PREFS_NAME    = "app_prefs"
private const val KEY_INSTALLED = "is_installed"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)

        // Create notification channel
        NotificationHelper.createNotificationChannel(this)

        enableEdgeToEdge()

        setContent {
            Expense__TrackerTheme {

                var screen      by remember { mutableStateOf(AppScreen.SPLASH) }
                var phoneNumber by remember { mutableStateOf("") }
                var homeTab     by remember { mutableStateOf(HomeTab.DASHBOARD) }

                // ── Shared state across all tabs ──────────
                var allTransactions by remember { mutableStateOf(listOf<ParsedTransaction>()) }
                var hasScanned      by remember { mutableStateOf(false) }
                var fromDateMillis  by remember { mutableStateOf(startOfCurrentMonth()) }
                var toDateMillis    by remember { mutableStateOf(System.currentTimeMillis()) }

                val filteredTransactions = remember(allTransactions, fromDateMillis, toDateMillis) {
                    allTransactions.filter { it.timestamp in fromDateMillis..toDateMillis }
                }

                when (screen) {

                    // ── Splash ────────────────────────────
                    AppScreen.SPLASH -> SplashScreen(onFinished = {
                        val prefs       = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                        val isInstalled = prefs.getBoolean(KEY_INSTALLED, false)

                        if (!isInstalled) {
                            // Fresh install → always show login
                            prefs.edit().putBoolean(KEY_INSTALLED, true).apply()
                            screen = AppScreen.LOGIN
                        } else {
                            // Returning user → check Firebase session
                            screen = if (FirebaseAuth.getInstance().currentUser != null)
                                AppScreen.HOME else AppScreen.LOGIN
                        }
                    })

                    // ── Login ─────────────────────────────
                    AppScreen.LOGIN -> LoginScreen(onContinue = { number ->
                        phoneNumber = number
                        screen = AppScreen.OTP
                    })

                    // ── OTP ───────────────────────────────
                    AppScreen.OTP -> OtpScreen(
                        phoneNumber    = phoneNumber,
                        onLoginSuccess = { screen = AppScreen.HOME }
                    )

                    // ── Home ──────────────────────────────
                    AppScreen.HOME -> {
                        // Ask notification permission AFTER login — only on Android 13+
                        LaunchedEffect(Unit) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                if (ContextCompat.checkSelfPermission(
                                        this@MainActivity,
                                        android.Manifest.permission.POST_NOTIFICATIONS
                                    ) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    requestPermissions(
                                        arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 100
                                    )
                                }
                            }
                        }
                        Scaffold(
                            bottomBar = {
                                NavigationBar(containerColor = CardBg) {

                                    NavigationBarItem(
                                        selected = homeTab == HomeTab.DASHBOARD,
                                        onClick  = { homeTab = HomeTab.DASHBOARD },
                                        icon  = { Text("📊", fontSize = 18.sp) },
                                        label = {
                                            Text("Dashboard", fontSize = 10.sp,
                                                color = if (homeTab == HomeTab.DASHBOARD)
                                                    AccentGreen else TextSecondary)
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            indicatorColor = FieldBg)
                                    )

                                    NavigationBarItem(
                                        selected = homeTab == HomeTab.CHARTS,
                                        onClick  = { homeTab = HomeTab.CHARTS },
                                        icon  = { Text("📈", fontSize = 18.sp) },
                                        label = {
                                            Text("Analytics", fontSize = 10.sp,
                                                color = if (homeTab == HomeTab.CHARTS)
                                                    AccentGreen else TextSecondary)
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            indicatorColor = FieldBg)
                                    )

                                    NavigationBarItem(
                                        selected = homeTab == HomeTab.HEATMAP,
                                        onClick  = { homeTab = HomeTab.HEATMAP },
                                        icon  = { Text("🗓️", fontSize = 18.sp) },
                                        label = {
                                            Text("Heatmap", fontSize = 10.sp,
                                                color = if (homeTab == HomeTab.HEATMAP)
                                                    AccentGreen else TextSecondary)
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            indicatorColor = FieldBg)
                                    )

                                    NavigationBarItem(
                                        selected = homeTab == HomeTab.AI,
                                        onClick  = { homeTab = HomeTab.AI },
                                        icon  = { Text("🤖", fontSize = 18.sp) },
                                        label = {
                                            Text("AI Advice", fontSize = 10.sp,
                                                color = if (homeTab == HomeTab.AI)
                                                    AccentGreen else TextSecondary)
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            indicatorColor = FieldBg)
                                    )

                                    NavigationBarItem(
                                        selected = homeTab == HomeTab.PROFILE,
                                        onClick  = { homeTab = HomeTab.PROFILE },
                                        icon  = { Text("👤", fontSize = 18.sp) },
                                        label = {
                                            Text("Profile", fontSize = 10.sp,
                                                color = if (homeTab == HomeTab.PROFILE)
                                                    AccentGreen else TextSecondary)
                                        },
                                        colors = NavigationBarItemDefaults.colors(
                                            indicatorColor = FieldBg)
                                    )
                                }
                            }
                        ) { padding ->
                            Box(modifier = Modifier.padding(padding)) {
                                when (homeTab) {

                                    HomeTab.DASHBOARD -> DashboardScreen(
                                        allTransactions      = allTransactions,
                                        hasScanned           = hasScanned,
                                        fromDateMillis       = fromDateMillis,
                                        toDateMillis         = toDateMillis,
                                        onTransactionsLoaded = {
                                            allTransactions = it
                                            hasScanned = true
                                        },
                                        onFromDateChanged = { fromDateMillis = it },
                                        onToDateChanged   = { toDateMillis = it }
                                    )

                                    HomeTab.CHARTS -> ChartsScreen(
                                        filteredTransactions = filteredTransactions,
                                        fromDateMillis       = fromDateMillis,
                                        toDateMillis         = toDateMillis
                                    )

                                    HomeTab.HEATMAP -> HeatmapScreen(
                                        transactions = filteredTransactions
                                    )

                                    HomeTab.AI -> AiInsightsScreen(
                                        filteredTransactions = filteredTransactions
                                    )

                                    HomeTab.PROFILE -> ProfileScreen(
                                        totalSpentThisRange = filteredTransactions
                                            .filter { it.type == TransactionType.DEBIT }
                                            .sumOf { it.amount },
                                        fromDateMillis = fromDateMillis,
                                        toDateMillis   = toDateMillis,
                                        onLogout = {
                                            // Clear install flag → next launch shows login
                                            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                                                .edit()
                                                .remove(KEY_INSTALLED)
                                                .apply()
                                            allTransactions = emptyList()
                                            hasScanned      = false
                                            screen          = AppScreen.LOGIN
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Shared field colours ──────────────────────────────────────
val fieldColors: TextFieldColors
    @Composable get() = OutlinedTextFieldDefaults.colors(
        focusedTextColor        = TextPrimary,
        unfocusedTextColor      = TextPrimary,
        focusedContainerColor   = FieldBg,
        unfocusedContainerColor = FieldBg,
        cursorColor             = AccentGreen,
        focusedBorderColor      = AccentGreen,
        unfocusedBorderColor    = FieldBorder,
        focusedLabelColor       = AccentGreen,
        unfocusedLabelColor     = TextSecondary,
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(onContinue: (String) -> Unit) {

    val context         = LocalContext.current
    var mobileNumber    by remember { mutableStateOf("") }
    val countries = listOf(
        "🇮🇳  India (+91)"     to "+91",
        "🇦🇺  Australia (+61)" to "+61",
        "🇺🇸  USA (+1)"        to "+1",
        "🇬🇧  UK (+44)"        to "+44",
        "🇨🇦  Canada (+1)"     to "+1"
    )
    var expanded        by remember { mutableStateOf(false) }
    var selectedCountry by remember { mutableStateOf(countries[0]) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AppBg)
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.linearGradient(listOf(AccentGreen, AccentGreen2))),
                contentAlignment = Alignment.Center
            ) {
                Text("₹", fontSize = 36.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(20.dp))
            Text("Expense Tracker", fontSize = 28.sp,
                fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(Modifier.height(6.dp))
            Text("Track every rupee, every day",
                fontSize = 14.sp, color = TextSecondary)
            Spacer(Modifier.height(36.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardBg)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Sign in with your mobile number",
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp)

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedCountry.first, onValueChange = {},
                        readOnly = true, label = { Text("Country") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        shape = RoundedCornerShape(12.dp), colors = fieldColors
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(CardBg)
                    ) {
                        countries.forEach { country ->
                            DropdownMenuItem(
                                text = { Text(country.first, color = TextPrimary) },
                                onClick = { selectedCountry = country; expanded = false }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = mobileNumber,
                    onValueChange = { if (it.length <= 10) mobileNumber = it },
                    label = { Text("Mobile Number") },
                    placeholder = { Text("10-digit number", color = TextSecondary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors
                )

                Button(
                    onClick = {
                        if (mobileNumber.length == 10) {
                            onContinue(selectedCountry.second + mobileNumber)
                        } else {
                            android.widget.Toast.makeText(
                                context,
                                "Enter a valid 10-digit number",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                ) {
                    Text("Send OTP", fontWeight = FontWeight.Bold,
                        fontSize = 16.sp, color = Color(0xFF0F1629))
                }
            }

            Spacer(Modifier.height(24.dp))
            Text(
                "Your number is only used for verification",
                color = TextSecondary, fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
