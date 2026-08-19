package com.example.expense__tracker

fun formatAmount(amount: Double): String {
    return if (amount % 1.0 == 0.0) {
        "₹%.0f".format(amount)      // 500 → ₹500
    } else {
        "₹%.2f".format(amount)      // 280.65 → ₹280.65
    }
}