package com.example.expense__tracker

data class HealthScore(
    val score: Int,
    val grade: String,
    val color: Long,
    val insights: List<String>
)

fun calculateHealthScore(
    transactions: List<ParsedTransaction>,
    budget: Double
): HealthScore {
    var score    = 100
    val insights = mutableListOf<String>()

    val debits   = transactions.filter { it.type == TransactionType.DEBIT }
    val credits  = transactions.filter { it.type == TransactionType.CREDIT }
    val spent    = debits.sumOf { it.amount }
    val received = credits.sumOf { it.amount }

    // Rule 1: spending vs income ratio
    if (received > 0) {
        val ratio = spent / received
        when {
            ratio > 0.9  -> {
                score -= 30
                insights.add("You spent ${(ratio*100).toInt()}% of your income — very high risk. Try to keep spending below 70%.")
            }
            ratio > 0.7  -> {
                score -= 15
                insights.add("You spent ${(ratio*100).toInt()}% of your income. Try to reduce non-essential expenses.")
            }
            ratio <= 0.5 ->
                insights.add("Excellent! You spent only ${(ratio*100).toInt()}% of income — great financial discipline.")
            else ->
                insights.add("You spent ${(ratio*100).toInt()}% of income — within healthy range.")
        }
    } else {
        insights.add("No income recorded this period. Add credits to track income vs spending.")
    }

    // Rule 2: budget adherence
    if (budget > 0) {
        when {
            spent > budget -> {
                score -= 20
                insights.add("Budget exceeded by ${formatAmount(spent - budget)}. Review your spending categories.")
            }
            spent > budget * 0.8 -> {
                score -= 10
                insights.add("You have used ${((spent/budget)*100).toInt()}% of budget. Only ${formatAmount(budget - spent)} left.")
            }
            else ->
                insights.add("Well within budget! You have ${formatAmount(budget - spent)} remaining.")
        }
    }

    // Rule 3: spending diversity
    val categories = debits.map { it.category }.distinct().size
    when {
        categories > 6 ->
            insights.add("Good spending diversity across $categories categories.")
        categories in 3..6 ->
            insights.add("Spending across $categories categories — reasonable distribution.")
        categories < 3 -> {
            score -= 5
            insights.add("Very concentrated spending in only $categories categories.")
        }
    }

    // Rule 4: savings
    val savings = received - spent
    when {
        received > 0 && savings > received * 0.3 ->
            insights.add("Saving ${((savings/received)*100).toInt()}% of income — excellent! Keep it up.")
        received > 0 && savings < 0 -> {
            score -= 15
            insights.add("Spending more than income this period. Immediate action needed.")
        }
        received > 0 ->
            insights.add("Saving ${((savings/received)*100).toInt()}% of income. Try to reach 30% savings rate.")
    }

    score = score.coerceIn(0, 100)

    val (grade, color) = when {
        score >= 80 -> "Excellent" to 0xFF00C896L
        score >= 60 -> "Good"      to 0xFF4D96FFL
        score >= 40 -> "Fair"      to 0xFFFF922BL
        else        -> "Needs Work" to 0xFFFF6B6BL
    }

    return HealthScore(score, grade, color, insights)
}
