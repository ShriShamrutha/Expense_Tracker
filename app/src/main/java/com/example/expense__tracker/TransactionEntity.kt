package com.example.expense__tracker

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val rawSms: String,
    val amount: Double,
    val type: String,       // "DEBIT" or "CREDIT"
    val bank: String,
    val vendor: String,
    val category: String,
    val timestamp: Long
)

// Convert between Room entity and ParsedTransaction
fun TransactionEntity.toParsed() = ParsedTransaction(
    rawSms    = rawSms,
    amount    = amount,
    type      = if (type == "DEBIT") TransactionType.DEBIT else TransactionType.CREDIT,
    bank      = bank,
    vendor    = vendor,
    category  = category,
    timestamp = timestamp
)

fun ParsedTransaction.toEntity() = TransactionEntity(
    rawSms    = rawSms,
    amount    = amount,
    type      = type.name,
    bank      = bank,
    vendor    = vendor,
    category  = category,
    timestamp = timestamp
)