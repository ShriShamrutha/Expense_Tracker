package com.example.expense__tracker

import android.content.Context

object BudgetManager {

    private const val PREF_NAME = "budget_prefs"
    private const val KEY_BUDGET = "monthly_budget"

    fun saveBudget(context: Context, amount: Double) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putFloat(KEY_BUDGET, amount.toFloat()).apply()
    }

    fun getBudget(context: Context): Double {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getFloat(KEY_BUDGET, 0f).toDouble()
    }
}