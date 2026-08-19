package com.example.expense__tracker

import android.app.Activity
import android.widget.Toast
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
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

@Composable
fun OtpScreen(
    phoneNumber: String,
    onLoginSuccess: () -> Unit
) {
    val context  = LocalContext.current
    val activity = context as Activity
    val auth     = FirebaseAuth.getInstance()

    var otp            by remember { mutableStateOf("") }
    var verificationId by remember { mutableStateOf("") }
    var isSending      by remember { mutableStateOf(true) }
    var isVerifying    by remember { mutableStateOf(false) }
    var statusMessage  by remember { mutableStateOf("Sending OTP to $phoneNumber…") }

    LaunchedEffect(phoneNumber) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    auth.signInWithCredential(credential)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) onLoginSuccess()
                        }
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    isSending     = false
                    statusMessage = "Failed: ${e.message}"
                }

                override fun onCodeSent(
                    vId: String,
                    token: PhoneAuthProvider.ForceResendingToken
                ) {
                    verificationId = vId
                    isSending      = false
                    statusMessage  = "OTP sent to $phoneNumber ✓"
                }
            })
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

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
                    .background(
                        Brush.linearGradient(listOf(AccentGreen, AccentGreen2))
                    ),
                contentAlignment = Alignment.Center
            ) { Text("🔐", fontSize = 30.sp) }

            Spacer(Modifier.height(20.dp))

            Text("Verify your number", fontSize = 24.sp,
                fontWeight = FontWeight.Bold, color = TextPrimary)

            Spacer(Modifier.height(8.dp))

            Text(statusMessage,
                color = TextSecondary, fontSize = 14.sp,
                textAlign = TextAlign.Center, lineHeight = 20.sp)

            Spacer(Modifier.height(36.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(CardBg)
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                OutlinedTextField(
                    value = otp,
                    onValueChange = { if (it.length <= 6) otp = it },
                    label = { Text("OTP Code") },
                    placeholder = { Text("• • • • • •", color = TextSecondary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    enabled = !isSending && !isVerifying,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors,
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        letterSpacing = 8.sp
                    )
                )

                Button(
                    onClick = {
                        when {
                            otp.length < 6 ->
                                Toast.makeText(context,
                                    "Enter all 6 digits",
                                    Toast.LENGTH_SHORT).show()

                            verificationId.isEmpty() ->
                                Toast.makeText(context,
                                    "Please wait, sending OTP…",
                                    Toast.LENGTH_SHORT).show()

                            else -> {
                                isVerifying = true
                                val credential = PhoneAuthProvider.getCredential(
                                    verificationId, otp)
                                auth.signInWithCredential(credential)
                                    .addOnCompleteListener { task ->
                                        isVerifying = false
                                        if (task.isSuccessful) {
                                            onLoginSuccess()
                                        } else {
                                            Toast.makeText(context,
                                                "Incorrect OTP. Try again.",
                                                Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    enabled = !isSending && !isVerifying,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGreen)
                ) {
                    Text(
                        when {
                            isSending   -> "Sending OTP…"
                            isVerifying -> "Verifying…"
                            else        -> "Verify & Continue"
                        },
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color(0xFF0F1629)
                    )
                }

                TextButton(
                    onClick = { },
                    enabled = !isSending && !isVerifying
                ) {
                    Text("Resend OTP", color = AccentGreen, fontSize = 14.sp)
                }
            }
        }
    }
}