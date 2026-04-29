package com.thegioisua.bankforwarder

data class TransactionRecord(
    val timeMs: Long,
    val orderId: String,
    val amount: Long,
    val prefix: String,
    val bank: String,
    val source: String,
    val status: String   // "success" | "failed"
)
