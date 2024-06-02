package com.edisoninnovations.save_money.models

data class Transaction(
    val id_transaccion: String,
    val category: String,
    val amount: Double,
    val note: String,
    val tipo: String,
    val imageUrls: List<String>?
)