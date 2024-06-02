package com.edisoninnovations.save_money.models
@kotlinx.serialization.Serializable
data class TransactionData(
    val id_categoria: Int,
    val nota: String?,
    val tipo: String,
    val cantidad: Double,
    val id_usuario: String,
    val fecha: String,
    val tiempo: String
)
@kotlinx.serialization.Serializable
data class Transimage(
    val id_transaccion: Int,
    val imagen: String
)